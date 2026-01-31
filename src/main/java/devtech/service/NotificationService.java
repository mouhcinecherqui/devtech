package devtechly.service;

import devtechly.domain.Notification;
import devtechly.domain.User;
import devtechly.repository.NotificationRepository;
import devtechly.repository.UserRepository;
import devtechly.security.AuthoritiesConstants;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    /**
     * Notifier un utilisateur avec les détails de base
     */
    public void notifyUser(String userLogin, String message, String type) {
        notifyUser(userLogin, message, type, null, null, null);
    }

    /**
     * Notifier un utilisateur avec tous les détails.
     * REQUIRES_NEW évite que toute exception (ex: contrainte) ne marque la transaction appelante comme rollback-only.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = Exception.class)
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
     * Notifier tous les administrateurs.
     * REQUIRES_NEW pour ne pas affecter la transaction de création de ticket en cas d'erreur.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = Exception.class)
    public void notifyAdmins(String message, String type, Long ticketId, String actionUrl) {
        List<User> admins = userRepository.findAllByAuthority(AuthoritiesConstants.ADMIN);

        if (admins == null || admins.isEmpty()) {
            LOG.warn("Aucun administrateur trouvé pour notification: {}", message);
            return;
        }

        for (User admin : admins) {
            // Utiliser le login système comme identifiant côté notifications
            notifyUser(admin.getLogin(), message, type, ticketId, actionUrl, admin.getId());
        }
    }

    /**
     * Notifier un client spécifique.
     * REQUIRES_NEW pour ne pas affecter la transaction de création de ticket en cas d'erreur.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = Exception.class)
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
