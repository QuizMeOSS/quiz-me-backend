package com.quizme.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {

    /**
     * Fetches a batch of PENDING events, oldest first, with a row-level
     * lock so that multiple application instances can poll
     * concurrently without multiple instances reading same event.
     */
    @Query(value = """
            SELECT * FROM outbox_events
            WHERE status = 'PENDING'
            ORDER BY created_at ASC
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """,
            nativeQuery = true)
    // string is native SQL, not JPQL
    List<OutboxEvent> findUnprocessedEvents(@Param("batchSize") int batchSize);
}