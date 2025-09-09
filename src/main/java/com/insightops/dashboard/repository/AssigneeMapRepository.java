package com.insightops.dashboard.repository;

import com.insightops.dashboard.domain.AssigneeMap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AssigneeMapRepository extends JpaRepository<AssigneeMap, Long> {
    
    Optional<AssigneeMap> findByConsultingCategory_SmallCategoryId(Long consultingCategoryId);
}

