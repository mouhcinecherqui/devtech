package devtech.service;

import devtech.domain.Paiement;
import devtech.domain.Ticket;
import devtech.repository.PaiementRepository;
import devtech.repository.TicketRepository;
import devtech.service.dto.PaiementDTO;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TicketPaymentService {

    private static final Logger log = LoggerFactory.getLogger(TicketPaymentService.class);

    private final CmiPaymentService cmiPaymentService;
    private final TicketRepository ticketRepository;
    private final PaiementRepository paiementRepository;

    // Configuration des types de paiement
    private static final Map<String, Double> PAYMENT_TYPES = new HashMap<>();

    static {
        PAYMENT_TYPES.put("TICKET_CREATION", 50.0); // 50 MAD pour création de ticket
        PAYMENT_TYPES.put("TICKET_UPGRADE", 25.0); // 25 MAD pour mise à jour
        PAYMENT_TYPES.put("PRIORITY_ACCESS", 100.0); // 100 MAD pour accès prioritaire
    }

    public TicketPaymentService(
        CmiPaymentService cmiPaymentService,
        TicketRepository ticketRepository,
        PaiementRepository paiementRepository
    ) {
        this.cmiPaymentService = cmiPaymentService;
        this.ticketRepository = ticketRepository;
        this.paiementRepository = paiementRepository;
    }

    /**
     * Vérifie si un ticket nécessite un paiement
     */
    public boolean isPaymentRequired(String ticketType) {
        return PAYMENT_TYPES.containsKey(ticketType);
    }

    /**
     * Obtient le montant de paiement pour un type de ticket
     */
    public Double getPaymentAmount(String ticketType) {
        return PAYMENT_TYPES.getOrDefault(ticketType, 0.0);
    }

    /**
     * Crée une demande de paiement pour un ticket
     */
    public Map<String, String> createTicketPaymentRequest(Long ticketId, String clientIp) {
        try {
            Ticket ticket = ticketRepository.findById(ticketId).orElseThrow(() -> new RuntimeException("Ticket non trouvé: " + ticketId));

            if (!ticket.getPaymentRequired()) {
                throw new RuntimeException("Ce ticket ne nécessite pas de paiement");
            }

            // Créer le DTO de paiement
            PaiementDTO paiementDTO = new PaiementDTO();
            paiementDTO.user = ticket.getCreatedBy();
            paiementDTO.amount = ticket.getPaymentAmount();
            paiementDTO.currency = ticket.getPaymentCurrency();
            paiementDTO.date = LocalDate.now();
            paiementDTO.description = "Paiement pour ticket #" + ticketId + " - " + ticket.getPaymentType();

            // Créer la demande de paiement CMI
            Map<String, String> paymentRequest = cmiPaymentService.createPaymentRequest(paiementDTO, clientIp);

            // Mettre à jour le ticket avec l'ID de paiement
            ticket.setPaymentStatus("PENDING");
            ticketRepository.save(ticket);

            log.info("Demande de paiement créée pour le ticket {}: {}", ticketId, paymentRequest.get("oid"));
            return paymentRequest;
        } catch (Exception e) {
            log.error("Erreur lors de la création de la demande de paiement pour le ticket {}", ticketId, e);
            throw new RuntimeException("Erreur lors de la création de la demande de paiement", e);
        }
    }

    /**
     * Traite la réponse de paiement pour un ticket
     */
    public void processTicketPaymentResponse(Map<String, String> responseParams) {
        try {
            String orderId = responseParams.get("oid");

            // Traiter la réponse CMI
            PaiementDTO paiementDTO = cmiPaymentService.processPaymentResponse(responseParams);

            // Trouver le ticket associé au paiement
            Optional<Ticket> ticketOpt = ticketRepository.findByPaiementId(paiementDTO.id);
            if (ticketOpt.isPresent()) {
                Ticket ticket = ticketOpt.get();

                // Mettre à jour le statut du ticket selon le paiement
                if ("COMPLETED".equals(paiementDTO.status)) {
                    ticket.setPaymentStatus("COMPLETED");
                    ticket.setPaiementId(paiementDTO.id);

                    // Activer les fonctionnalités selon le type de paiement
                    activateTicketFeatures(ticket);

                    log.info("Paiement traité avec succès pour le ticket {}: {}", ticket.getId(), orderId);
                } else {
                    ticket.setPaymentStatus("FAILED");
                    log.warn("Paiement échoué pour le ticket {}: {}", ticket.getId(), orderId);
                }

                ticketRepository.save(ticket);
            }
        } catch (Exception e) {
            log.error("Erreur lors du traitement de la réponse de paiement", e);
            throw new RuntimeException("Erreur lors du traitement de la réponse de paiement", e);
        }
    }

    /**
     * Active les fonctionnalités du ticket selon le type de paiement
     */
    private void activateTicketFeatures(Ticket ticket) {
        switch (ticket.getPaymentType()) {
            case "TICKET_CREATION":
                // Le ticket est déjà créé, pas d'action supplémentaire
                break;
            case "TICKET_UPGRADE":
                // Améliorer la priorité du ticket
                ticket.setStatus("Prioritaire");
                break;
            case "PRIORITY_ACCESS":
                // Donner accès prioritaire
                ticket.setStatus("VIP");
                break;
            default:
                log.warn("Type de paiement non reconnu: {}", ticket.getPaymentType());
        }
    }

    /**
     * Vérifie le statut de paiement d'un ticket
     */
    public String getTicketPaymentStatus(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId).orElseThrow(() -> new RuntimeException("Ticket non trouvé: " + ticketId));

        return ticket.getPaymentStatus();
    }

    /**
     * Obtient les informations de paiement d'un ticket
     */
    public Map<String, Object> getTicketPaymentInfo(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId).orElseThrow(() -> new RuntimeException("Ticket non trouvé: " + ticketId));

        Map<String, Object> paymentInfo = new HashMap<>();
        paymentInfo.put("paymentRequired", ticket.getPaymentRequired());
        paymentInfo.put("paymentAmount", ticket.getPaymentAmount());
        paymentInfo.put("paymentCurrency", ticket.getPaymentCurrency());
        paymentInfo.put("paymentStatus", ticket.getPaymentStatus());
        paymentInfo.put("paymentType", ticket.getPaymentType());

        return paymentInfo;
    }

    /**
     * Configure un ticket pour nécessiter un paiement
     */
    public void configureTicketPayment(Long ticketId, String paymentType) {
        Ticket ticket = ticketRepository.findById(ticketId).orElseThrow(() -> new RuntimeException("Ticket non trouvé: " + ticketId));

        Double amount = getPaymentAmount(paymentType);
        if (amount > 0) {
            ticket.setPaymentRequired(true);
            ticket.setPaymentAmount(amount);
            ticket.setPaymentType(paymentType);
            ticket.setPaymentStatus("PENDING");
            ticketRepository.save(ticket);

            log.info("Configuration de paiement pour le ticket {}: {} MAD", ticketId, amount);
        }
    }
}
