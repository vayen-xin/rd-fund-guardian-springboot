package com.vayen.rdcm.repository;

import com.vayen.rdcm.entity.OperationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OperationLogRepository extends JpaRepository<OperationLog, Long> {
    
    List<OperationLog> findByOperatorId(Long operatorId);
    
    List<OperationLog> findByOperationType(String operationType);
    
    List<OperationLog> findAllByOrderByCreatedAtDesc();
}
