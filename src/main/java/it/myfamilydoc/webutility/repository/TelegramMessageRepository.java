package it.myfamilydoc.webutility.repository;

import it.myfamilydoc.webutility.entity.TelegramMessage;
import it.myfamilydoc.webutility.entity.TelegramMessage.TelegramMessageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for TelegramMessage entity
 */
@Repository
public interface TelegramMessageRepository extends JpaRepository<TelegramMessage, Long> {

    /**
     * Find all messages ordered by creation date descending
     */
    List<TelegramMessage> findAllByOrderByCreatedAtDesc();

    /**
     * Find messages by status
     */
    List<TelegramMessage> findByStatusOrderByCreatedAtDesc(TelegramMessageStatus status);

    /**
     * Find messages by chat ID
     */
    List<TelegramMessage> findByChatIdOrderByCreatedAtDesc(String chatId);

    /**
     * Find messages created by user
     */
    List<TelegramMessage> findByCreatedByOrderByCreatedAtDesc(Long createdBy);

    /**
     * Find messages created after a specific date
     */
    List<TelegramMessage> findByCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime date);

    /**
     * Count messages by status
     */
    long countByStatus(TelegramMessageStatus status);

    /**
     * Get total count
     */
    @Query("SELECT COUNT(t) FROM TelegramMessage t")
    long countTotal();

    /**
     * Get statistics
     */
    @Query("SELECT t.status, COUNT(t) FROM TelegramMessage t GROUP BY t.status")
    List<Object[]> getStatusStatistics();
}