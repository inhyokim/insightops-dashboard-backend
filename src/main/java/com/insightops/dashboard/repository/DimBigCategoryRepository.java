package com.insightops.dashboard.repository;

import com.insightops.dashboard.domain.DimBigCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DimBigCategoryRepository extends JpaRepository<DimBigCategory, Long> {
    
    Optional<DimBigCategory> findByCode(String code);
}

