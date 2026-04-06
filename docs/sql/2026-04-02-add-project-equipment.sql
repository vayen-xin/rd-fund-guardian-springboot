CREATE TABLE IF NOT EXISTS `project_equipment` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关联ID',
  `project_id` BIGINT NOT NULL COMMENT '项目ID',
  `device_id` BIGINT NOT NULL COMMENT '设备ID',
  `linked_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '关联时间',
  UNIQUE KEY `uk_project_device` (`project_id`, `device_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目设备关联表';
