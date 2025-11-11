package devtech.repository;

import devtech.domain.Message;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Message entity.
 */
@SuppressWarnings("unused")
@Repository
public interface MessageRepository extends JpaRepository<Message, Long>, JpaSpecificationExecutor<Message> {
    /**
     * Find all messages by ticket ID ordered by timestamp ascending
     */
    List<Message> findByTicketIdOrderByTimestampAsc(Long ticketId);

    /**
     * Find all messages by ticket ID ordered by timestamp descending
     */
    List<Message> findByTicketIdOrderByTimestampDesc(Long ticketId);

    /**
     * Find recent messages since a specific timestamp ordered by timestamp descending
     */
    List<Message> findByTimestampAfterOrderByTimestampDesc(Instant timestamp, Pageable pageable);

    /**
     * Find recent messages from clients since a specific timestamp
     */
    List<Message> findByTimestampAfterAndIsFromClientTrueOrderByTimestampDesc(Instant timestamp, Pageable pageable);

    /**
     * Find all messages from clients for a specific ticket
     */
    List<Message> findByTicketIdAndIsFromClientTrueOrderByTimestampAsc(Long ticketId);

    /**
     * Find all messages from admins for a specific ticket
     */
    List<Message> findByTicketIdAndIsFromClientFalseOrderByTimestampAsc(Long ticketId);

    /**
     * Find messages by sender ID
     */
    List<Message> findBySenderIdOrderByTimestampDesc(Long senderId);

    /**
     * Find messages by sender email
     */
    List<Message> findBySenderEmailOrderByTimestampDesc(String senderEmail);

    /**
     * Count messages by ticket ID
     */
    long countByTicketId(Long ticketId);

    /**
     * Count unread messages from clients for a specific ticket
     */
    long countByTicketIdAndIsFromClientTrue(Long ticketId);

    /**
     * Find the latest message for a specific ticket
     */
    Optional<Message> findFirstByTicketIdOrderByTimestampDesc(Long ticketId);

    /**
     * Find the latest message from a client for a specific ticket
     */
    Optional<Message> findFirstByTicketIdAndIsFromClientTrueOrderByTimestampDesc(Long ticketId);

    /**
     * Find messages by ticket ID with pagination
     */
    Page<Message> findByTicketIdOrderByTimestampAsc(Long ticketId, Pageable pageable);
}
