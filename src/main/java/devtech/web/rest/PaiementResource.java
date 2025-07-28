package devtech.web.rest;

import devtech.service.PaiementService;
import devtech.service.dto.PaiementDTO;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/paiements")
public class PaiementResource {

    private final PaiementService paiementService;

    public PaiementResource(PaiementService paiementService) {
        this.paiementService = paiementService;
    }

    @GetMapping
    public List<PaiementDTO> getAllPaiements() {
        return paiementService.findAll();
    }

    @GetMapping("/{id}")
    public PaiementDTO getPaiement(@PathVariable Long id) {
        return paiementService.findById(id);
    }

    @GetMapping("/user/{user}")
    public List<PaiementDTO> getPaiementsByUser(@PathVariable String user) {
        return paiementService.findByUser(user);
    }

    @GetMapping("/status/{status}")
    public List<PaiementDTO> getPaiementsByStatus(@PathVariable String status) {
        return paiementService.findByStatus(status);
    }

    @GetMapping("/recent")
    public List<PaiementDTO> getRecentPaiements() {
        return paiementService.findRecentPayments();
    }

    @GetMapping("/pending")
    public List<PaiementDTO> getPendingPaiements() {
        return paiementService.findPendingPayments();
    }

    @GetMapping("/today")
    public List<PaiementDTO> getTodayPaiements() {
        return paiementService.findCompletedPaymentsToday();
    }

    @GetMapping("/stats")
    public Map<String, Object> getPaymentStats() {
        return paiementService.getPaymentStats();
    }

    @GetMapping("/date-range")
    public List<PaiementDTO> getPaiementsByDateRange(@RequestParam LocalDate startDate, @RequestParam LocalDate endDate) {
        return paiementService.findByDateRange(startDate, endDate);
    }

    /**
     * Crée une demande de paiement CMI
     */
    @PostMapping("/create-payment")
    public ResponseEntity<Map<String, String>> createPayment(@RequestBody PaiementDTO paiementDTO, HttpServletRequest request) {
        String clientIp = getClientIpAddress(request);
        Map<String, String> paymentRequest = paiementService.createPaymentRequest(paiementDTO, clientIp);

        return ResponseEntity.ok(paymentRequest);
    }

    /**
     * Callback CMI - traite la réponse de paiement
     */
    @PostMapping("/cmi-callback")
    public ResponseEntity<String> cmiCallback(@RequestParam Map<String, String> responseParams) {
        try {
            PaiementDTO paiement = paiementService.processPaymentResponse(responseParams);

            // Rediriger vers la page de succès ou d'échec selon le statut
            if ("COMPLETED".equals(paiement.status)) {
                return ResponseEntity.ok("OK");
            } else {
                return ResponseEntity.ok("FAILED");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("ERROR: " + e.getMessage());
        }
    }

    /**
     * Vérifie le statut d'un paiement
     */
    @GetMapping("/status/check/{orderId}")
    public PaiementDTO checkPaymentStatus(@PathVariable String orderId) {
        return paiementService.checkPaymentStatus(orderId);
    }

    @GetMapping("/{id}/facture")
    public ResponseEntity<byte[]> getFacturePdf(@PathVariable Long id) {
        byte[] pdf = paiementService.generateFacturePdf(id);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=facture-paiement-" + id + ".pdf")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdf);
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
