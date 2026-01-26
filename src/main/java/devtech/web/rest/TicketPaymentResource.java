package devtechly.web.rest;

import devtechly.service.TicketPaymentService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ticket-payments")
public class TicketPaymentResource {

    private static final Logger log = LoggerFactory.getLogger(TicketPaymentResource.class);

    private final TicketPaymentService ticketPaymentService;

    public TicketPaymentResource(TicketPaymentService ticketPaymentService) {
        this.ticketPaymentService = ticketPaymentService;
    }

    /**
     * Vérifie si un ticket nécessite un paiement
     */
    @GetMapping("/check/{ticketType}")
    public ResponseEntity<Map<String, Object>> checkPaymentRequired(@PathVariable String ticketType) {
        log.debug("Vérification du paiement requis pour le type de ticket: {}", ticketType);

        boolean paymentRequired = ticketPaymentService.isPaymentRequired(ticketType);
        Double amount = ticketPaymentService.getPaymentAmount(ticketType);

        Map<String, Object> response = Map.of("paymentRequired", paymentRequired, "amount", amount, "currency", "MAD");

        return ResponseEntity.ok(response);
    }

    /**
     * Crée une demande de paiement pour un ticket
     */
    @PostMapping("/create/{ticketId}")
    public ResponseEntity<Map<String, String>> createPaymentRequest(@PathVariable Long ticketId, HttpServletRequest request) {
        log.debug("Création d'une demande de paiement pour le ticket: {}", ticketId);

        String clientIp = getClientIpAddress(request);
        Map<String, String> paymentRequest = ticketPaymentService.createTicketPaymentRequest(ticketId, clientIp);

        return ResponseEntity.ok(paymentRequest);
    }

    /**
     * Traite la réponse de paiement pour un ticket
     */
    @PostMapping("/callback")
    public ResponseEntity<String> processPaymentResponse(@RequestParam Map<String, String> responseParams) {
        log.debug("Traitement de la réponse de paiement pour ticket: {}", responseParams.get("oid"));

        try {
            ticketPaymentService.processTicketPaymentResponse(responseParams);
            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            log.error("Erreur lors du traitement de la réponse de paiement", e);
            return ResponseEntity.badRequest().body("ERROR");
        }
    }

    /**
     * Obtient le statut de paiement d'un ticket
     */
    @GetMapping("/status/{ticketId}")
    public ResponseEntity<Map<String, Object>> getPaymentStatus(@PathVariable Long ticketId) {
        log.debug("Obtention du statut de paiement pour le ticket: {}", ticketId);

        try {
            Map<String, Object> paymentInfo = ticketPaymentService.getTicketPaymentInfo(ticketId);
            return ResponseEntity.ok(paymentInfo);
        } catch (Exception e) {
            log.error("Erreur lors de l'obtention du statut de paiement", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Configure un ticket pour nécessiter un paiement
     */
    @PostMapping("/configure/{ticketId}")
    public ResponseEntity<Void> configurePayment(@PathVariable Long ticketId, @RequestParam String paymentType) {
        log.debug("Configuration du paiement pour le ticket {}: {}", ticketId, paymentType);

        try {
            ticketPaymentService.configureTicketPayment(ticketId, paymentType);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Erreur lors de la configuration du paiement", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Obtient l'adresse IP du client
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0];
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}
