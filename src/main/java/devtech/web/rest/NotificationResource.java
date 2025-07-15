package devtech.web.rest;

import devtech.domain.Notification;
import devtech.repository.NotificationRepository;
import devtech.security.SecurityUtils;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@Transactional
public class NotificationResource {

    private final NotificationRepository notificationRepository;

    public NotificationResource(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    // Récupérer les notifications de l'utilisateur connecté
    @GetMapping("")
    public List<Notification> getMyNotifications() {
        String login = SecurityUtils.getCurrentUserLogin().orElse(null);
        if (login == null) return List.of();
        return notificationRepository.findByUserLoginOrderByCreatedDateDesc(login);
    }

    // Marquer une notification comme lue
    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        Notification notif = notificationRepository.findById(id).orElse(null);
        if (notif == null) return ResponseEntity.notFound().build();
        notif.setRead(true);
        notificationRepository.save(notif);
        return ResponseEntity.ok().build();
    }

    // Marquer toutes les notifications comme lues
    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead() {
        String login = SecurityUtils.getCurrentUserLogin().orElse(null);
        if (login == null) return ResponseEntity.badRequest().build();
        List<Notification> notifs = notificationRepository.findByUserLoginAndIsReadFalseOrderByCreatedDateDesc(login);
        for (Notification n : notifs) {
            n.setRead(true);
        }
        notificationRepository.saveAll(notifs);
        return ResponseEntity.ok().build();
    }
}
