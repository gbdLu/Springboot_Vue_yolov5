package com.forest.utils;

import java.io.*;
import java.nio.file.*;

public class FileUtil {

    /**
     * 读取文件为字节数组
     */
    public static byte[] readBytes(File file) throws IOException {
        return Files.readAllBytes(file.toPath());
    }

    /**
     * 读取文件为字节数组（按路径）
     */
    public static byte[] readBytes(String path) throws IOException {
        return readBytes(new File(path));
    }

    /**
     * 获取文件扩展名
     */
    public static String getExtension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot + 1) : "";
    }

    /**
     * 创建目录（如果不存在）
     */
    public static void mkdir(String dirPath) {
        File dir = new File(dirPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }
}
