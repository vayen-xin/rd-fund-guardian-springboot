package com.vayen.rdcm.repository;

import com.vayen.rdcm.entity.ProjectEmployee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectEmployeeRepository extends JpaRepository<ProjectEmployee, Long> {
    
    List<ProjectEmployee> findByProjectId(Long projectId);
    
    List<ProjectEmployee> findByEmployeeId(Long employeeId);
    
    void deleteByProjectId(Long projectId);
}
