package com.vayen.rdcm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vayen.rdcm.entity.ProjectSettlement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

/**
 * 项目结算 Mapper
 */
@Mapper
public interface ProjectSettlementMapper extends BaseMapper<ProjectSettlement> {
    
    /**
     * 检查项目某月是否已存在结算
     */
    @org.apache.ibatis.annotations.Select("SELECT COUNT(1) > 0 FROM project_settlement " +
            "WHERE project_id = #{projectId} AND company_id = #{companyId} AND settlement_month = #{settlementMonth}")
    boolean existsByProjectIdAndMonth(@Param("projectId") Long projectId,
                                      @Param("companyId") Long companyId,
                                      @Param("settlementMonth") LocalDate settlementMonth);
}
