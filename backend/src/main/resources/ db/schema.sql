-- 创建数据库
CREATE DATABASE IF NOT EXISTS forest_fire_management DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE forest_fire_management;

-- 1. 角色表
CREATE TABLE `role` (
                        `id` INT PRIMARY KEY AUTO_INCREMENT,
                        `role_name` VARCHAR(50) NOT NULL,
                        `role_code` VARCHAR(50) NOT NULL UNIQUE,
                        `description` VARCHAR(255),
                        `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO `role` (`role_name`, `role_code`) VALUES
                                                  ('系统管理员', 'ADMIN'),
                                                  ('林区管护负责人', 'MANAGER'),
                                                  ('一线护林巡检员', 'GUARD');

-- 2. 用户表
CREATE TABLE `user` (
                        `id` INT PRIMARY KEY AUTO_INCREMENT,
                        `username` VARCHAR(50) NOT NULL UNIQUE,
                        `password` VARCHAR(255) NOT NULL,
                        `real_name` VARCHAR(50) NOT NULL,
                        `phone` VARCHAR(20),
                        `role_id` INT NOT NULL,
                        `status` TINYINT DEFAULT 1,
                        `last_login_time` DATETIME,
                        `last_login_ip` VARCHAR(50),
                        `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
                        `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        FOREIGN KEY (`role_id`) REFERENCES `role`(`id`)
);

-- 默认管理员：admin / 123456（密码需要BCrypt加密）
INSERT INTO `user` (`username`, `password`, `real_name`, `role_id`) VALUES
    ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKAt6Z5E6', '系统管理员', 1);

-- 3. 林区表
CREATE TABLE `forest_area` (
                               `id` INT PRIMARY KEY AUTO_INCREMENT,
                               `area_name` VARCHAR(100) NOT NULL,
                               `location` VARCHAR(255),
                               `fire_risk_level` TINYINT,
                               `manager_id` INT,
                               `total_area` DECIMAL(10,2),
                               `description` TEXT,
                               `status` TINYINT DEFAULT 1,
                               `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
                               `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                               FOREIGN KEY (`manager_id`) REFERENCES `user`(`id`)
);

-- 4. 监控点位表
CREATE TABLE `monitoring_point` (
                                    `id` INT PRIMARY KEY AUTO_INCREMENT,
                                    `forest_area_id` INT NOT NULL,
                                    `point_name` VARCHAR(100) NOT NULL,
                                    `point_type` VARCHAR(50),
                                    `longitude` DECIMAL(10,7),
                                    `latitude` DECIMAL(10,7),
                                    `description` TEXT,
                                    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
                                    FOREIGN KEY (`forest_area_id`) REFERENCES `forest_area`(`id`)
);

-- 5. 识别记录表（3类：fire, human, smoke）
CREATE TABLE `detection_record` (
                                    `id` INT PRIMARY KEY AUTO_INCREMENT,
                                    `forest_area_id` INT,
                                    `image_original` VARCHAR(255) NOT NULL,
                                    `image_result` VARCHAR(255),
                                    `detection_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
                                    `total_count` INT DEFAULT 0,
                                    `fire_count` INT DEFAULT 0,
                                    `human_count` INT DEFAULT 0,
                                    `smoke_count` INT DEFAULT 0,
                                    `result_json` JSON,
                                    `upload_user_id` INT,
                                    `status` TINYINT DEFAULT 1,
                                    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
                                    FOREIGN KEY (`forest_area_id`) REFERENCES `forest_area`(`id`),
                                    FOREIGN KEY (`upload_user_id`) REFERENCES `user`(`id`)
);

-- 6. 火情工单表
CREATE TABLE `work_order` (
                              `id` INT PRIMARY KEY AUTO_INCREMENT,
                              `order_no` VARCHAR(50) NOT NULL UNIQUE,
                              `detection_record_id` INT,
                              `forest_area_id` INT NOT NULL,
                              `order_type` TINYINT,
                              `order_status` TINYINT DEFAULT 1,
                              `hazard_type` VARCHAR(50),
                              `hazard_desc` VARCHAR(255),
                              `assigned_to` INT,
                              `assigned_by` INT,
                              `assigned_at` DATETIME,
                              `disposal_desc` TEXT,
                              `disposal_images` VARCHAR(500),
                              `disposal_at` DATETIME,
                              `review_result` TINYINT,
                              `review_comment` VARCHAR(255),
                              `reviewed_by` INT,
                              `reviewed_at` DATETIME,
                              `closed_at` DATETIME,
                              `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
                              `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                              FOREIGN KEY (`forest_area_id`) REFERENCES `forest_area`(`id`),
                              FOREIGN KEY (`assigned_to`) REFERENCES `user`(`id`),
                              FOREIGN KEY (`assigned_by`) REFERENCES `user`(`id`),
                              FOREIGN KEY (`reviewed_by`) REFERENCES `user`(`id`)
);

-- 7. 知识库表
CREATE TABLE `knowledge` (
                             `id` INT PRIMARY KEY AUTO_INCREMENT,
                             `title` VARCHAR(200) NOT NULL,
                             `category` VARCHAR(50) NOT NULL,
                             `content` LONGTEXT NOT NULL,
                             `cover_image` VARCHAR(255),
                             `view_count` INT DEFAULT 0,
                             `created_by` INT NOT NULL,
                             `status` TINYINT DEFAULT 1,
                             `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
                             `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                             FOREIGN KEY (`created_by`) REFERENCES `user`(`id`)
);

-- 8. 消息通知表
CREATE TABLE `notification` (
                                `id` INT PRIMARY KEY AUTO_INCREMENT,
                                `user_id` INT NOT NULL,
                                `title` VARCHAR(100) NOT NULL,
                                `content` VARCHAR(500) NOT NULL,
                                `type` VARCHAR(50),
                                `related_id` INT,
                                `is_read` TINYINT DEFAULT 0,
                                `read_at` DATETIME,
                                `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
                                FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
);

-- 9. 操作日志表
CREATE TABLE `operation_log` (
                                 `id` INT PRIMARY KEY AUTO_INCREMENT,
                                 `user_id` INT NOT NULL,
                                 `username` VARCHAR(50),
                                 `operation` VARCHAR(100) NOT NULL,
                                 `method` VARCHAR(100),
                                 `url` VARCHAR(255),
                                 `ip` VARCHAR(50),
                                 `params` TEXT,
                                 `result` VARCHAR(50),
                                 `error_msg` TEXT,
                                 `duration` INT,
                                 `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
);