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

CREATE TABLE IF NOT EXISTS `device_spec_template` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '规格模板ID',
    `device_type` VARCHAR(50) NOT NULL UNIQUE COMMENT '设备类型 电视/音响/麦克风/投影仪/其他',
    `status` TINYINT DEFAULT 1 COMMENT '状态 1启用 0停用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备类型规格模板表';

CREATE TABLE IF NOT EXISTS `device_spec_field` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '规格字段ID',
    `template_id` BIGINT NOT NULL COMMENT '所属规格模板ID',
    `field_key` VARCHAR(100) NOT NULL COMMENT '字段键',
    `field_label` VARCHAR(100) NOT NULL COMMENT '字段名称',
    `field_type` VARCHAR(20) NOT NULL DEFAULT 'text' COMMENT '字段类型 text/textarea/number/select/date/boolean',
    `required` TINYINT NOT NULL DEFAULT 0 COMMENT '是否必填 1是 0否',
    `options` TEXT DEFAULT NULL COMMENT '可选项JSON，仅select使用',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序号',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY `uk_template_field` (`template_id`, `field_key`),
    INDEX `idx_template_id` (`template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备规格模板字段表';

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

CREATE TABLE IF NOT EXISTS `inventory_batch` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '盘点批次ID',
    `batch_no` VARCHAR(40) NOT NULL UNIQUE COMMENT '批次编号',
    `batch_name` VARCHAR(100) NOT NULL COMMENT '批次名称',
    `scope_type` VARCHAR(10) NOT NULL COMMENT '盘点范围类型 FLOOR楼层 ROOM接待室',
    `floor_id` BIGINT DEFAULT NULL COMMENT '盘点楼层ID',
    `room_id` BIGINT DEFAULT NULL COMMENT '盘点接待室ID（按楼层时为空）',
    `operator` VARCHAR(50) NOT NULL COMMENT '盘点负责人',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态 0盘点中 1已提交 2已关闭',
    `snapshot_time` DATETIME NOT NULL COMMENT '快照生成时间',
    `submitted_at` DATETIME DEFAULT NULL COMMENT '提交时间',
    `closed_at` DATETIME DEFAULT NULL COMMENT '关闭时间',
    `total_count` INT NOT NULL DEFAULT 0 COMMENT '设备总数',
    `present_count` INT NOT NULL DEFAULT 0 COMMENT '在场数',
    `missing_count` INT NOT NULL DEFAULT 0 COMMENT '缺失数',
    `mismatch_count` INT NOT NULL DEFAULT 0 COMMENT '位置不符数',
    `repair_count` INT NOT NULL DEFAULT 0 COMMENT '待维修数',
    `checked_count` INT NOT NULL DEFAULT 0 COMMENT '已盘点数',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '批次备注',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_status` (`status`),
    INDEX `idx_scope` (`scope_type`, `floor_id`, `room_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='盘点批次表';

CREATE TABLE IF NOT EXISTS `inventory_item` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '盘点明细ID',
    `batch_id` BIGINT NOT NULL COMMENT '所属盘点批次ID',
    `device_id` BIGINT NOT NULL COMMENT '设备ID',
    `device_code` VARCHAR(50) NOT NULL COMMENT '设备编号（快照）',
    `device_name` VARCHAR(100) NOT NULL COMMENT '设备名称（快照）',
    `device_type` VARCHAR(50) NOT NULL COMMENT '设备类型（快照）',
    `snapshot_floor_id` BIGINT DEFAULT NULL COMMENT '快照楼层ID',
    `snapshot_room_id` BIGINT DEFAULT NULL COMMENT '快照接待室ID',
    `check_result` TINYINT DEFAULT NULL COMMENT '盘点结果 1在场 2缺失 3位置不符 4待维修，NULL未盘点',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '盘点备注',
    `ledger_floor_id` BIGINT DEFAULT NULL COMMENT '提交时台账楼层ID（差异基准）',
    `ledger_room_id` BIGINT DEFAULT NULL COMMENT '提交时台账接待室ID（差异基准）',
    `location_mismatch` TINYINT NOT NULL DEFAULT 0 COMMENT '提交时是否位置不符 1是 0否',
    `process_status` TINYINT DEFAULT NULL COMMENT '处理状态 1待处理 2已处理（缺失/位置不符）',
    `process_remark` VARCHAR(500) DEFAULT NULL COMMENT '处理备注',
    `processed_at` DATETIME DEFAULT NULL COMMENT '处理时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY `uk_batch_device` (`batch_id`, `device_id`),
    INDEX `idx_batch_id` (`batch_id`),
    INDEX `idx_check_result` (`batch_id`, `check_result`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='盘点明细表';

CREATE TABLE IF NOT EXISTS `room_activity` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '活动ID',
    `activity_no` VARCHAR(40) NOT NULL UNIQUE COMMENT '活动编号',
    `activity_name` VARCHAR(200) NOT NULL COMMENT '活动名称',
    `room_id` BIGINT NOT NULL COMMENT '接待室ID',
    `floor_id` BIGINT NOT NULL COMMENT '所属楼层ID（冗余，便于按楼层筛选）',
    `start_time` DATETIME NOT NULL COMMENT '开始时间',
    `end_time` DATETIME NOT NULL COMMENT '结束时间',
    `manager` VARCHAR(50) NOT NULL COMMENT '负责人',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态 0待开始 1进行中 2已结束（按当前时间懒推进）',
    `released_at` DATETIME DEFAULT NULL COMMENT '占用释放时间（到期自动释放或手动结束）',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_room_time` (`room_id`, `start_time`, `end_time`),
    INDEX `idx_floor_id` (`floor_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_start_end` (`start_time`, `end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='接待室活动占用表';

CREATE TABLE IF NOT EXISTS `device_replacement` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '替换记录ID',
    `replacement_no` VARCHAR(40) NOT NULL UNIQUE COMMENT '替换单号',
    `room_id` BIGINT NOT NULL COMMENT '接待室ID',
    `floor_id` BIGINT NOT NULL COMMENT '楼层ID（接待室所在楼层，便于筛选）',
    `faulty_device_id` BIGINT NOT NULL COMMENT '故障设备ID（卸下）',
    `faulty_device_code` VARCHAR(50) NOT NULL COMMENT '故障设备编号（快照）',
    `faulty_device_name` VARCHAR(100) NOT NULL COMMENT '故障设备名称（快照）',
    `faulty_device_type` VARCHAR(50) NOT NULL COMMENT '故障设备类型（快照）',
    `spare_device_id` BIGINT NOT NULL COMMENT '备用设备ID（换上）',
    `spare_device_code` VARCHAR(50) NOT NULL COMMENT '备用设备编号（快照）',
    `spare_device_name` VARCHAR(100) NOT NULL COMMENT '备用设备名称（快照）',
    `spare_device_type` VARCHAR(50) NOT NULL COMMENT '备用设备类型（快照）',
    `fault_phenomenon` VARCHAR(500) NOT NULL COMMENT '故障现象',
    `operator` VARCHAR(50) NOT NULL COMMENT '替换操作人（值班员）',
    `replacement_time` DATETIME NOT NULL COMMENT '替换时间',
    `process_result` TINYINT NOT NULL DEFAULT 1 COMMENT '处理结果 1待维修 2已修复 3已报废',
    `process_remark` VARCHAR(500) DEFAULT NULL COMMENT '处理备注（维修/报废说明）',
    `processed_by` VARCHAR(50) DEFAULT NULL COMMENT '处理登记人',
    `processed_at` DATETIME DEFAULT NULL COMMENT '处理登记时间',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_floor_room` (`floor_id`, `room_id`),
    INDEX `idx_faulty_device` (`faulty_device_id`),
    INDEX `idx_spare_device` (`spare_device_id`),
    INDEX `idx_process_result` (`process_result`),
    INDEX `idx_replacement_time` (`replacement_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='接待中影音设备故障应急替换记录表';

CREATE TABLE IF NOT EXISTS `room_activity_device` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '占用明细ID',
    `activity_id` BIGINT NOT NULL COMMENT '活动ID',
    `device_id` BIGINT NOT NULL COMMENT '设备ID',
    `device_code` VARCHAR(50) NOT NULL COMMENT '设备编号（快照）',
    `device_name` VARCHAR(100) NOT NULL COMMENT '设备名称（快照）',
    `device_type` VARCHAR(50) NOT NULL COMMENT '设备类型（快照）',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY `uk_activity_device` (`activity_id`, `device_id`),
    INDEX `idx_activity_id` (`activity_id`),
    INDEX `idx_device_id` (`device_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动占用影音设备明细表';

CREATE TABLE IF NOT EXISTS `visit_registration` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '来访单ID',
    `visit_no` VARCHAR(40) NOT NULL UNIQUE COMMENT '来访单号',
    `visitor_org` VARCHAR(200) NOT NULL COMMENT '来访单位',
    `visitor_count` INT NOT NULL DEFAULT 1 COMMENT '预计人数',
    `room_id` BIGINT NOT NULL COMMENT '接待室ID',
    `floor_id` BIGINT NOT NULL COMMENT '所属楼层ID（冗余，便于按楼层筛选）',
    `start_time` DATETIME NOT NULL COMMENT '预计开始时间',
    `end_time` DATETIME NOT NULL COMMENT '预计结束时间',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态 0待到访 1接待中 2已结束 3已取消（接待中需值班员确认到场）',
    `checked_in_at` DATETIME DEFAULT NULL COMMENT '值班员确认到场时间',
    `cancelled_at` DATETIME DEFAULT NULL COMMENT '取消时间',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_room_time` (`room_id`, `start_time`, `end_time`),
    INDEX `idx_floor_id` (`floor_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_start_end` (`start_time`, `end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='来访登记表';

CREATE TABLE IF NOT EXISTS `device_combo` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '影音组合ID',
    `combo_name` VARCHAR(100) NOT NULL UNIQUE COMMENT '组合名称',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '组合说明',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    `created_by` VARCHAR(50) DEFAULT NULL COMMENT '创建人（管理员）',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='常用影音组合表';

CREATE TABLE IF NOT EXISTS `device_combo_item` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '组合明细ID',
    `combo_id` BIGINT NOT NULL COMMENT '所属组合ID',
    `device_id` BIGINT NOT NULL COMMENT '设备ID',
    `device_code` VARCHAR(50) NOT NULL COMMENT '设备编号（快照）',
    `device_name` VARCHAR(100) NOT NULL COMMENT '设备名称（快照）',
    `device_type` VARCHAR(50) NOT NULL COMMENT '设备类型（快照）',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序号',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY `uk_combo_device` (`combo_id`, `device_id`),
    INDEX `idx_combo_id` (`combo_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='常用影音组合设备明细表';

CREATE TABLE IF NOT EXISTS `device_combo_record` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '套用记录ID',
    `record_no` VARCHAR(40) NOT NULL UNIQUE COMMENT '套用单号',
    `combo_id` BIGINT NOT NULL COMMENT '组合ID',
    `combo_name` VARCHAR(100) NOT NULL COMMENT '组合名称（快照）',
    `room_id` BIGINT NOT NULL COMMENT '目标接待室ID',
    `floor_id` BIGINT NOT NULL COMMENT '目标楼层ID（冗余，便于筛选）',
    `operator` VARCHAR(50) NOT NULL COMMENT '套用人（值班员）',
    `apply_time` DATETIME NOT NULL COMMENT '套用时间',
    `required_count` INT NOT NULL DEFAULT 0 COMMENT '组合设备总数',
    `applied_count` INT NOT NULL DEFAULT 0 COMMENT '本次调入台数',
    `present_count` INT NOT NULL DEFAULT 0 COMMENT '套用前已在房间台数',
    `skipped_count` INT NOT NULL DEFAULT 0 COMMENT '跳过台数',
    `before_snapshot` TEXT DEFAULT NULL COMMENT '套用前接待室设备清单快照JSON',
    `after_snapshot` TEXT DEFAULT NULL COMMENT '套用后接待室设备清单快照JSON',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_floor_room` (`floor_id`, `room_id`),
    INDEX `idx_combo_id` (`combo_id`),
    INDEX `idx_apply_time` (`apply_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='影音组合一键套用记录表';

CREATE TABLE IF NOT EXISTS `device_combo_record_item` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '套用结果明细ID',
    `record_id` BIGINT NOT NULL COMMENT '套用记录ID',
    `device_id` BIGINT NOT NULL COMMENT '设备ID',
    `device_code` VARCHAR(50) NOT NULL COMMENT '设备编号（快照）',
    `device_name` VARCHAR(100) NOT NULL COMMENT '设备名称（快照）',
    `device_type` VARCHAR(50) NOT NULL COMMENT '设备类型（快照）',
    `result` TINYINT NOT NULL COMMENT '套用结果 1调入 2已在房间 3跳过',
    `skip_reason` VARCHAR(500) DEFAULT NULL COMMENT '跳过原因（在别的房间/待修/活动占用等）',
    `from_room_id` BIGINT DEFAULT NULL COMMENT '套用前所在接待室ID',
    `from_room_name` VARCHAR(100) DEFAULT NULL COMMENT '套用前所在接待室名称（快照）',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序号',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_record_id` (`record_id`),
    INDEX `idx_device_id` (`device_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='影音组合套用结果明细表';

-- 预置四类设备规格模板（模板停用不会清除历史设备规格数据）
INSERT IGNORE INTO `device_spec_template` (`id`, `device_type`, `status`) VALUES
    (1, '电视', 1),
    (2, '音响', 1),
    (3, '麦克风', 1),
    (4, '投影仪', 1);

INSERT IGNORE INTO `device_spec_field`
    (`id`, `template_id`, `field_key`, `field_label`, `field_type`, `required`, `options`, `sort_order`) VALUES
    (1,  1, 'screenSize',   '屏幕尺寸',   'number',   1, NULL, 1),
    (2,  1, 'resolution',   '分辨率',     'select',   1, '["720P","1080P","2K","4K","8K"]', 2),
    (3,  1, 'panelType',    '面板类型',   'select',   0, '["LCD","LED","OLED","QLED","Mini-LED"]', 3),
    (4,  1, 'smartSystem',  '智能系统',   'text',     0, NULL, 4),
    (5,  1, 'hasHdmi',      '是否有HDMI', 'boolean',  0, NULL, 5),
    (6,  2, 'power',        '功率(W)',    'number',   1, NULL, 1),
    (7,  2, 'channel',      '声道',       'select',   1, '["2.0","2.1","5.1","7.1"]', 2),
    (8,  2, 'connection',   '连接方式',   'select',   0, '["有线","蓝牙","有线+蓝牙"]', 3),
    (9,  2, 'frequencyRange','频率范围',  'text',     0, NULL, 4),
    (10, 3, 'pickupPattern','指向性',     'select',   1, '["全指向","单指向","双指向"]', 1),
    (11, 3, 'wireless',     '是否无线',   'boolean',  1, NULL, 2),
    (12, 3, 'batteryLife',  '续航时长(小时)','number',0, NULL, 3),
    (13, 3, 'interfaceType','接口类型',   'select',   0, '["USB","3.5mm","XLR卡侬","6.35mm"]', 4),
    (14, 4, 'brightness',   '亮度(流明)', 'number',   1, NULL, 1),
    (15, 4, 'resolution',   '分辨率',     'select',   1, '["SVGA","XGA","1080P","4K"]', 2),
    (16, 4, 'throwRatio',   '投射比',     'text',     0, NULL, 3),
    (17, 4, 'lampLife',     '灯泡寿命(小时)','number',0, NULL, 4),
    (18, 4, 'wirelessScreen','是否支持无线投屏','boolean',0, NULL, 5);
