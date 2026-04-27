-- Add tenant isolation column to project_equipment
ALTER TABLE `project_equipment`
  ADD COLUMN `company_id` BIGINT NULL COMMENT '公司ID' AFTER `id`;

-- Backfill company_id from project table
UPDATE `project_equipment` pe
JOIN `project` p ON p.id = pe.project_id
SET pe.company_id = p.company_id
WHERE pe.company_id IS NULL;

-- Enforce NOT NULL after backfill
ALTER TABLE `project_equipment`
  MODIFY COLUMN `company_id` BIGINT NOT NULL COMMENT '公司ID';

-- Replace old unique key if exists
ALTER TABLE `project_equipment`
  DROP INDEX `uk_project_device`;

ALTER TABLE `project_equipment`
  ADD UNIQUE KEY `uk_company_project_device` (`company_id`, `project_id`, `device_id`),
  ADD KEY `idx_company_project` (`company_id`, `project_id`);
