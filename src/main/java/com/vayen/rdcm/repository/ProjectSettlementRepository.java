package com.vayen.rdcm.repository;

import com.vayen.rdcm.entity.ProjectSettlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProjectSettlementRepository extends JpaRepository<ProjectSettlement, Long> {
    
    Optional<ProjectSettlement> findByProjectId(Long projectId);
}
