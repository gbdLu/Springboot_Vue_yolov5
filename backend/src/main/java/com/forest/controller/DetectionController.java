package com.forest.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forest.dto.Result;
import com.forest.entity.DetectionRecord;
import com.forest.entity.WorkOrder;
import com.forest.service.DetectionService;
import com.forest.service.WorkOrderService;
import com.forest.utils.HttpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/detection")
public class DetectionController {

    @Value("${upload.path}")
    private String uploadPath;

    @Value("${upload.detection-url}")
    private String detectionUrl;

    @Autowired
    private DetectionService detectionService;

    @Autowired
    private WorkOrderService workOrderService;

    @Autowired
    private ObjectMapper objectMapper;

    @PostMapping("/upload")
    public Result<Map<String, Object>> uploadAndDetect(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "forestAreaId", required = false) Integer forestAreaId,
            @RequestAttribute("userId") Integer userId) {

        try {
            // 1. 保存原图
            String originalFilename = file.getOriginalFilename();
            String ext = FileUtil.extName(originalFilename);
            String newFileName = IdUtil.fastSimpleUUID() + "." + ext;
            String savePath = uploadPath + "original/" + newFileName;
            File saveFile = new File(savePath);
            if (!saveFile.getParentFile().exists()) {
                saveFile.getParentFile().mkdirs();
            }
            file.transferTo(saveFile);

            // 2. 调用YOLOv5推理服务（3类：fire=0, human=1, smoke=2）
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("image_path", savePath);
            requestBody.put("confidence", 0.25);

            String resultJson = HttpUtil.postJson(detectionUrl, objectMapper.writeValueAsString(requestBody));
            Map<String, Object> detectionResult = objectMapper.readValue(resultJson, Map.class);

            // 3. 解析结果
            List<Map<String, Object>> detections = (List<Map<String, Object>>) detectionResult.get("detections");
            int fireCount = 0, humanCount = 0, smokeCount = 0;

            for (Map<String, Object> det : detections) {
                int classId = (int) det.get("class_id");
                switch (classId) {
                    case 0: fireCount++; break;
                    case 1: humanCount++; break;
                    case 2: smokeCount++; break;
                }
            }

            int totalCount = fireCount + humanCount + smokeCount;

            // 4. 保存识别记录
            DetectionRecord record = new DetectionRecord();
            record.setForestAreaId(forestAreaId);
            record.setImageOriginal("original/" + newFileName);
            record.setImageResult("result/" + newFileName);
            record.setDetectionTime(LocalDateTime.now());
            record.setTotalCount(totalCount);
            record.setFireCount(fireCount);
            record.setHumanCount(humanCount);
            record.setSmokeCount(smokeCount);
            record.setResultJson(resultJson);
            record.setUploadUserId(userId);
            detectionService.save(record);

            // 5. 如果有fire或smoke，自动生成工单
            List<Integer> workOrderIds = new ArrayList<>();
            boolean hasFireAndHuman = (fireCount > 0 && humanCount > 0);

            if (fireCount > 0 || smokeCount > 0) {
                int orderType = (fireCount > 0) ? 1 : 2;
                String hazardType = "";
                if (fireCount > 0) hazardType += "明火";
                if (smokeCount > 0) hazardType += "烟雾";

                if (hasFireAndHuman) {
                    hazardType = "违规用火人群（" + hazardType + "）";
                }

                WorkOrder order = new WorkOrder();
                order.setOrderNo("WO" + System.currentTimeMillis() + IdUtil.fastSimpleUUID().substring(0, 6));
                order.setDetectionRecordId(record.getId());
                order.setForestAreaId(forestAreaId);
                order.setOrderType(orderType);
                order.setOrderStatus(1);
                order.setHazardType(hazardType);
                order.setHazardDesc("AI识别到" + totalCount + "个目标：" + hazardType);
                workOrderService.save(order);
                workOrderIds.add(order.getId());
            }

            Map<String, Object> data = new HashMap<>();
            data.put("recordId", record.getId());
            data.put("detections", detections);
            data.put("totalCount", totalCount);
            data.put("fireCount", fireCount);
            data.put("humanCount", humanCount);
            data.put("smokeCount", smokeCount);
            data.put("firePeopleCount", hasFireAndHuman ? Math.min(fireCount, humanCount) : 0);
            data.put("workOrderIds", workOrderIds);

            return Result.success(data);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("识别失败：" + e.getMessage());
        }
    }
}