ALTER TABLE system_log
    ADD COLUMN status VARCHAR(20) NULL AFTER details,
    ADD COLUMN result_message VARCHAR(255) NULL AFTER status;
