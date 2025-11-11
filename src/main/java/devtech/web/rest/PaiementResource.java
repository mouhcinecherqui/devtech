package devtech.web.rest;

import devtech.service.PaiementService;
import devtech.service.dto.PaiementDTO;
import devtech.service.exception.PaiementException;
import devtech.web.rest.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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

    @GetMapping("/test")
    public ResponseEntity<String> testEndpoint() {
        return ResponseEntity.ok("Endpoint paiements accessible");
    }

    @GetMapping("/cmi/test")
    public ResponseEntity<Map<String, Object>> testCmiEndpoint() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Endpoint CMI accessible");
        response.put("timestamp", new java.util.Date());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public PaiementDTO getPaiement(@PathVariable Long id) {
        return paiementService.findById(id);
    }

    @PostMapping
    public ResponseEntity<?> createPaiement(@RequestBody PaiementDTO paiementDTO, HttpServletRequest request) {
        System.out.println("=== DEBUG: Méthode createPaiement appelée ===");
        System.out.println("Données reçues: " + paiementDTO);

        try {
            // Récupérer l'IP du client
            String clientIp = getClientIpAddress(request);
            paiementDTO.clientIp = clientIp;
            System.out.println("IP client: " + clientIp);

            PaiementDTO createdPaiement = paiementService.create(paiementDTO);
            System.out.println("Paiement créé avec succès: " + createdPaiement.id);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdPaiement);
        } catch (PaiementException e) {
            System.out.println("Erreur PaiementException: " + e.getMessage());
            ErrorResponse errorResponse = new ErrorResponse("Erreur de validation", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            System.out.println("Erreur Exception: " + e.getMessage());
            e.printStackTrace();
            ErrorResponse errorResponse = new ErrorResponse("Erreur interne du serveur", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updatePaiement(@PathVariable Long id, @RequestBody PaiementDTO paiementDTO) {
        try {
            PaiementDTO updatedPaiement = paiementService.update(id, paiementDTO);
            return ResponseEntity.ok(updatedPaiement);
        } catch (PaiementException e) {
            ErrorResponse errorResponse = new ErrorResponse("Erreur de validation", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            ErrorResponse errorResponse = new ErrorResponse("Erreur interne du serveur", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePaiement(@PathVariable Long id) {
        try {
            paiementService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (PaiementException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/user/{user}")
    public List<PaiementDTO> getPaiementsByUser(@PathVariable String user) {
        return paiementService.findByUser(user);
    }

    @GetMapping("/status/{status}")
    public List<PaiementDTO> getPaiementsByStatus(@PathVariable String status) {
        return paiementService.findByStatus(status);
    }

    /**
     * Crée une demande de paiement CMI pour un ticket
     */
    @PostMapping("/cmi/initiate")
    public ResponseEntity<Map<String, Object>> initiateCmiPayment(
        @RequestBody Map<String, Object> request,
        HttpServletRequest httpRequest
    ) {
        System.out.println("=== DEBUG: Méthode initiateCmiPayment appelée ===");
        System.out.println("Request reçu: " + request);

        try {
            Long ticketId = Long.valueOf(request.get("ticketId").toString());
            Double amount = Double.valueOf(request.get("amount").toString());
            String user = request.get("user").toString();
            String description = request.get("description") != null
                ? request.get("description").toString()
                : "Paiement ticket #" + ticketId;

            System.out.println(
                "Données extraites - ticketId: " + ticketId + ", amount: " + amount + ", user: " + user + ", description: " + description
            );

            System.out.println("Création du DTO de paiement...");
            // Créer le DTO de paiement
            PaiementDTO paiementDTO = new PaiementDTO();
            paiementDTO.user = user;
            paiementDTO.amount = amount;
            paiementDTO.currency = "MAD";
            paiementDTO.description = description;
            paiementDTO.clientIp = getClientIpAddress(httpRequest);

            System.out.println("DTO créé: " + paiementDTO);

            // Créer la demande de paiement CMI
            System.out.println("Appel de createCmiPaymentRequest...");
            Map<String, String> cmiParams = paiementService.createCmiPaymentRequest(paiementDTO);
            System.out.println("Paramètres CMI créés: " + cmiParams);

            // Retourner les paramètres CMI et l'URL de redirection
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("gatewayUrl", paiementService.getCmiGatewayUrl());
            response.put("params", cmiParams);
            response.put("orderId", cmiParams.get("oid"));

            System.out.println("Réponse finale: " + response);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.out.println("=== ERREUR dans initiateCmiPayment ===");
            System.out.println("Exception: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Callback CMI pour traiter les retours de paiement
     */
    @PostMapping("/cmi/callback")
    public ResponseEntity<String> cmiCallback(@RequestParam Map<String, String> params) {
        try {
            // Traiter la réponse CMI
            PaiementDTO paiementDTO = paiementService.processCmiPaymentResponse(params);

            // Rediriger vers la page de résultat appropriée
            String redirectUrl;
            if ("COMPLETED".equals(paiementDTO.status)) {
                redirectUrl = "http://localhost:3000/payment-result?status=success&oid=" + paiementDTO.cmiOrderId;
            } else {
                redirectUrl = "http://localhost:3000/payment-result?status=failed&oid=" + paiementDTO.cmiOrderId;
            }

            return ResponseEntity.ok().header("Location", redirectUrl).body("<script>window.location.href='" + redirectUrl + "';</script>");
        } catch (Exception e) {
            String redirectUrl = "http://localhost:3000/payment-result?status=failed&error=" + e.getMessage();
            return ResponseEntity.ok().header("Location", redirectUrl).body("<script>window.location.href='" + redirectUrl + "';</script>");
        }
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
