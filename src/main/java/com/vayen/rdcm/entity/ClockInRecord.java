package com.vayen.rdcm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 打卡记录实体
 */
@Data
@TableName("clock_in_record")
public class ClockInRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("branch_id")
    private Long branchId;

    @TableField("employee_id")
    private Long employeeId;

    @TableField("work_date")
    private String workDate;

    @TableField("check_in")
    private LocalDateTime checkIn;

    @TableField("check_out")
    private LocalDateTime checkOut;

    @TableField("duration")
    private Double duration;

    @TableField("source")
    private String source;

    @TableField("create_time")
    private LocalDateTime createTime;
}