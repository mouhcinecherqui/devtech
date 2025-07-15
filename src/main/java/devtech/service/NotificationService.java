package devtech.service;

import devtech.domain.Notification;
import devtech.repository.NotificationRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public void notifyUser(String userLogin, String message, String type) {
        Notification notif = new Notification();
        notif.setUserLogin(userLogin);
        notif.setMessage(message);
        notif.setType(type);
        notif.setRead(false);
        notif.setCreatedDate(Instant.now());
        notificationRepository.save(notif);
    }
}
