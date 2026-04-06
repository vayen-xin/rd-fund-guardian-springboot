-- =====================================================
-- 项目管理系统 - 数据库设计 v4.0 (MySQL + JSON)
-- 基于现有项目 rd-fund-guardian 的数据库重构
-- 创建时间：2026-03-20
-- =====================================================

-- =====================================================
-- 1. 公司表（多租户核心）
-- =====================================================
DROP TABLE IF EXISTS `company`;
CREATE TABLE `company` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '公司ID',
  `name` VARCHAR(100) NOT NULL UNIQUE COMMENT '公司名称',
  `code` VARCHAR(50) NOT NULL UNIQUE COMMENT '公司编码',
  `status` ENUM('active','inactive') DEFAULT 'active' COMMENT '状态',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX `idx_company_code` (`code`),
  INDEX `idx_company_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公司表';

-- =====================================================
-- 2. 系统用户表（账号体系）
-- =====================================================
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
  `company_id` BIGINT NOT NULL COMMENT '所属公司ID',
  `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '登录账号',
  `name` VARCHAR(50) NOT NULL COMMENT '用户姓名',
  `role` ENUM('admin','branch_admin','employee') NOT NULL COMMENT '角色权限',
  `password_hash` VARCHAR(255) NOT NULL COMMENT '密码哈希（BCrypt）',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机',
  `is_active` BOOLEAN DEFAULT TRUE COMMENT '是否启用',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX `idx_company_id` (`company_id`),
  INDEX `idx_username` (`username`),
  INDEX `idx_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- 注意：保留原有的 user_account 表用于兼容，新系统使用 sys_user

-- =====================================================
-- 3. 项目表（增加 company_id 字段）
-- =====================================================
DROP TABLE IF EXISTS `project`;
CREATE TABLE `project` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '项目ID',
  `company_id` BIGINT NOT NULL COMMENT '所属公司ID',
  `project_name` VARCHAR(200) NOT NULL COMMENT '项目名称',
  `code` VARCHAR(100) DEFAULT NULL UNIQUE COMMENT '项目编码',
  `status` ENUM('pending','ongoing','ended','settled') DEFAULT 'pending' COMMENT '项目状态：待开始/进行中/已结束/已结算',
  `start_date` DATE DEFAULT NULL COMMENT '项目开始日期',
  `end_date` DATE DEFAULT NULL COMMENT '项目结束日期',
  `manager_name` VARCHAR(50) DEFAULT NULL COMMENT '项目经理姓名（可能无系统账号）',
  `manager_phone` VARCHAR(20) DEFAULT NULL COMMENT '项目经理联系方式',
  `description` TEXT DEFAULT NULL COMMENT '项目描述',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX `idx_company_id` (`company_id`),
  INDEX `idx_status` (`status`),
  INDEX `idx_start_date` (`start_date`),
  INDEX `idx_end_date` (`end_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目表';

-- =====================================================
-- 4. 项目员工表（与系统账号无关，支持系数配置）
-- =====================================================
DROP TABLE IF EXISTS `project_employee`;
CREATE TABLE `project_employee` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '项目员工ID',
  `company_id` BIGINT NOT NULL COMMENT '所属公司ID',
  `project_id` BIGINT NOT NULL COMMENT '项目ID',
  `employee_id` BIGINT DEFAULT NULL COMMENT '关联员工表ID（可选）',
  `employee_name` VARCHAR(50) NOT NULL COMMENT '员工姓名',
  `employee_type` ENUM('formal','part_time') DEFAULT 'formal' COMMENT '员工类型：正式/兼职',
  `coefficient` DECIMAL(3,2) DEFAULT 0.70 COMMENT '默认费用系数（月度可覆盖）',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `department` VARCHAR(100) DEFAULT NULL COMMENT '所属部门',
  `notes` TEXT DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX `idx_company_id` (`company_id`),
  INDEX `idx_project_id` (`project_id`),
  INDEX `idx_employee_name` (`employee_name`),
  INDEX `idx_employee_id` (`employee_id`),
  UNIQUE KEY `uk_project_employee` (`project_id`, `employee_name`) COMMENT '同一项目下员工姓名唯一'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目员工表';

-- =====================================================
-- 5. 设备表
-- =====================================================
DROP TABLE IF EXISTS `device`;
CREATE TABLE `device` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '设备ID',
  `company_id` BIGINT NOT NULL COMMENT '所属公司ID',
  `device_name` VARCHAR(100) NOT NULL COMMENT '设备名称',
  `model` VARCHAR(100) DEFAULT NULL COMMENT '设备型号',
  `specification` TEXT DEFAULT NULL COMMENT '设备规格',
  `purchase_date` DATE DEFAULT NULL COMMENT '购买日期',
  `purchase_price` DECIMAL(12,2) DEFAULT NULL COMMENT '购买价格',
  `daily_depreciation` DECIMAL(10,2) DEFAULT NULL COMMENT '每日折旧单价（元/天）',
  `monthly_rental` DECIMAL(10,2) DEFAULT NULL COMMENT '每月租赁单价（元/月）',
  `status` ENUM('normal','maintenance','scrapped') DEFAULT 'normal' COMMENT '设备状态',
  `notes` TEXT DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX `idx_company_id` (`company_id`),
  INDEX `idx_device_name` (`device_name`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备表';

-- =====================================================
-- 6. 项目月度费用数据表（核心表 - JSON存储）
-- =====================================================
DROP TABLE IF EXISTS `project_monthly_data`;
CREATE TABLE `project_monthly_data` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  `company_id` BIGINT NOT NULL COMMENT '所属公司ID',
  `project_id` BIGINT NOT NULL COMMENT '项目ID',
  `work_month` DATE NOT NULL COMMENT '工作月份（YYYY-MM-01）',
  
  -- 8大类费用明细（JSON格式）
  `cost_data` JSON NOT NULL COMMENT '费用明细JSON结构',
  
  -- 冗余的总金额字段（快速查询用，避免解析JSON）
  `labor_total` DECIMAL(12,2) DEFAULT 0 COMMENT '人员人工费用合计',
  `direct_material_total` DECIMAL(12,2) DEFAULT 0 COMMENT '直接投入-材料费',
  `direct_fuel_total` DECIMAL(12,2) DEFAULT 0 COMMENT '直接投入-燃料动力费',
  `direct_rental_total` DECIMAL(12,2) DEFAULT 0 COMMENT '直接投入-设备租赁费',
  `depreciation_total` DECIMAL(12,2) DEFAULT 0 COMMENT '折旧费用',
  `amortization_total` DECIMAL(12,2) DEFAULT 0 COMMENT '无形资产摊销',
  `design_total` DECIMAL(12,2) DEFAULT 0 COMMENT '设计费用',
  `commissioning_total` DECIMAL(12,2) DEFAULT 0 COMMENT '装备调试与试验费',
  `outsourced_total` DECIMAL(12,2) DEFAULT 0 COMMENT '委托外部研发（折后）',
  `other_total` DECIMAL(12,2) DEFAULT 0 COMMENT '其他费用',
  `grand_total` DECIMAL(12,2) DEFAULT 0 COMMENT '所有费用总计',
  
  -- 状态和版本
  `version` INT DEFAULT 1 COMMENT '版本号（修改时递增）',
  `status` ENUM('draft','finalized','settled') DEFAULT 'draft' COMMENT '数据状态',
  
  `created_by` BIGINT NOT NULL COMMENT '创建人ID（sys_user.id）',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  
  UNIQUE KEY `uk_project_month` (`project_id`, `work_month`) COMMENT '项目+月份唯一',
  INDEX `idx_company_month` (`company_id`, `work_month`) COMMENT '公司+月份查询',
  INDEX `idx_project_status` (`project_id`, `status`) COMMENT '项目状态筛选'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目月度费用数据表（JSON存储）';

-- =====================================================
-- 7. 项目结算表（保留原有结构，增加company_id）
-- =====================================================
DROP TABLE IF EXISTS `project_settlement`;
CREATE TABLE `project_settlement` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '结算ID',
  `company_id` BIGINT NOT NULL COMMENT '所属公司ID',
  `project_id` BIGINT NOT NULL COMMENT '项目ID',
  `settlement_month` DATE NOT NULL COMMENT '结算月份（固定，YYYY-MM-01）',
  `total_amount` DECIMAL(12,2) DEFAULT NULL COMMENT '总成本',
  `status` ENUM('pending','approved','rejected','re_settled') DEFAULT 'pending' COMMENT '结算状态',
  `version` INT DEFAULT 1 COMMENT '版本号',
  `remark` TEXT DEFAULT NULL COMMENT '备注',
  `created_by` BIGINT NOT NULL COMMENT '创建人ID（sys_user.id）',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX `idx_company_settle` (`company_id`, `project_id`, `settlement_month`),
  INDEX `idx_settlement_month` (`settlement_month`),
  INDEX `idx_status` (`status`),
  UNIQUE KEY `uk_project_settlement_month` (`project_id`, `settlement_month`) COMMENT '项目-月份唯一结算'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目结算表';

-- =====================================================
-- 8. 项目操作日志表
-- =====================================================
DROP TABLE IF EXISTS `project_operation_log`;
CREATE TABLE `project_operation_log` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
  `company_id` BIGINT NOT NULL COMMENT '所属公司ID',
  `project_id` BIGINT NOT NULL COMMENT '项目ID',
  `operator_id` BIGINT NOT NULL COMMENT '操作人ID（sys_user.id）',
  `action` VARCHAR(50) NOT NULL COMMENT '操作类型',
  `target_type` VARCHAR(30) DEFAULT NULL COMMENT '目标类型：employee/device/cost/settlement',
  `target_id` BIGINT DEFAULT NULL COMMENT '目标记录ID',
  `old_value` TEXT DEFAULT NULL COMMENT '修改前值（JSON格式）',
  `new_value` TEXT DEFAULT NULL COMMENT '修改后值（JSON格式）',
  `remark` TEXT DEFAULT NULL COMMENT '附加说明',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  INDEX `idx_company_project` (`company_id`, `project_id`),
  INDEX `idx_operator` (`operator_id`),
  INDEX `idx_action` (`action`),
  INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目操作日志表';

-- =====================================================
-- 9. 系统操作日志表
-- =====================================================
DROP TABLE IF EXISTS `system_log`;
CREATE TABLE `system_log` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
  `company_id` BIGINT NOT NULL COMMENT '所属公司ID',
  `user_id` BIGINT NOT NULL COMMENT '操作用户ID（sys_user.id）',
  `module` VARCHAR(50) NOT NULL COMMENT '操作模块：user/device/permission/system/login',
  `action` VARCHAR(50) NOT NULL COMMENT '操作类型：create/update/delete/login/logout',
  `ip` VARCHAR(45) DEFAULT NULL COMMENT '操作IP',
  `user_agent` TEXT DEFAULT NULL COMMENT '浏览器/客户端信息',
  `details` TEXT DEFAULT NULL COMMENT '操作详情（JSON格式）',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  INDEX `idx_company_user` (`company_id`, `user_id`),
  INDEX `idx_module_action` (`module`, `action`),
  INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统操作日志表';

-- =====================================================
-- 初始化数据
-- =====================================================

-- 插入示例公司
INSERT INTO `company` (`name`, `code`) VALUES
('总公司', 'HQ'),
('北京分公司', 'BJ'),
('上海分公司', 'SH');

-- 插入超级管理员（密码：admin123 的 BCrypt 哈希）
-- BCrypt.hashpw("admin123", BCrypt.gensalt()) -> $2a$10$N9qo8uLOickgx2ZMRZoMy.4Yr8wqVh4vEfZ6yh7k5s8vW1YGelB0m
INSERT INTO `sys_user` (`company_id`, `username`, `name`, `role`, `password_hash`) VALUES
(1, 'admin', '系统管理员', 'admin', '$2a$10$N9qo8uLOickgx2ZMRZoMy.4Yr8wqVh4vEfZ6yh7k5s8vW1YGelB0m');

-- 插入测试员工（project_employee）
INSERT INTO `project_employee` (`company_id`, `project_id`, `employee_name`, `employee_type`) VALUES
(1, 1, '张三', 'formal'),
(1, 1, '李四', 'part_time');

-- 插入测试设备
INSERT INTO `device` (`company_id`, `device_name`, `daily_depreciation`) VALUES
(1, '投影仪', 50.00),
(1, '服务器', 200.00);

-- 插入测试月度数据（示例）
INSERT INTO `project_monthly_data` (`company_id`, `project_id`, `work_month`, `cost_data`, `grand_total`, `status`, `created_by`) VALUES
(1, 1, '2026-03-01', 
 '{
   "labor": [
     {"employee_name":"张三","employee_type":"formal","coefficient":0.75,"salary":{"hours":160,"amount":12000},"social_security":{"hours":160,"amount":2000},"housing_fund":{"hours":160,"amount":800}}
   ],
   "direct_material": [{"item_name":"A材料","quantity":50,"unit_price":100,"amount":5000}],
   "depreciation": [{"device_name":"投影仪","daily_depreciation":50,"usage_days":20,"amount":1000}]
 }',
 20000.00, 'draft', 1);

-- 插入测试项目
INSERT INTO `project` (`company_id`, `project_name`, `status`, `start_date`) VALUES
(1, '智能合规管理系统', 'ongoing', '2026-03-01');

COMMIT;
