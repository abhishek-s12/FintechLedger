package com.abhishek.fintech.outbox.repository;

import com.abhishek.fintech.outbox.entity.OutboxEvent;
import com.abhishek.fintech.outbox.entity.OutboxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    @Query("SELECT o FROM OutboxEvent o WHERE o.status = :status ORDER BY o.createdAt ASC")
    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(@Param("status") OutboxStatus status, Pageable pageable);

    List<OutboxEvent> findByAggregateTypeAndAggregateId(String aggregateType, String aggregateId);
}
