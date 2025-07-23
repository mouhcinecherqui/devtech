package devtech.service;

import devtech.domain.Ticket;
import devtech.domain.TicketMessage;
import devtech.repository.TicketMessageRepository;
import devtech.repository.TicketRepository;
import devtech.security.SecurityUtils;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TicketMessageService {

    private static final Logger log = LoggerFactory.getLogger(TicketMessageService.class);

    private final TicketMessageRepository ticketMessageRepository;
    private final TicketRepository ticketRepository;

    public TicketMessageService(TicketMessageRepository ticketMessageRepository, TicketRepository ticketRepository) {
        this.ticketMessageRepository = ticketMessageRepository;
        this.ticketRepository = ticketRepository;
    }

    /**
     * Ajoute un message à un ticket
     */
    public TicketMessage addMessage(Long ticketId, String content, TicketMessage.AuthorType authorType, String authorLogin) {
        log.info("Tentative d'ajout de message pour le ticket {} avec le contenu: {}", ticketId, content);

        try {
            // Vérifier que le ticket existe
            Ticket ticket = ticketRepository
                .findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket non trouvé avec l'ID: " + ticketId));

            log.info("Ticket trouvé: {}", ticket.getId());

            // Vérifier que le contenu n'est pas vide
            if (content == null || content.trim().isEmpty()) {
                throw new RuntimeException("Le contenu du message ne peut pas être vide");
            }

            // Créer le message
            TicketMessage message = new TicketMessage(ticket, content.trim(), authorType, authorLogin);
            log.info("Message créé, tentative de sauvegarde...");

            // Sauvegarder le message
            TicketMessage savedMessage = ticketMessageRepository.save(message);
            log.info("Message sauvegardé avec succès, ID: {}", savedMessage.getId());

            // Vérifier que le message a bien été sauvegardé
            if (savedMessage.getId() == null) {
                throw new RuntimeException("Le message n'a pas été sauvegardé correctement");
            }

            return savedMessage;
        } catch (Exception e) {
            log.error("Erreur lors de l'ajout du message pour le ticket {}: {}", ticketId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Ajoute un message client à un ticket
     */
    public TicketMessage addClientMessage(Long ticketId, String content) {
        log.info("Ajout d'un message client pour le ticket {}", ticketId);

        try {
            String clientLogin = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new RuntimeException("Utilisateur non connecté"));

            log.info("Utilisateur connecté: {}", clientLogin);

            return addMessage(ticketId, content, TicketMessage.AuthorType.CLIENT, clientLogin);
        } catch (Exception e) {
            log.error("Erreur lors de l'ajout du message client pour le ticket {}: {}", ticketId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Ajoute un message admin à un ticket
     */
    public TicketMessage addAdminMessage(Long ticketId, String content) {
        log.info("Ajout d'un message admin pour le ticket {}", ticketId);

        try {
            String adminLogin = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new RuntimeException("Utilisateur non connecté"));

            log.info("Admin connecté: {}", adminLogin);

            return addMessage(ticketId, content, TicketMessage.AuthorType.ADMIN, adminLogin);
        } catch (Exception e) {
            log.error("Erreur lors de l'ajout du message admin pour le ticket {}: {}", ticketId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Récupère tous les messages d'un ticket (pour les admins)
     */
    public List<TicketMessage> getTicketMessages(Long ticketId) {
        try {
            log.info("Récupération de tous les messages pour le ticket {}", ticketId);
            List<TicketMessage> messages = ticketMessageRepository.findByTicketIdOrderByCreatedDateAsc(ticketId);
            log.info("Nombre de messages trouvés: {}", messages.size());
            return messages;
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des messages pour le ticket {}: {}", ticketId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Récupère les messages publics d'un ticket (pour les clients)
     */
    public List<TicketMessage> getPublicTicketMessages(Long ticketId) {
        try {
            log.info("Récupération des messages publics pour le ticket {}", ticketId);
            List<TicketMessage> messages = ticketMessageRepository.findPublicMessagesByTicketIdOrderByCreatedDateAsc(ticketId);
            log.info("Nombre de messages publics trouvés: {}", messages.size());
            return messages;
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des messages publics pour le ticket {}: {}", ticketId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Compte le nombre de messages d'un ticket
     */
    public long getMessageCount(Long ticketId) {
        return ticketMessageRepository.countByTicketId(ticketId);
    }

    /**
     * Supprime tous les messages d'un ticket
     */
    public void deleteTicketMessages(Long ticketId) {
        ticketMessageRepository.deleteByTicketId(ticketId);
    }

    /**
     * Vérifie si un utilisateur peut voir les messages d'un ticket
     */
    public boolean canUserViewMessages(Long ticketId, String userLogin) {
        try {
            Ticket ticket = ticketRepository.findById(ticketId).orElse(null);
            if (ticket == null) return false;

            // Les admins peuvent voir tous les messages
            if (isAdmin(userLogin)) return true;

            // Les clients ne peuvent voir que leurs propres tickets
            return ticket.getCreatedBy().equals(userLogin);
        } catch (Exception e) {
            log.error("Erreur lors de la vérification des permissions pour voir les messages: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Vérifie si un utilisateur peut ajouter un message à un ticket
     */
    public boolean canUserAddMessage(Long ticketId, String userLogin) {
        try {
            Ticket ticket = ticketRepository.findById(ticketId).orElse(null);
            if (ticket == null) return false;

            // Les admins peuvent ajouter des messages à tous les tickets
            if (isAdmin(userLogin)) return true;

            // Les clients ne peuvent ajouter des messages qu'à leurs propres tickets
            return ticket.getCreatedBy().equals(userLogin);
        } catch (Exception e) {
            log.error("Erreur lors de la vérification des permissions pour ajouter des messages: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Vérifie si un utilisateur est admin
     */
    private boolean isAdmin(String userLogin) {
        // Cette méthode devrait être implémentée selon votre logique d'autorisation
        // Pour l'instant, on considère qu'un utilisateur est admin s'il a le rôle ADMIN
        return userLogin != null && (userLogin.contains("admin") || userLogin.contains("manager"));
    }
}
