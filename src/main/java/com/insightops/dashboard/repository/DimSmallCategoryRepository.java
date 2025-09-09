package com.insightops.dashboard.repository;

import com.insightops.dashboard.domain.DimSmallCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DimSmallCategoryRepository extends JpaRepository<DimSmallCategory, Long> {
    
    Optional<DimSmallCategory> findByCode(String code);
    Optional<DimSmallCategory> findByName(String name);
}

