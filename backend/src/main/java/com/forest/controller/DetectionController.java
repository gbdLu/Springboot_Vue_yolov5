package com.forest.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.forest.dto.PageParam;
import com.forest.dto.Result;
import com.forest.entity.DetectionRecord;
import com.forest.entity.ForestArea;
import com.forest.entity.Notification;
import com.forest.entity.Role;
import com.forest.entity.User;
import com.forest.entity.WorkOrder;
import com.forest.mapper.RoleMapper;
import com.forest.service.DetectionService;
import com.forest.service.ForestAreaService;
import com.forest.service.NotificationService;
import com.forest.service.UserService;
import com.forest.service.WorkOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
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
    private ForestAreaService forestAreaService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserService userService;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RestTemplate restTemplate;

    @PostMapping("/upload")
    public Result<Map<String, Object>> uploadAndDetect(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "forestAreaId", required = false) Integer forestAreaId,
            @RequestAttribute("userId") Integer userId) {

        try {
            // 1. 校验文件类型
            String originalFilename = file.getOriginalFilename();
            String ext = FileUtil.extName(originalFilename);
            String[] allowedExtensions = {"jpg", "jpeg", "png", "bmp", "webp"};
            boolean allowed = false;
            for (String allowedExt : allowedExtensions) {
                if (allowedExt.equalsIgnoreCase(ext)) {
                    allowed = true;
                    break;
                }
            }
            if (!allowed) {
                return Result.error("不支持的图片格式，仅支持 jpg/jpeg/png/bmp/webp");
            }

            // 2. 保存原图
            String newFileName = IdUtil.fastSimpleUUID() + "." + ext;
            String savePath = uploadPath + "original/" + newFileName;
            File saveFile = new File(savePath);
            if (!saveFile.getParentFile().exists()) {
                saveFile.getParentFile().mkdirs();
            }
            file.transferTo(saveFile);

            // 3. 调用FastAPI推理服务（multipart/form-data 文件上传）
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new FileSystemResource(saveFile));

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            String resultJson = restTemplate.postForObject(detectionUrl, requestEntity, String.class);
            Map<String, Object> detectionResult = objectMapper.readValue(resultJson, Map.class);

            // 4. 提取 result_image 并从 map 中移除（避免存入数据库过大）
            String resultImageBase64 = (String) detectionResult.remove("result_image");

            // 5. 解析结果
            List<Map<String, Object>> detections = (List<Map<String, Object>>) detectionResult.get("detections");
            int fireCount = 0, humanCount = 0, smokeCount = 0;

            for (Map<String, Object> det : detections) {
                int classId = ((Number) det.get("class_id")).intValue();
                switch (classId) {
                    case 0: fireCount++; break;
                    case 1: humanCount++; break;
                    case 2: smokeCount++; break;
                }
            }

            int totalCount = fireCount + humanCount + smokeCount;

            // 6. 保存结果图到文件
            String resultRelativePath = "result/" + newFileName;
            if (resultImageBase64 != null && !resultImageBase64.isEmpty()) {
                byte[] imageBytes = java.util.Base64.getDecoder().decode(resultImageBase64);
                String resultPath = uploadPath + resultRelativePath;
                File resultFile = new File(resultPath);
                if (!resultFile.getParentFile().exists()) {
                    resultFile.getParentFile().mkdirs();
                }
                try (FileOutputStream fos = new FileOutputStream(resultFile)) {
                    fos.write(imageBytes);
                }
            }

            // 7. 保存识别记录（result_json 已移除 result_image）
            String resultJsonForDB = objectMapper.writeValueAsString(detectionResult);
            DetectionRecord record = new DetectionRecord();
            record.setForestAreaId(forestAreaId);
            record.setImageOriginal("original/" + newFileName);
            record.setImageResult(resultRelativePath);
            record.setDetectionTime(LocalDateTime.now());
            record.setTotalCount(totalCount);
            record.setFireCount(fireCount);
            record.setHumanCount(humanCount);
            record.setSmokeCount(smokeCount);
            record.setResultJson(resultJsonForDB);
            record.setUploadUserId(userId);
            record.setStatus(1);
            record.setCreatedAt(LocalDateTime.now());
            detectionService.save(record);

            // 8. 如果有fire或smoke，自动生成工单
            List<Integer> workOrderIds = new ArrayList<>();
            boolean hasFireAndHuman = (fireCount > 0 && humanCount > 0);

            if (fireCount > 0 || smokeCount > 0) {
                int orderType = (fireCount > 0) ? 1 : 2;  // 1=一级紧急, 2=二级普通
                String hazardType = "";
                if (fireCount > 0) hazardType += "明火";
                if (smokeCount > 0) hazardType += (hazardType.isEmpty() ? "" : "、") + "烟雾";

                if (hasFireAndHuman) {
                    hazardType = "违规用火人群（" + hazardType + "）";
                }

                // 如果 forestAreaId 为 null，尝试获取默认林区
                Integer orderForestAreaId = forestAreaId;
                if (orderForestAreaId == null) {
                    List<ForestArea> areas = forestAreaService.list();
                    if (areas != null && !areas.isEmpty()) {
                        orderForestAreaId = areas.get(0).getId();
                    }
                }

                WorkOrder order = new WorkOrder();
                order.setOrderNo("WO" + System.currentTimeMillis() + IdUtil.fastSimpleUUID().substring(0, 6));
                order.setDetectionRecordId(record.getId());
                order.setForestAreaId(orderForestAreaId);
                order.setOrderType(orderType);
                order.setOrderStatus(1);  // 待指派
                order.setHazardType(hazardType);
                order.setHazardDesc("AI识别到" + totalCount + "个目标：" + hazardType);
                order.setCreatedAt(LocalDateTime.now());
                order.setUpdatedAt(LocalDateTime.now());
                workOrderService.save(order);
                workOrderIds.add(order.getId());

                // 发送站内消息给林区管理员（MANAGER）
                try {
                    List<User> allUsers = userService.list();
                    if (allUsers != null) {
                        for (User user : allUsers) {
                            // 只通知林区管理员（MANAGER），不通知巡林员（GUARD）
                            if (user.getRoleId() != null) {
                                Role userRole = roleMapper.selectById(user.getRoleId());
                                if (userRole != null && "MANAGER".equals(userRole.getRoleCode())) {
                                    Notification noti = new Notification();
                                    noti.setUserId(user.getId());
                                    noti.setTitle("新隐患工单生成");
                                    noti.setContent("AI识别到" + hazardType + "，已自动生成工单：" + order.getOrderNo());
                                    noti.setType("work_order");
                                    noti.setRelatedId(order.getId());
                                    noti.setIsRead(0);
                                    noti.setCreatedAt(LocalDateTime.now());
                                    notificationService.save(noti);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    // 通知发送失败不影响主流程
                    e.printStackTrace();
                }
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
            // 返回结果图 base64（带 data URI 前缀）
            if (resultImageBase64 != null && !resultImageBase64.isEmpty()) {
                data.put("resultImage", "data:image/jpeg;base64," + resultImageBase64);
            }

            return Result.success(data);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("识别失败：" + e.getMessage());
        }
    }

    @PostMapping("/batch-upload")
    public Result<List<Map<String, Object>>> batchUpload(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "forestAreaId", required = false) Integer forestAreaId,
            @RequestAttribute("userId") Integer userId) {

        List<Map<String, Object>> results = new ArrayList<>();
        String[] allowedExtensions = {"jpg", "jpeg", "png", "bmp", "webp"};

        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            Map<String, Object> fileResult = new HashMap<>();
            fileResult.put("fileName", file.getOriginalFilename());
            fileResult.put("index", i + 1);

            try {
                // 校验文件类型
                String originalFilename = file.getOriginalFilename();
                String ext = FileUtil.extName(originalFilename);
                boolean allowed = false;
                for (String allowedExt : allowedExtensions) {
                    if (allowedExt.equalsIgnoreCase(ext)) {
                        allowed = true;
                        break;
                    }
                }
                if (!allowed) {
                    fileResult.put("success", false);
                    fileResult.put("message", "不支持的图片格式");
                    results.add(fileResult);
                    continue;
                }

                // 保存原图
                String newFileName = IdUtil.fastSimpleUUID() + "." + ext;
                String savePath = uploadPath + "original/" + newFileName;
                File saveFile = new File(savePath);
                if (!saveFile.getParentFile().exists()) {
                    saveFile.getParentFile().mkdirs();
                }
                file.transferTo(saveFile);

                // 调用FastAPI推理服务
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.MULTIPART_FORM_DATA);
                MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                body.add("file", new FileSystemResource(saveFile));
                HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

                String resultJson = restTemplate.postForObject(detectionUrl, requestEntity, String.class);
                Map<String, Object> detectionResult = objectMapper.readValue(resultJson, Map.class);

                // 提取结果
                String resultImageBase64 = (String) detectionResult.remove("result_image");
                List<Map<String, Object>> detections = (List<Map<String, Object>>) detectionResult.get("detections");
                int fireCount = 0, humanCount = 0, smokeCount = 0;

                for (Map<String, Object> det : detections) {
                    int classId = ((Number) det.get("class_id")).intValue();
                    switch (classId) {
                        case 0: fireCount++; break;
                        case 1: humanCount++; break;
                        case 2: smokeCount++; break;
                    }
                }

                int totalCount = fireCount + humanCount + smokeCount;

                // 保存结果图
                String resultRelativePath = "result/" + newFileName;
                if (resultImageBase64 != null && !resultImageBase64.isEmpty()) {
                    byte[] imageBytes = java.util.Base64.getDecoder().decode(resultImageBase64);
                    String resultPath = uploadPath + resultRelativePath;
                    File resultFile = new File(resultPath);
                    if (!resultFile.getParentFile().exists()) {
                        resultFile.getParentFile().mkdirs();
                    }
                    try (FileOutputStream fos = new FileOutputStream(resultFile)) {
                        fos.write(imageBytes);
                    }
                }

                // 保存识别记录
                String resultJsonForDB = objectMapper.writeValueAsString(detectionResult);
                DetectionRecord record = new DetectionRecord();
                record.setForestAreaId(forestAreaId);
                record.setImageOriginal("original/" + newFileName);
                record.setImageResult(resultRelativePath);
                record.setDetectionTime(LocalDateTime.now());
                record.setTotalCount(totalCount);
                record.setFireCount(fireCount);
                record.setHumanCount(humanCount);
                record.setSmokeCount(smokeCount);
                record.setResultJson(resultJsonForDB);
                record.setUploadUserId(userId);
                record.setStatus(1);
                record.setCreatedAt(LocalDateTime.now());
                detectionService.save(record);

                // 9. 如果有fire或smoke，自动生成工单
                List<Integer> workOrderIds = new ArrayList<>();
                boolean hasFireAndHuman = (fireCount > 0 && humanCount > 0);

                if (fireCount > 0 || smokeCount > 0) {
                    int orderType = (fireCount > 0) ? 1 : 2;  // 1=一级紧急, 2=二级普通
                    String hazardType = "";
                    if (fireCount > 0) hazardType += "明火";
                    if (smokeCount > 0) hazardType += (hazardType.isEmpty() ? "" : "、") + "烟雾";

                    if (hasFireAndHuman) {
                        hazardType = "违规用火人群（" + hazardType + "）";
                    }

                    // 如果 forestAreaId 为 null，尝试获取默认林区
                    Integer orderForestAreaId = forestAreaId;
                    if (orderForestAreaId == null) {
                        List<ForestArea> areas = forestAreaService.list();
                        if (areas != null && !areas.isEmpty()) {
                            orderForestAreaId = areas.get(0).getId();
                        }
                    }

                    WorkOrder order = new WorkOrder();
                    order.setOrderNo("WO" + System.currentTimeMillis() + IdUtil.fastSimpleUUID().substring(0, 6));
                    order.setDetectionRecordId(record.getId());
                    order.setForestAreaId(orderForestAreaId);
                    order.setOrderType(orderType);
                    order.setOrderStatus(1);  // 待指派
                    order.setHazardType(hazardType);
                    order.setHazardDesc("AI识别到" + totalCount + "个目标：" + hazardType);
                    order.setCreatedAt(LocalDateTime.now());
                    order.setUpdatedAt(LocalDateTime.now());
                    workOrderService.save(order);
                    workOrderIds.add(order.getId());

                    // 发送站内消息给林区管理员（MANAGER）
                    try {
                        List<User> allUsers = userService.list();
                        if (allUsers != null) {
                            for (User user : allUsers) {
                                // 只通知林区管理员（MANAGER），不通知巡林员（GUARD）
                                if (user.getRoleId() != null) {
                                    Role userRole = roleMapper.selectById(user.getRoleId());
                                    if (userRole != null && "MANAGER".equals(userRole.getRoleCode())) {
                                        Notification noti = new Notification();
                                        noti.setUserId(user.getId());
                                        noti.setTitle("新隐患工单生成");
                                        noti.setContent("AI识别到" + hazardType + "，已自动生成工单：" + order.getOrderNo());
                                        noti.setType("work_order");
                                        noti.setRelatedId(order.getId());
                                        noti.setIsRead(0);
                                        noti.setCreatedAt(LocalDateTime.now());
                                        notificationService.save(noti);
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        // 通知发送失败不影响主流程
                        e.printStackTrace();
                    }
                }

                fileResult.put("success", true);
                fileResult.put("recordId", record.getId());
                fileResult.put("totalCount", totalCount);
                fileResult.put("fireCount", fireCount);
                fileResult.put("humanCount", humanCount);
                fileResult.put("smokeCount", smokeCount);
                fileResult.put("hasHazard", totalCount > 0);
                fileResult.put("workOrderIds", workOrderIds);
                // 返回结果图 base64（带 data URI 前缀）
                if (resultImageBase64 != null && !resultImageBase64.isEmpty()) {
                    fileResult.put("resultImage", "data:image/jpeg;base64," + resultImageBase64);
                }

            } catch (Exception e) {
                fileResult.put("success", false);
                fileResult.put("message", "识别失败：" + e.getMessage());
            }

            results.add(fileResult);
        }

        return Result.success(results);
    }

    @GetMapping("/history")
    public Result<Map<String, Object>> history(PageParam pageParam,
                                                @RequestParam(required = false) Integer forestAreaId) {
        Page<DetectionRecord> page = new Page<>(pageParam.getPageNum(), pageParam.getPageSize());
        LambdaQueryWrapper<DetectionRecord> wrapper = new LambdaQueryWrapper<>();
        if (forestAreaId != null) {
            wrapper.eq(DetectionRecord::getForestAreaId, forestAreaId);
        }
        wrapper.orderByDesc(DetectionRecord::getCreatedAt);
        Page<DetectionRecord> result = detectionService.page(page, wrapper);

        // 填充关联信息
        result.getRecords().forEach(record -> {
            if (record.getForestAreaId() != null) {
                ForestArea area = forestAreaService.getById(record.getForestAreaId());
                if (area != null) record.setForestAreaName(area.getAreaName());
            }
            if (record.getUploadUserId() != null) {
                User user = userService.getById(record.getUploadUserId());
                if (user != null) record.setUploadUserName(user.getRealName());
            }
        });

        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        return Result.success(data);
    }

    @GetMapping("/image/{recordId}")
    public void getImage(@PathVariable Integer recordId,
                         @RequestParam(value = "type", defaultValue = "original") String type,
                         jakarta.servlet.http.HttpServletResponse response) throws Exception {
        DetectionRecord record = detectionService.getById(recordId);
        if (record == null) {
            response.setStatus(404);
            return;
        }
        String imagePath;
        if ("result".equals(type)) {
            imagePath = record.getImageResult();
        } else {
            imagePath = record.getImageOriginal();
        }
        if (imagePath == null) {
            // 如果请求结果图但没有，回退到原图
            imagePath = record.getImageOriginal();
        }
        if (imagePath == null) {
            response.setStatus(404);
            return;
        }
        File file = new File(uploadPath + imagePath);
        if (!file.exists()) {
            response.setStatus(404);
            return;
        }
        response.setContentType("image/jpeg");
        response.setHeader("Cache-Control", "max-age=3600");
        response.getOutputStream().write(FileUtil.readBytes(file));
    }

    @GetMapping("/result-image/{recordId}")
    public void getResultImage(@PathVariable Integer recordId,
                               jakarta.servlet.http.HttpServletResponse response) throws Exception {
        DetectionRecord record = detectionService.getById(recordId);
        if (record == null || record.getImageResult() == null) {
            response.setStatus(404);
            return;
        }
        File file = new File(uploadPath + record.getImageResult());
        if (!file.exists()) {
            response.setStatus(404);
            return;
        }
        response.setContentType("image/jpeg");
        response.getOutputStream().write(FileUtil.readBytes(file));
    }

    @GetMapping("/detail/{id}")
    public Result<DetectionRecord> getDetail(@PathVariable Integer id) {
        DetectionRecord record = detectionService.getById(id);
        if (record == null) return Result.error("记录不存在");
        if (record.getForestAreaId() != null) {
            ForestArea area = forestAreaService.getById(record.getForestAreaId());
            if (area != null) record.setForestAreaName(area.getAreaName());
        }
        if (record.getUploadUserId() != null) {
            User user = userService.getById(record.getUploadUserId());
            if (user != null) record.setUploadUserName(user.getRealName());
        }
        return Result.success(record);
    }

    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Integer id,
                               @RequestAttribute("roleCode") String roleCode) {
        if (!"ADMIN".equals(roleCode)) {
            return Result.error("仅管理员可删除识别记录");
        }
        // 检查是否有关联工单
        LambdaQueryWrapper<WorkOrder> workOrderWrapper = new LambdaQueryWrapper<>();
        workOrderWrapper.eq(WorkOrder::getDetectionRecordId, id);
        long workOrderCount = workOrderService.count(workOrderWrapper);
        if (workOrderCount > 0) {
            return Result.error("请先删除关联工单，该记录关联了 " + workOrderCount + " 个工单");
        }
        detectionService.removeById(id);
        return Result.success("删除成功", null);
    }
}
