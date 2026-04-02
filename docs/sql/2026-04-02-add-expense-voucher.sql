CREATE TABLE IF NOT EXISTS `expense_voucher` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '凭证ID',
  `company_id` BIGINT NOT NULL COMMENT '所属公司ID',
  `project_id` BIGINT NOT NULL COMMENT '所属项目ID',
  `year_month` VARCHAR(7) NOT NULL COMMENT '所属月份 yyyy-MM',
  `category` VARCHAR(32) NOT NULL COMMENT '费用分类',
  `original_file_name` VARCHAR(255) NOT NULL COMMENT '原始文件名',
  `stored_file_name` VARCHAR(255) NOT NULL COMMENT '存储文件名',
  `relative_path` VARCHAR(500) NOT NULL COMMENT '相对存储路径',
  `content_type` VARCHAR(100) DEFAULT NULL COMMENT '文件类型',
  `file_size` BIGINT DEFAULT NULL COMMENT '文件大小',
  `uploaded_by` BIGINT NOT NULL COMMENT '上传人ID',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX `idx_voucher_company_project` (`company_id`, `project_id`),
  INDEX `idx_voucher_month_category` (`year_month`, `category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='费用凭证文件表';
