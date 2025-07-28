package devtech.service;

// PDF
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;
import devtech.domain.Paiement;
import devtech.repository.PaiementRepository;
import devtech.service.dto.PaiementDTO;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PaiementService {

    private final PaiementRepository paiementRepository;
    private final CmiPaymentService cmiPaymentService;

    public PaiementService(PaiementRepository paiementRepository, CmiPaymentService cmiPaymentService) {
        this.paiementRepository = paiementRepository;
        this.cmiPaymentService = cmiPaymentService;
    }

    public List<PaiementDTO> findAll() {
        return paiementRepository.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    public PaiementDTO findById(Long id) {
        return paiementRepository.findById(id).map(this::toDto).orElseThrow();
    }

    public PaiementDTO findByOrderId(String orderId) {
        return paiementRepository.findByCmiOrderId(orderId).map(this::toDto).orElseThrow();
    }

    /**
     * Crée une demande de paiement CMI
     */
    public Map<String, String> createPaymentRequest(PaiementDTO paiementDTO, String clientIp) {
        return cmiPaymentService.createPaymentRequest(paiementDTO, clientIp);
    }

    /**
     * Traite la réponse CMI
     */
    public PaiementDTO processPaymentResponse(Map<String, String> responseParams) {
        return cmiPaymentService.processPaymentResponse(responseParams);
    }

    /**
     * Vérifie le statut d'un paiement
     */
    public PaiementDTO checkPaymentStatus(String orderId) {
        return cmiPaymentService.checkPaymentStatus(orderId);
    }

    /**
     * Trouve tous les paiements d'un utilisateur
     */
    public List<PaiementDTO> findByUser(String user) {
        return paiementRepository.findByUserOrderByCreatedAtDesc(user).stream().map(this::toDto).collect(Collectors.toList());
    }

    /**
     * Trouve tous les paiements par statut
     */
    public List<PaiementDTO> findByStatus(String status) {
        return paiementRepository.findByStatusOrderByCreatedAtDesc(status).stream().map(this::toDto).collect(Collectors.toList());
    }

    /**
     * Trouve tous les paiements entre deux dates
     */
    public List<PaiementDTO> findByDateRange(LocalDate startDate, LocalDate endDate) {
        return paiementRepository
            .findByDateBetweenOrderByDateDesc(startDate, endDate)
            .stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    /**
     * Trouve les paiements récents (derniers 30 jours)
     */
    public List<PaiementDTO> findRecentPayments() {
        LocalDate startDate = LocalDate.now().minusDays(30);
        return paiementRepository.findRecentPayments(startDate).stream().map(this::toDto).collect(Collectors.toList());
    }

    /**
     * Trouve les paiements en attente
     */
    public List<PaiementDTO> findPendingPayments() {
        return paiementRepository.findPendingPayments().stream().map(this::toDto).collect(Collectors.toList());
    }

    /**
     * Trouve les paiements complétés aujourd'hui
     */
    public List<PaiementDTO> findCompletedPaymentsToday() {
        return paiementRepository.findCompletedPaymentsToday(LocalDate.now()).stream().map(this::toDto).collect(Collectors.toList());
    }

    /**
     * Obtient les statistiques de paiement
     */
    public Map<String, Object> getPaymentStats() {
        Map<String, Object> stats = new java.util.HashMap<>();

        // Compter par statut
        List<Object[]> statusCounts = paiementRepository.countByStatus();
        for (Object[] statusCount : statusCounts) {
            stats.put((String) statusCount[0], statusCount[1]);
        }

        // Total des paiements
        stats.put("total", paiementRepository.count());

        // Montant total des paiements complétés
        List<Paiement> completedPayments = paiementRepository.findByStatusOrderByCreatedAtDesc("COMPLETED");
        double totalAmount = completedPayments.stream().mapToDouble(p -> p.getAmount() != null ? p.getAmount() : 0.0).sum();
        stats.put("totalAmount", totalAmount);

        // Paiements aujourd'hui
        List<PaiementDTO> todayPayments = findCompletedPaymentsToday();
        stats.put("todayCount", todayPayments.size());
        double todayAmount = todayPayments.stream().mapToDouble(p -> p.amount != null ? p.amount : 0.0).sum();
        stats.put("todayAmount", todayAmount);

        return stats;
    }

    public byte[] generateFacturePdf(Long paiementId) {
        PaiementDTO paiement = findById(paiementId);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();

            // En-tête
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
            Font headerFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
            Font normalFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);

            document.add(new Paragraph("FACTURE DE PAIEMENT", titleFont));
            document.add(new Paragraph(" ")); // Espace

            // Informations de la facture
            document.add(new Paragraph("Numéro de facture: " + paiement.id, headerFont));
            document.add(new Paragraph("Date: " + paiement.date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), normalFont));
            document.add(new Paragraph(" ")); // Espace

            // Informations du client
            document.add(new Paragraph("INFORMATIONS CLIENT", headerFont));
            document.add(new Paragraph("Utilisateur: " + paiement.user, normalFont));
            document.add(new Paragraph(" ")); // Espace

            // Détails du paiement
            document.add(new Paragraph("DÉTAILS DU PAIEMENT", headerFont));
            document.add(new Paragraph("Montant: " + String.format("%.2f", paiement.amount) + " " + paiement.currency, normalFont));
            document.add(new Paragraph("Statut: " + paiement.status, normalFont));

            if (paiement.cmiTransactionId != null) {
                document.add(new Paragraph("ID Transaction: " + paiement.cmiTransactionId, normalFont));
            }
            if (paiement.cmiApprovalCode != null) {
                document.add(new Paragraph("Code d'approbation: " + paiement.cmiApprovalCode, normalFont));
            }
            if (paiement.description != null) {
                document.add(new Paragraph("Description: " + paiement.description, normalFont));
            }

            document.add(new Paragraph(" ")); // Espace

            // Informations CMI
            if (paiement.cmiOrderId != null) {
                document.add(new Paragraph("INFORMATIONS CMI", headerFont));
                document.add(new Paragraph("ID Commande: " + paiement.cmiOrderId, normalFont));
                document.add(
                    new Paragraph(
                        "Date de création: " +
                        (paiement.cmiCreatedAt != null
                                ? paiement.cmiCreatedAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
                                : "N/A"),
                        normalFont
                    )
                );
            }

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erreur génération PDF", e);
        }
    }

    private PaiementDTO toDto(Paiement p) {
        PaiementDTO dto = new PaiementDTO();
        dto.id = p.getId();
        dto.user = p.getUser();
        dto.amount = p.getAmount();
        dto.currency = p.getCurrency();
        dto.date = p.getDate();
        dto.status = p.getStatus();
        dto.cmiTransactionId = p.getCmiTransactionId();
        dto.cmiOrderId = p.getCmiOrderId();
        dto.cmiResponseCode = p.getCmiResponseCode();
        dto.cmiResponseMessage = p.getCmiResponseMessage();
        dto.cmiApprovalCode = p.getCmiApprovalCode();
        dto.cmiMerchantId = p.getCmiMerchantId();
        dto.cmiPaymentMethod = p.getCmiPaymentMethod();
        dto.cmiCardType = p.getCmiCardType();
        dto.cmiCardNumber = p.getCmiCardNumber();
        dto.cmiCardHolder = p.getCmiCardHolder();
        dto.cmiInstallment = p.getCmiInstallment();
        dto.cmi3DSecure = p.getCmi3DSecure();
        dto.cmiIpAddress = p.getCmiIpAddress();
        dto.cmiUserAgent = p.getCmiUserAgent();
        dto.cmiCreatedAt = p.getCmiCreatedAt();
        dto.cmiUpdatedAt = p.getCmiUpdatedAt();
        dto.description = p.getDescription();
        dto.clientIp = p.getClientIp();
        dto.createdAt = p.getCreatedAt();
        dto.updatedAt = p.getUpdatedAt();
        return dto;
    }
}
