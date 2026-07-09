SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `floor` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '楼层ID',
    `floor_name` VARCHAR(50) NOT NULL COMMENT '楼层名称',
    `floor_number` INT NOT NULL COMMENT '楼层编号',
    `building_name` VARCHAR(50) DEFAULT NULL COMMENT '所属楼栋',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_floor_number` (`floor_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='楼层表';

CREATE TABLE IF NOT EXISTS `reception_room` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '接待室ID',
    `room_name` VARCHAR(100) NOT NULL COMMENT '接待室名称',
    `room_code` VARCHAR(50) NOT NULL UNIQUE COMMENT '接待室编号',
    `floor_id` BIGINT NOT NULL COMMENT '所属楼层ID',
    `capacity` INT DEFAULT 0 COMMENT '容纳人数',
    `equipment_count` INT DEFAULT 0 COMMENT '设备数量',
    `status` TINYINT DEFAULT 1 COMMENT '状态 1启用 0禁用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_floor_id` (`floor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='接待室表';

CREATE TABLE IF NOT EXISTS `device` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '设备ID',
    `device_code` VARCHAR(50) NOT NULL UNIQUE COMMENT '设备编号',
    `device_name` VARCHAR(100) NOT NULL COMMENT '设备名称',
    `device_type` VARCHAR(50) NOT NULL COMMENT '设备类型 电视/音响/麦克风/投影仪/其他',
    `brand` VARCHAR(50) DEFAULT NULL COMMENT '品牌',
    `model` VARCHAR(100) DEFAULT NULL COMMENT '型号',
    `spec_json` TEXT DEFAULT NULL COMMENT '规格参数JSON',
    `image_url` VARCHAR(500) DEFAULT NULL COMMENT '设备图片URL',
    `current_floor_id` BIGINT COMMENT '当前所属楼层ID',
    `current_room_id` BIGINT COMMENT '当前所属接待室ID',
    `status` TINYINT DEFAULT 1 COMMENT '状态 1正常 0损坏 2待维修',
    `purchase_date` DATE DEFAULT NULL COMMENT '采购日期',
    `warranty_end_date` DATE DEFAULT NULL COMMENT '保修截止日期',
    `created_by` VARCHAR(50) DEFAULT NULL COMMENT '创建人',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_device_code` (`device_code`),
    INDEX `idx_current_floor` (`current_floor_id`),
    INDEX `idx_current_room` (`current_room_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备表';

CREATE TABLE IF NOT EXISTS `device_transfer` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '流转记录ID',
    `device_id` BIGINT NOT NULL COMMENT '设备ID',
    `device_code` VARCHAR(50) NOT NULL COMMENT '设备编号',
    `from_floor_id` BIGINT COMMENT '原楼层ID',
    `from_room_id` BIGINT COMMENT '原接待室ID',
    `to_floor_id` BIGINT COMMENT '目标楼层ID',
    `to_room_id` BIGINT COMMENT '目标接待室ID',
    `transfer_reason` VARCHAR(500) DEFAULT NULL COMMENT '流转原因',
    `operator` VARCHAR(50) NOT NULL COMMENT '操作人',
    `transfer_time` DATETIME NOT NULL COMMENT '流转时间',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_device_id` (`device_id`),
    INDEX `idx_transfer_time` (`transfer_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备流转记录表';
