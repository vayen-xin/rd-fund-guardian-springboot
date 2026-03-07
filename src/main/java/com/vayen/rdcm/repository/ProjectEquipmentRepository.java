package com.vayen.rdcm.repository;

import com.vayen.rdcm.entity.ProjectEquipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectEquipmentRepository extends JpaRepository<ProjectEquipment, Long> {
    
    List<ProjectEquipment> findByProjectId(Long projectId);
    
    void deleteByProjectId(Long projectId);
}
