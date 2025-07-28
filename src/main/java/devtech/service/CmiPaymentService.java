package devtech.service;

import devtech.domain.Paiement;
import devtech.repository.PaiementRepository;
import devtech.service.dto.PaiementDTO;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CmiPaymentService {

    private static final Logger log = LoggerFactory.getLogger(CmiPaymentService.class);

    private String merchantId = "12345678";
    private String storeKey = "your-secret-store-key-here";
    private String storeName = "DevTech Store";
    private String gatewayUrl = "https://testpayment.cmi.co.ma/fim/est3Dgate";
    private String callbackUrl = "http://localhost:8080/api/paiements/cmi-callback";
    private String okUrl = "http://localhost:4200/payment-success";
    private String failUrl = "http://localhost:4200/payment-failed";

    private final PaiementRepository paiementRepository;

    public CmiPaymentService(PaiementRepository paiementRepository) {
        this.paiementRepository = paiementRepository;
    }

    /**
     * Crée une demande de paiement CMI
     */
    public Map<String, String> createPaymentRequest(PaiementDTO paiementDTO, String clientIp) {
        try {
            // Générer un ID de commande unique
            String orderId = generateOrderId();

            // Créer le paiement en base
            Paiement paiement = new Paiement();
            paiement.setUser(paiementDTO.user);
            paiement.setAmount(paiementDTO.amount);
            paiement.setCurrency(paiementDTO.currency);
            paiement.setDate(paiementDTO.date);
            paiement.setStatus("PENDING");
            paiement.setDescription(paiementDTO.description);
            paiement.setClientIp(clientIp);
            paiement.setCmiOrderId(orderId);
            paiement.setCmiMerchantId(merchantId);
            paiement.setCmiCreatedAt(LocalDateTime.now());
            paiement.setCmiUpdatedAt(LocalDateTime.now());

            paiementRepository.save(paiement);

            // Préparer les paramètres CMI
            Map<String, String> params = new HashMap<>();
            params.put("clientid", merchantId);
            params.put("storetype", "3D_PAY");
            params.put("hash", "");
            params.put("random", generateRandomString());
            params.put("amount", String.format("%.2f", paiementDTO.amount));
            params.put("currency", paiementDTO.currency);
            params.put("oid", orderId);
            params.put("okUrl", okUrl);
            params.put("failUrl", failUrl);
            params.put("rnd", generateRandomString());
            params.put("lang", "fr");
            params.put("email", paiementDTO.user + "@devtech.com");
            params.put("tel", "");
            params.put("BillToName", paiementDTO.user);
            params.put("BillToStreet1", "");
            params.put("BillToCity", "");
            params.put("BillToCountry", "MA");
            params.put("BillToPostalCode", "");
            params.put("ShipToName", paiementDTO.user);
            params.put("ShipToStreet1", "");
            params.put("ShipToCity", "");
            params.put("ShipToCountry", "MA");
            params.put("ShipToPostalCode", "");
            params.put("description", paiementDTO.description != null ? paiementDTO.description : "Paiement DevTech");

            // Générer le hash
            String hash = generateHash(params);
            params.put("hash", hash);

            log.info("Payment request created for order: {}", orderId);
            return params;
        } catch (Exception e) {
            log.error("Error creating payment request", e);
            throw new RuntimeException("Erreur lors de la création de la demande de paiement", e);
        }
    }

    /**
     * Traite la réponse CMI
     */
    public PaiementDTO processPaymentResponse(Map<String, String> responseParams) {
        try {
            String orderId = responseParams.get("oid");
            String responseCode = responseParams.get("Response");
            String responseMessage = responseParams.get("ErrMsg");
            String transactionId = responseParams.get("TransId");
            String approvalCode = responseParams.get("AuthCode");
            String hash = responseParams.get("HASH");

            // Vérifier le hash
            if (!verifyHash(responseParams, hash)) {
                log.error("Hash verification failed for order: {}", orderId);
                throw new RuntimeException("Vérification du hash échouée");
            }

            // Trouver le paiement
            Paiement paiement = paiementRepository
                .findByCmiOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Paiement non trouvé: " + orderId));

            // Mettre à jour le paiement
            paiement.setCmiTransactionId(transactionId);
            paiement.setCmiResponseCode(responseCode);
            paiement.setCmiResponseMessage(responseMessage);
            paiement.setCmiApprovalCode(approvalCode);
            paiement.setCmiUpdatedAt(LocalDateTime.now());

            // Déterminer le statut
            if ("Approved".equals(responseCode)) {
                paiement.setStatus("COMPLETED");
                paiement.setDate(java.time.LocalDate.now());
            } else {
                paiement.setStatus("FAILED");
            }

            // Mettre à jour les informations de carte si disponibles
            if (responseParams.containsKey("EXTRA_CARDBRAND")) {
                paiement.setCmiCardType(responseParams.get("EXTRA_CARDBRAND"));
            }
            if (responseParams.containsKey("EXTRA_CARDISSUER")) {
                paiement.setCmiPaymentMethod(responseParams.get("EXTRA_CARDISSUER"));
            }

            paiementRepository.save(paiement);

            log.info("Payment response processed for order: {} with status: {}", orderId, paiement.getStatus());
            return toDto(paiement);
        } catch (Exception e) {
            log.error("Error processing payment response", e);
            throw new RuntimeException("Erreur lors du traitement de la réponse de paiement", e);
        }
    }

    /**
     * Vérifie le statut d'un paiement
     */
    public PaiementDTO checkPaymentStatus(String orderId) {
        Paiement paiement = paiementRepository
            .findByCmiOrderId(orderId)
            .orElseThrow(() -> new RuntimeException("Paiement non trouvé: " + orderId));

        return toDto(paiement);
    }

    /**
     * Génère un ID de commande unique
     */
    private String generateOrderId() {
        return "DT" + System.currentTimeMillis() + "_" + generateRandomString(6);
    }

    /**
     * Génère une chaîne aléatoire
     */
    private String generateRandomString() {
        return generateRandomString(20);
    }

    private String generateRandomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * Génère le hash CMI
     */
    private String generateHash(Map<String, String> params) {
        try {
            // Construire la chaîne à hasher
            StringBuilder hashString = new StringBuilder();

            // Ajouter les paramètres dans l'ordre spécifique
            String[] hashParams = { "clientid", "amount", "oid", "okUrl", "failUrl", "rnd", "currency", "lang" };

            for (String param : hashParams) {
                if (params.containsKey(param)) {
                    hashString.append(params.get(param));
                }
            }

            // Ajouter la clé secrète
            hashString.append(storeKey);

            // Générer le hash SHA256
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(hashString.toString().getBytes(StandardCharsets.UTF_8));

            // Convertir en hex
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString().toUpperCase();
        } catch (Exception e) {
            log.error("Error generating hash", e);
            throw new RuntimeException("Erreur lors de la génération du hash", e);
        }
    }

    /**
     * Vérifie le hash de la réponse
     */
    private boolean verifyHash(Map<String, String> params, String receivedHash) {
        try {
            // Construire la chaîne à hasher
            StringBuilder hashString = new StringBuilder();

            // Ajouter les paramètres dans l'ordre spécifique pour la vérification
            String[] hashParams = { "clientid", "oid", "Response", "AuthCode", "ProcReturnCode", "TransId", "ErrMsg" };

            for (String param : hashParams) {
                if (params.containsKey(param)) {
                    hashString.append(params.get(param));
                }
            }

            // Ajouter la clé secrète
            hashString.append(storeKey);

            // Générer le hash SHA256
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(hashString.toString().getBytes(StandardCharsets.UTF_8));

            // Convertir en hex
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            String calculatedHash = hexString.toString().toUpperCase();
            return calculatedHash.equals(receivedHash);
        } catch (Exception e) {
            log.error("Error verifying hash", e);
            return false;
        }
    }

    /**
     * Convertit l'entité en DTO
     */
    private PaiementDTO toDto(Paiement paiement) {
        PaiementDTO dto = new PaiementDTO();
        dto.id = paiement.getId();
        dto.user = paiement.getUser();
        dto.amount = paiement.getAmount();
        dto.currency = paiement.getCurrency();
        dto.date = paiement.getDate();
        dto.status = paiement.getStatus();
        dto.cmiTransactionId = paiement.getCmiTransactionId();
        dto.cmiOrderId = paiement.getCmiOrderId();
        dto.cmiResponseCode = paiement.getCmiResponseCode();
        dto.cmiResponseMessage = paiement.getCmiResponseMessage();
        dto.cmiApprovalCode = paiement.getCmiApprovalCode();
        dto.cmiMerchantId = paiement.getCmiMerchantId();
        dto.cmiPaymentMethod = paiement.getCmiPaymentMethod();
        dto.cmiCardType = paiement.getCmiCardType();
        dto.cmiCardNumber = paiement.getCmiCardNumber();
        dto.cmiCardHolder = paiement.getCmiCardHolder();
        dto.cmiInstallment = paiement.getCmiInstallment();
        dto.cmi3DSecure = paiement.getCmi3DSecure();
        dto.cmiIpAddress = paiement.getCmiIpAddress();
        dto.cmiUserAgent = paiement.getCmiUserAgent();
        dto.cmiCreatedAt = paiement.getCmiCreatedAt();
        dto.cmiUpdatedAt = paiement.getCmiUpdatedAt();
        dto.description = paiement.getDescription();
        dto.clientIp = paiement.getClientIp();
        dto.createdAt = paiement.getCreatedAt();
        dto.updatedAt = paiement.getUpdatedAt();
        return dto;
    }

    /**
     * Obtient l'URL de la passerelle CMI
     */
    public String getGatewayUrl() {
        return gatewayUrl;
    }
}
