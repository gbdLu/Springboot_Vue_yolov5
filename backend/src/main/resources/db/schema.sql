-- ========================================
-- 林区明火智能监管系统 - 数据库初始化脚本
-- ========================================

CREATE DATABASE IF NOT EXISTS forest_fire_management DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE forest_fire_management;

-- 1. 角色表
DROP TABLE IF EXISTS `role`;
CREATE TABLE `role` (
  `id` int NOT NULL AUTO_INCREMENT,
  `role_name` varchar(50) NOT NULL COMMENT '角色名称',
  `role_code` varchar(50) NOT NULL COMMENT '角色编码',
  `description` varchar(200) DEFAULT NULL COMMENT '描述',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 2. 用户表
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `password` varchar(200) NOT NULL COMMENT '密码',
  `real_name` varchar(50) DEFAULT NULL COMMENT '真实姓名',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `role_id` int DEFAULT NULL COMMENT '角色ID',
  `status` int DEFAULT 1 COMMENT '状态: 0=禁用, 1=正常',
  `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
  `last_login_ip` varchar(50) DEFAULT NULL COMMENT '最后登录IP',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `fk_user_role` (`role_id`),
  CONSTRAINT `fk_user_role` FOREIGN KEY (`role_id`) REFERENCES `role` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 3. 林区表
DROP TABLE IF EXISTS `forest_area`;
CREATE TABLE `forest_area` (
  `id` int NOT NULL AUTO_INCREMENT,
  `area_name` varchar(100) NOT NULL COMMENT '林区名称',
  `location` varchar(200) DEFAULT NULL COMMENT '位置',
  `fire_risk_level` int DEFAULT 1 COMMENT '火险等级: 1=低, 2=中, 3=高',
  `manager_id` int DEFAULT NULL COMMENT '负责人ID',
  `total_area` decimal(10,2) DEFAULT NULL COMMENT '总面积(亩)',
  `description` text DEFAULT NULL COMMENT '描述',
  `status` int DEFAULT 1 COMMENT '状态: 0=禁用, 1=正常',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_area_manager` (`manager_id`),
  CONSTRAINT `fk_area_manager` FOREIGN KEY (`manager_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='林区表';

-- 4. 监控点表
DROP TABLE IF EXISTS `monitoring_point`;
CREATE TABLE `monitoring_point` (
  `id` int NOT NULL AUTO_INCREMENT,
  `forest_area_id` int NOT NULL COMMENT '所属林区ID',
  `point_name` varchar(100) NOT NULL COMMENT '监控点名称',
  `point_type` varchar(50) DEFAULT NULL COMMENT '类型: camera=摄像头, sensor=传感器',
  `longitude` decimal(10,6) DEFAULT NULL COMMENT '经度',
  `latitude` decimal(10,6) DEFAULT NULL COMMENT '纬度',
  `description` text DEFAULT NULL COMMENT '描述',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_point_area` (`forest_area_id`),
  CONSTRAINT `fk_point_area` FOREIGN KEY (`forest_area_id`) REFERENCES `forest_area` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='监控点表';

-- 5. 识别记录表
DROP TABLE IF EXISTS `detection_record`;
CREATE TABLE `detection_record` (
  `id` int NOT NULL AUTO_INCREMENT,
  `forest_area_id` int DEFAULT NULL COMMENT '林区ID',
  `image_original` varchar(500) DEFAULT NULL COMMENT '原图路径',
  `image_result` varchar(500) DEFAULT NULL COMMENT '结果图路径',
  `detection_time` datetime DEFAULT NULL COMMENT '识别时间',
  `total_count` int DEFAULT 0 COMMENT '总检测数',
  `fire_count` int DEFAULT 0 COMMENT '明火数',
  `human_count` int DEFAULT 0 COMMENT '人员数',
  `smoke_count` int DEFAULT 0 COMMENT '烟雾数',
  `result_json` longtext DEFAULT NULL COMMENT '识别结果JSON',
  `upload_user_id` int DEFAULT NULL COMMENT '上传用户ID',
  `status` int DEFAULT 1 COMMENT '状态',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_detection_area` (`forest_area_id`),
  KEY `fk_detection_user` (`upload_user_id`),
  CONSTRAINT `fk_detection_area` FOREIGN KEY (`forest_area_id`) REFERENCES `forest_area` (`id`),
  CONSTRAINT `fk_detection_user` FOREIGN KEY (`upload_user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='识别记录表';

-- 6. 工单表
DROP TABLE IF EXISTS `work_order`;
CREATE TABLE `work_order` (
  `id` int NOT NULL AUTO_INCREMENT,
  `order_no` varchar(50) NOT NULL COMMENT '工单编号',
  `detection_record_id` int DEFAULT NULL COMMENT '识别记录ID',
  `forest_area_id` int DEFAULT NULL COMMENT '林区ID',
  `order_type` int DEFAULT 1 COMMENT '工单类型: 1=一级紧急, 2=二级普通',
  `order_status` int DEFAULT 1 COMMENT '工单状态: 1=待指派, 2=已指派, 3=已处置, 4=已归档',
  `hazard_type` varchar(100) DEFAULT NULL COMMENT '隐患类型',
  `hazard_desc` text DEFAULT NULL COMMENT '隐患描述',
  `assigned_to` int DEFAULT NULL COMMENT '指派给',
  `assigned_by` int DEFAULT NULL COMMENT '指派者',
  `assigned_at` datetime DEFAULT NULL COMMENT '指派时间',
  `disposal_desc` text DEFAULT NULL COMMENT '处置描述',
  `disposal_images` varchar(1000) DEFAULT NULL COMMENT '处置图片',
  `disposal_at` datetime DEFAULT NULL COMMENT '处置时间',
  `review_result` int DEFAULT NULL COMMENT '审核结果: 1=通过, 2=退回',
  `review_comment` text DEFAULT NULL COMMENT '审核意见',
  `reviewed_by` int DEFAULT NULL COMMENT '审核人',
  `reviewed_at` datetime DEFAULT NULL COMMENT '审核时间',
  `closed_at` datetime DEFAULT NULL COMMENT '归档时间',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `fk_order_area` (`forest_area_id`),
  KEY `fk_order_detection` (`detection_record_id`),
  KEY `fk_order_assigned_to` (`assigned_to`),
  KEY `fk_order_assigned_by` (`assigned_by`),
  CONSTRAINT `fk_order_area` FOREIGN KEY (`forest_area_id`) REFERENCES `forest_area` (`id`),
  CONSTRAINT `fk_order_detection` FOREIGN KEY (`detection_record_id`) REFERENCES `detection_record` (`id`),
  CONSTRAINT `fk_order_assigned_to` FOREIGN KEY (`assigned_to`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_order_assigned_by` FOREIGN KEY (`assigned_by`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单表';

-- 7. 消息通知表
DROP TABLE IF EXISTS `notification`;
CREATE TABLE `notification` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL COMMENT '接收用户ID',
  `title` varchar(200) NOT NULL COMMENT '标题',
  `content` text DEFAULT NULL COMMENT '内容',
  `type` varchar(50) DEFAULT NULL COMMENT '类型: work_order=工单, system=系统',
  `related_id` int DEFAULT NULL COMMENT '关联ID',
  `is_read` int DEFAULT 0 COMMENT '是否已读: 0=未读, 1=已读',
  `read_at` datetime DEFAULT NULL COMMENT '已读时间',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_noti_user` (`user_id`),
  CONSTRAINT `fk_noti_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息通知表';

-- 8. 操作日志表
DROP TABLE IF EXISTS `operation_log`;
CREATE TABLE `operation_log` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int DEFAULT NULL COMMENT '操作用户ID',
  `username` varchar(50) DEFAULT NULL COMMENT '用户名',
  `operation` varchar(200) DEFAULT NULL COMMENT '操作描述',
  `method` varchar(20) DEFAULT NULL COMMENT '请求方法',
  `url` varchar(500) DEFAULT NULL COMMENT '请求URL',
  `ip` varchar(50) DEFAULT NULL COMMENT 'IP地址',
  `params` text DEFAULT NULL COMMENT '请求参数',
  `result` text DEFAULT NULL COMMENT '返回结果',
  `error_msg` text DEFAULT NULL COMMENT '错误信息',
  `duration` int DEFAULT 0 COMMENT '耗时(ms)',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_log_user` (`user_id`),
  CONSTRAINT `fk_log_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- 9. 知识库表
DROP TABLE IF EXISTS `knowledge`;
CREATE TABLE `knowledge` (
  `id` int NOT NULL AUTO_INCREMENT,
  `title` varchar(200) NOT NULL COMMENT '标题',
  `category` varchar(50) DEFAULT NULL COMMENT '分类',
  `content` text DEFAULT NULL COMMENT '内容',
  `cover_image` varchar(500) DEFAULT NULL COMMENT '封面图',
  `view_count` int DEFAULT 0 COMMENT '浏览量',
  `created_by` int DEFAULT NULL COMMENT '创建者ID',
  `status` int DEFAULT 1 COMMENT '状态: 0=草稿, 1=发布',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_knowledge_user` (`created_by`),
  CONSTRAINT `fk_knowledge_user` FOREIGN KEY (`created_by`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库表';

-- ========================================
-- 初始数据
-- ========================================

-- 角色初始数据
INSERT INTO `role` (`id`, `role_name`, `role_code`, `description`) VALUES
(1, '系统管理员', 'ADMIN', '拥有所有权限'),
(2, '林区管理员', 'MANAGER', '管理林区和工单'),
(3, '巡护员', 'GUARD', '图片上传、工单处置');

-- 用户初始数据 (密码: 123456 BCrypt加密)
INSERT INTO `user` (`id`, `username`, `password`, `real_name`, `phone`, `role_id`, `status`) VALUES
(1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '系统管理员', '13800000001', 1, 1),
(2, 'manager1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '张管理', '13800000002', 2, 1),
(3, 'guard1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '李巡护', '13800000003', 3, 1);

-- 林区初始数据
INSERT INTO `forest_area` (`id`, `area_name`, `location`, `fire_risk_level`, `manager_id`, `total_area`, `description`, `status`) VALUES
(1, '东山林区', '东经116.4° 北纬39.9°', 3, 2, 5000.00, '高火险等级林区，需重点监控', 1),
(2, '西湖林区', '东经120.1° 北纬30.2°', 2, 2, 3200.00, '中等火险等级林区', 1),
(3, '南岭林区', '东经113.2° 北纬24.8°', 1, 2, 8000.00, '低火险等级林区', 1);

-- 监控点初始数据
INSERT INTO `monitoring_point` (`id`, `forest_area_id`, `point_name`, `point_type`, `longitude`, `latitude`, `description`) VALUES
(1, 1, '东山入口摄像头', 'camera', 116.400000, 39.900000, '林区入口监控'),
(2, 1, '东山瞭望台', 'camera', 116.405000, 39.905000, '高点监控'),
(3, 2, '西湖入口', 'camera', 120.100000, 30.200000, '林区入口监控'),
(4, 2, '西湖林区传感器', 'sensor', 120.105000, 30.205000, '温湿度传感器');

-- 知识库初始数据
INSERT INTO `knowledge` (`id`, `title`, `category`, `content`, `view_count`, `created_by`, `status`) VALUES
(1, '森林防火基本知识', '防火知识', '森林防火是保护森林资源的重要措施。每年的森林防火期，需要加强巡查和监控，及时发现和处理火情。\n\n一、森林火灾的分类\n1. 地表火：沿地面蔓延的火\n2. 树冠火：在树冠层蔓延的火\n3. 地下火：在腐殖质层或泥炭层燃烧的火\n\n二、森林火灾的预防措施\n1. 加强防火宣传教育\n2. 严格野外用火管理\n3. 建设防火隔离带\n4. 配备专业灭火设备', 0, 1, 1),
(2, '灭火器使用方法', '灭火技能', '灭火器是扑救初起火灾的重要工具，正确使用灭火器可以有效控制火势。\n\n使用步骤：\n1. 提起灭火器\n2. 拔掉保险销\n3. 握住喷管对准火焰根部\n4. 压下压把进行喷射\n\n注意事项：\n- 站在上风向\n- 保持安全距离\n- 对准火焰根部喷射', 0, 1, 1),
(3, '林区巡护规范', '巡护规范', '林区巡护是森林防火的第一道防线。\n\n巡护要求：\n1. 每日至少巡护一次\n2. 重点区域增加巡护频次\n3. 携带必要通讯设备\n4. 做好巡护记录\n\n发现火情处理：\n1. 立即上报\n2. 判断火势大小\n3. 尝试初期灭火\n4. 疏散周边人员', 0, 1, 1);
