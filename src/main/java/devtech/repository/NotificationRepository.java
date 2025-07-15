package devtech.repository;

import devtech.domain.Notification;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserLoginAndIsReadFalseOrderByCreatedDateDesc(String userLogin);
    List<Notification> findByUserLoginOrderByCreatedDateDesc(String userLogin);
}
