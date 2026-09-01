package com.forest.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.forest.dto.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/upload")
public class FileUploadController {

    @Value("${upload.path}")
    private String uploadPath;

    @PostMapping("/image")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            // 校验文件类型
            String originalFilename = file.getOriginalFilename();
            String ext = FileUtil.extName(originalFilename);
            String[] allowedExtensions = {"jpg", "jpeg", "png", "bmp", "webp", "gif"};
            boolean allowed = false;
            for (String allowedExt : allowedExtensions) {
                if (allowedExt.equalsIgnoreCase(ext)) {
                    allowed = true;
                    break;
                }
            }
            if (!allowed) {
                return Result.error("不支持的图片格式，仅支持 jpg/jpeg/png/bmp/webp/gif");
            }

            // 保存文件
            String newFileName = IdUtil.fastSimpleUUID() + "." + ext;
            String savePath = uploadPath + "images/" + newFileName;
            File saveFile = new File(savePath);
            if (!saveFile.getParentFile().exists()) {
                saveFile.getParentFile().mkdirs();
            }
            file.transferTo(saveFile);

            // 返回访问URL
            String url = "/api/upload/image/" + newFileName;
            return Result.success(url);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("上传失败：" + e.getMessage());
        }
    }

    @GetMapping("/image/{fileName}")
    public void getImage(@PathVariable String fileName,
                         jakarta.servlet.http.HttpServletResponse response) throws Exception {
        File file = new File(uploadPath + "images/" + fileName);
        if (!file.exists()) {
            response.setStatus(404);
            return;
        }
        String contentType = "image/jpeg";
        if (fileName.endsWith(".png")) {
            contentType = "image/png";
        } else if (fileName.endsWith(".gif")) {
            contentType = "image/gif";
        } else if (fileName.endsWith(".webp")) {
            contentType = "image/webp";
        }
        response.setContentType(contentType);
        response.setHeader("Cache-Control", "max-age=3600");
        response.getOutputStream().write(FileUtil.readBytes(file));
    }
}
