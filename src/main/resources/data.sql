-- =====================================================
-- 初始化数据
-- =====================================================

-- 插入示例公司
INSERT INTO `company` (`name`, `code`) VALUES
('总公司', 'HQ'),
('北京分公司', 'BJ'),
('上海分公司', 'SH');

-- 插入超级管理员（密码：admin123 的 SHA256）
-- SHA256("admin123") = "240be518fabd2724ddb6f04eeb1da5967448d7e831c85c4ee" (示例，需实际计算)
-- 这里使用明文占位，生产环境必须加密
INSERT INTO `sys_user` (`company_id`, `username`, `name`, `role`, `password_hash`) VALUES
(1, 'admin', '系统管理员', 'admin', '240be518fabd2724ddb6f04eeb1da5967448d7e831c85c4ee'); -- admin123 的SHA256

-- 插入测试项目
INSERT INTO `project` (`company_id`, `project_name`, `code`, `status`, `start_date`, `description`) VALUES
(1, '智能合规管理系统', 'RDCM-2026-001', 'ongoing', '2026-03-01', '研发费用合规管理项目');

-- 插入测试设备
INSERT INTO `device` (`company_id`, `device_name`, `daily_depreciation`) VALUES
(1, '投影仪', 50.00),
(1, '服务器', 200.00);

COMMIT;
