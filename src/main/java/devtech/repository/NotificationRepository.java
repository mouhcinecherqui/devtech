package devtechly.repository;

import devtechly.domain.Notification;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Notification entity.
 */
@SuppressWarnings("unused")
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long>, JpaSpecificationExecutor<Notification> {
    /**
     * Find all notifications by user ID
     */
    List<Notification> findByUserId(Long userId);

    /**
     * Find notifications by user login with pagination
     */
    Page<Notification> findByUserLoginOrderByTimestampDesc(String userLogin, Pageable pageable);

    /**
     * Find notifications by user login OR email (for OAuth2: login = email; for User: login and email can differ)
     */
    Page<Notification> findByUserLoginInOrderByTimestampDesc(Collection<String> userLogins, Pageable pageable);

    /**
     * Find a notification for a specific user by id
     */
    Optional<Notification> findByIdAndUserLogin(Long id, String userLogin);

    /**
     * Find a notification by id when userLogin is one of the given identifiers (login or email)
     */
    Optional<Notification> findByIdAndUserLoginIn(Long id, Collection<String> userLogins);

    /**
     * Find all unread notifications
     */
    List<Notification> findByReadFalse();

    /**
     * Find all unread notifications by user ID
     */
    List<Notification> findByUserIdAndReadFalse(Long userId);

    /**
     * Find unread notifications by user login
     */
    List<Notification> findByUserLoginAndReadFalse(String userLogin);

    /**
     * Find unread notifications by user login or email
     */
    List<Notification> findByUserLoginInAndReadFalse(Collection<String> userLogins);

    /**
     * Count unread notifications by user ID
     */
    long countByUserIdAndReadFalse(Long userId);

    /**
     * Find notifications by ticket ID
     */
    List<Notification> findByTicketId(Long ticketId);

    /**
     * Find notifications by payment ID
     */
    List<Notification> findByPaymentId(Long paymentId);

    /**
     * Find notifications by type
     */
    List<Notification> findByType(String type);

    /**
     * Find notifications by type and user ID
     */
    List<Notification> findByTypeAndUserId(String type, Long userId);
}
