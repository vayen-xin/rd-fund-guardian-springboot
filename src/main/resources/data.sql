-- =====================================================
-- 研发项目资金合规管理系统 - 数据库初始化脚本
-- 数据库：H2 Database
-- 版本：V1.0
-- 日期：2026-03-07
-- =====================================================

-- 如果表已存在则先删除（开发环境使用）
DROP TABLE IF EXISTS operation_log;
DROP TABLE IF EXISTS expense_voucher;
DROP TABLE IF EXISTS project_settlement;
DROP TABLE IF EXISTS project_equipment;
DROP TABLE IF EXISTS project_employee;
DROP TABLE IF EXISTS project;
DROP TABLE IF EXISTS clock_in_record;
DROP TABLE IF EXISTS equipment;
DROP TABLE IF EXISTS employee;
DROP TABLE IF EXISTS user_account;

-- =====================================================
-- 1. 用户账号表
-- =====================================================
CREATE TABLE user_account (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password_hash VARCHAR(255) NOT NULL COMMENT '密码（加密）',
    status INT DEFAULT 1 COMMENT '1 启用 0 停用',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
);

-- =====================================================
-- 2. 人员库表
-- =====================================================
CREATE TABLE employee (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL COMMENT '姓名',
    type VARCHAR(20) NOT NULL COMMENT '正式/兼职',
    status INT DEFAULT 1 COMMENT '1 启用 0 停用',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
);

-- =====================================================
-- 3. 设备库表
-- =====================================================
CREATE TABLE equipment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '设备名称',
    depreciation_rate DECIMAL(10,2) NOT NULL COMMENT '折旧单价（元/小时）',
    status INT DEFAULT 1 COMMENT '1 启用 0 停用',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
);

-- =====================================================
-- 4. 打卡记录表
-- =====================================================
CREATE TABLE clock_in_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL COMMENT '关联员工 ID（逻辑关联）',
    clock_in_time TIMESTAMP NOT NULL COMMENT '打卡时间',
    duration_hours DECIMAL(10,2) NOT NULL COMMENT '工时（小时）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
);

-- =====================================================
-- 5. 项目表
-- =====================================================
CREATE TABLE project (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL COMMENT '项目名称',
    start_time DATE NOT NULL COMMENT '开始时间',
    end_time DATE COMMENT '结束时间（可为空）',
    status VARCHAR(20) DEFAULT '进行中' COMMENT '进行中/已结束/已结算',
    created_by BIGINT NOT NULL COMMENT '创建人 ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
);

-- =====================================================
-- 6. 项目 - 员工关联表
-- =====================================================
CREATE TABLE project_employee (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL COMMENT '项目 ID',
    employee_id BIGINT NOT NULL COMMENT '员工 ID',
    coefficient DECIMAL(5,4) NOT NULL COMMENT '系数（如 0.7200 表示 72%）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
);

-- =====================================================
-- 7. 项目 - 设备关联表
-- =====================================================
CREATE TABLE project_equipment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL COMMENT '项目 ID',
    equipment_id BIGINT NOT NULL COMMENT '设备 ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
);

-- =====================================================
-- 8. 项目结算表
-- =====================================================
CREATE TABLE project_settlement (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL UNIQUE COMMENT '项目 ID',
    labor_cost DECIMAL(15,2) DEFAULT 0 COMMENT '人工费用总额',
    direct_input_cost DECIMAL(15,2) DEFAULT 0 COMMENT '直接投入费用',
    depreciation_cost DECIMAL(15,2) DEFAULT 0 COMMENT '折旧费用',
    intangible_amortization DECIMAL(15,2) DEFAULT 0 COMMENT '无形资产摊销',
    design_test_cost DECIMAL(15,2) DEFAULT 0 COMMENT '设计试验费用',
    outsourcing_cost DECIMAL(15,2) DEFAULT 0 COMMENT '外包合作费用',
    ip_cost DECIMAL(15,2) DEFAULT 0 COMMENT '知识产权费用',
    other_cost DECIMAL(15,2) DEFAULT 0 COMMENT '其他费用',
    total_amount DECIMAL(15,2) DEFAULT 0 COMMENT '合计金额',
    settled_by BIGINT NOT NULL COMMENT '结算人 ID',
    settled_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '结算时间'
);

-- =====================================================
-- 9. 费用凭证表
-- =====================================================
CREATE TABLE expense_voucher (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    settlement_id BIGINT NOT NULL COMMENT '关联结算 ID',
    expense_type VARCHAR(50) NOT NULL COMMENT '费用类型（八类之一）',
    amount DECIMAL(15,2) NOT NULL COMMENT '金额',
    voucher_files JSON COMMENT '凭证文件列表 [{name, path, uploadTime}]',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
);

-- =====================================================
-- 10. 操作日志表
-- =====================================================
CREATE TABLE operation_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    operator_id BIGINT NOT NULL COMMENT '操作人 ID',
    operation_type VARCHAR(50) NOT NULL COMMENT '操作类型',
    operation_detail JSON COMMENT '操作详情（动态内容）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
);

-- =====================================================
-- 索引创建（提升查询性能）
-- =====================================================
CREATE INDEX idx_employee_status ON employee(status);
CREATE INDEX idx_equipment_status ON equipment(status);
CREATE INDEX idx_clock_in_employee ON clock_in_record(employee_id);
CREATE INDEX idx_clock_in_time ON clock_in_record(clock_in_time);
CREATE INDEX idx_project_status ON project(status);
CREATE INDEX idx_project_created_by ON project(created_by);
CREATE INDEX idx_project_employee_project ON project_employee(project_id);
CREATE INDEX idx_project_employee_employee ON project_employee(employee_id);
CREATE INDEX idx_project_equipment_project ON project_equipment(project_id);
CREATE INDEX idx_project_equipment_equipment ON project_equipment(equipment_id);
CREATE INDEX idx_settlement_project ON project_settlement(project_id);
CREATE INDEX idx_voucher_settlement ON expense_voucher(settlement_id);
CREATE INDEX idx_operation_log_operator ON operation_log(operator_id);
CREATE INDEX idx_operation_log_type ON operation_log(operation_type);
CREATE INDEX idx_operation_log_time ON operation_log(created_at);

-- =====================================================
-- 初始测试数据（开发环境使用）
-- =====================================================

-- 默认管理员账号（密码：admin123，实际应加密存储）
INSERT INTO user_account (username, password_hash, status) VALUES 
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lqkkO9QS3TzCjH3rS', 1);

-- 测试员工数据
INSERT INTO employee (name, type, status) VALUES 
('张三', '正式', 1),
('李四', '正式', 1),
('王五', '兼职', 1);

-- 测试设备数据
INSERT INTO equipment (name, depreciation_rate, status) VALUES 
('服务器 01', 50.00, 1),
('测试电脑 01', 20.00, 1),
('开发笔记本 01', 30.00, 1);

-- 测试打卡记录（近 30 天）
INSERT INTO clock_in_record (employee_id, clock_in_time, duration_hours) VALUES 
(1, '2026-02-01 09:00:00', 8.0),
(1, '2026-02-02 09:00:00', 8.0),
(1, '2026-02-03 09:00:00', 8.0),
(2, '2026-02-01 09:00:00', 8.0),
(2, '2026-02-02 09:00:00', 8.0),
(3, '2026-02-01 09:00:00', 4.0);

-- 测试项目
INSERT INTO project (name, start_time, status, created_by) VALUES 
('AI 算法研发', '2026-02-01', '进行中', 1),
('数据分析平台', '2026-02-15', '进行中', 1);

-- 测试项目 - 员工关联（系数）
INSERT INTO project_employee (project_id, employee_id, coefficient) VALUES 
(1, 1, 0.7200),  -- 张三 72%
(1, 2, 0.5000),  -- 李四 50%
(2, 1, 0.3000),  -- 张三 30%
(2, 3, 0.8000);  -- 王五 80%

-- 测试项目 - 设备关联
INSERT INTO project_equipment (project_id, equipment_id) VALUES 
(1, 1),  -- AI 算法研发使用服务器 01
(1, 2),  -- AI 算法研发使用测试电脑 01
(2, 3);  -- 数据分析平台使用开发笔记本 01
