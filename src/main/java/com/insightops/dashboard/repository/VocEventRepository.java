package com.insightops.dashboard.repository;

import com.insightops.dashboard.domain.VocEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface VocEventRepository extends JpaRepository<VocEvent, Long> {

    interface VocEventProjection {
        Long getVocEventId();
        String getSourceId();
        Instant getReceivedAtUtc();
        LocalDate getConsultingDate();
        String getBigCategoryName();
        String getConsultingCategoryName();
        String getClientAge();
        String getClientGender();
    }

    @Query(value = """
        SELECT ve.voc_event_id as vocEventId,
               ve.source_id as sourceId,
               ve.received_at_utc as receivedAtUtc,
               ve.consulting_date as consultingDate,
               bc.name as bigCategoryName,
               sc.name as consultingCategoryName,
               ve.client_age as clientAge,
               ve.client_gender as clientGender
        FROM voc_event ve
        JOIN dim_big_category bc ON ve.big_category_id = bc.big_category_id
        JOIN dim_small_category sc ON ve.consulting_category_id = sc.small_category_id
        WHERE ve.consulting_date BETWEEN :from AND :to
        AND (:consultingCategoryId IS NULL OR ve.consulting_category_id = :consultingCategoryId)
        ORDER BY ve.consulting_date DESC, ve.received_at_utc DESC
        LIMIT :limit OFFSET :offset
        """, nativeQuery = true)
    List<VocEventProjection> findVocEvents(@Param("from") LocalDate from,
                                          @Param("to") LocalDate to,
                                          @Param("consultingCategoryId") Long consultingCategoryId,
                                          @Param("offset") int offset,
                                          @Param("limit") int limit);
}
