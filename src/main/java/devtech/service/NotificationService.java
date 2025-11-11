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

    /**
     * Notifier un utilisateur avec les détails de base
     */
    public void notifyUser(String userLogin, String message, String type) {
        notifyUser(userLogin, message, type, null, null, null);
    }

    /**
     * Notifier un utilisateur avec tous les détails
     */
    public void notifyUser(String userLogin, String message, String type, Long ticketId, String actionUrl, Long userId) {
        Notification notif = new Notification();
        notif.setUserLogin(userLogin);
        notif.setMessage(message);
        notif.setType(type);
        notif.setTicketId(ticketId);
        notif.setActionUrl(actionUrl);
        notif.setUserId(userId);
        notif.setRead(false);
        notif.setCreatedDate(Instant.now());
        notif.setTimestamp(Instant.now());

        // Définir un titre par défaut basé sur le type
        String title = getDefaultTitle(type);
        notif.setTitle(title);

        notificationRepository.save(notif);
    }

    /**
     * Notifier tous les administrateurs
     */
    public void notifyAdmins(String message, String type, Long ticketId, String actionUrl) {
        notifyUser("admin", message, type, ticketId, actionUrl, null);
    }

    /**
     * Notifier un client spécifique
     */
    public void notifyClient(String clientLogin, String message, String type, Long ticketId, String actionUrl) {
        notifyUser(clientLogin, message, type, ticketId, actionUrl, null);
    }

    /**
     * Obtenir un titre par défaut basé sur le type de notification
     */
    private String getDefaultTitle(String type) {
        switch (type) {
            case "TICKET_CREATED":
                return "Nouveau ticket créé";
            case "TICKET_UPDATED":
                return "Ticket mis à jour";
            case "MESSAGE_RECEIVED":
                return "Nouveau message reçu";
            case "PAYMENT_RECEIVED":
                return "Paiement reçu";
            case "PAYMENT_CONFIRMED":
                return "Paiement confirmé";
            default:
                return "Notification";
        }
    }
}
