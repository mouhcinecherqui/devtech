package devtech.service;

// PDF
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;
import devtech.domain.Paiement;
import devtech.repository.PaiementRepository;
import devtech.service.dto.PaiementDTO;
import devtech.service.exception.PaiementException;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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

    /**
     * Crée un nouveau paiement
     */
    public PaiementDTO create(PaiementDTO paiementDTO) {
        System.out.println("=== DEBUG: Service create appelé ===");
        System.out.println("DTO reçu: " + paiementDTO);
        System.out.println("Client: '" + paiementDTO.user + "'");
        System.out.println("Montant: " + paiementDTO.amount);
        System.out.println("Devise: '" + paiementDTO.currency + "'");
        System.out.println("Statut: '" + paiementDTO.status + "'");
        System.out.println(
            "Date: " +
            paiementDTO.date +
            " (type: " +
            (paiementDTO.date != null ? paiementDTO.date.getClass().getSimpleName() : "null") +
            ")"
        );

        // Validation des données
        System.out.println("Début de la validation...");
        validatePaiementData(paiementDTO);
        System.out.println("Validation réussie");

        Paiement paiement = new Paiement();

        // Copier les données du DTO vers l'entité
        paiement.setUser(paiementDTO.user);

        // Gestion du montant - accepter String ou Double
        if (paiementDTO.amount != null) {
            if (paiementDTO.amount instanceof Double) {
                paiement.setAmount((Double) paiementDTO.amount);
            } else if (paiementDTO.amount instanceof String) {
                try {
                    paiement.setAmount(Double.parseDouble((String) paiementDTO.amount));
                } catch (NumberFormatException e) {
                    throw new PaiementException("Le montant doit être un nombre valide");
                }
            } else if (paiementDTO.amount instanceof Number) {
                paiement.setAmount(((Number) paiementDTO.amount).doubleValue());
            } else {
                throw new PaiementException("Type de montant non supporté");
            }
        } else {
            paiement.setAmount(null);
        }

        paiement.setCurrency(paiementDTO.currency != null ? paiementDTO.currency : "MAD");

        // Gestion de la date - accepter string ou LocalDate
        if (paiementDTO.date != null) {
            if (paiementDTO.date instanceof LocalDate) {
                paiement.setDate((LocalDate) paiementDTO.date);
            } else if (paiementDTO.date instanceof String) {
                try {
                    paiement.setDate(LocalDate.parse((String) paiementDTO.date));
                } catch (Exception e) {
                    paiement.setDate(LocalDate.now());
                }
            }
        } else {
            paiement.setDate(LocalDate.now());
        }

        paiement.setStatus(paiementDTO.status != null ? paiementDTO.status : "PENDING");
        paiement.setDescription(paiementDTO.description);

        // Champs CMI
        paiement.setCmiOrderId(paiementDTO.cmiOrderId);
        paiement.setCmiTransactionId(paiementDTO.cmiTransactionId);
        paiement.setCmiPaymentMethod(paiementDTO.cmiPaymentMethod);
        paiement.setCmiCardType(paiementDTO.cmiCardType);
        paiement.setCmiCardHolder(paiementDTO.cmiCardHolder);
        paiement.setCmiIpAddress(paiementDTO.clientIp);

        // Timestamps - ignorer les timestamps envoyés par le frontend
        paiement.setCreatedAt(LocalDateTime.now());
        paiement.setUpdatedAt(LocalDateTime.now());
        paiement.setCmiCreatedAt(LocalDateTime.now());
        paiement.setCmiUpdatedAt(LocalDateTime.now());

        try {
            Paiement savedPaiement = paiementRepository.save(paiement);
            return toDto(savedPaiement);
        } catch (Exception e) {
            throw new PaiementException("Erreur lors de la création du paiement", e);
        }
    }

    /**
     * Met à jour un paiement existant
     */
    public PaiementDTO update(Long id, PaiementDTO paiementDTO) {
        // Validation des données
        validatePaiementData(paiementDTO);

        Paiement paiement = paiementRepository
            .findById(id)
            .orElseThrow(() -> new PaiementException("Paiement non trouvé avec l'ID: " + id));

        // Mettre à jour les champs
        paiement.setUser(paiementDTO.user);

        // Gestion du montant - accepter String ou Double
        if (paiementDTO.amount != null) {
            if (paiementDTO.amount instanceof Double) {
                paiement.setAmount((Double) paiementDTO.amount);
            } else if (paiementDTO.amount instanceof String) {
                try {
                    paiement.setAmount(Double.parseDouble((String) paiementDTO.amount));
                } catch (NumberFormatException e) {
                    throw new PaiementException("Le montant doit être un nombre valide");
                }
            } else if (paiementDTO.amount instanceof Number) {
                paiement.setAmount(((Number) paiementDTO.amount).doubleValue());
            } else {
                throw new PaiementException("Type de montant non supporté");
            }
        } else {
            paiement.setAmount(null);
        }

        paiement.setCurrency(paiementDTO.currency != null ? paiementDTO.currency : "MAD");

        // Gestion de la date - accepter string ou LocalDate
        if (paiementDTO.date != null) {
            if (paiementDTO.date instanceof LocalDate) {
                paiement.setDate((LocalDate) paiementDTO.date);
            } else if (paiementDTO.date instanceof String) {
                try {
                    paiement.setDate(LocalDate.parse((String) paiementDTO.date));
                } catch (Exception e) {
                    paiement.setDate(LocalDate.now());
                }
            }
        } else {
            paiement.setDate(LocalDate.now());
        }

        paiement.setStatus(paiementDTO.status);
        paiement.setDescription(paiementDTO.description);

        // Champs CMI
        paiement.setCmiOrderId(paiementDTO.cmiOrderId);
        paiement.setCmiTransactionId(paiementDTO.cmiTransactionId);
        paiement.setCmiPaymentMethod(paiementDTO.cmiPaymentMethod);
        paiement.setCmiCardType(paiementDTO.cmiCardType);
        paiement.setCmiCardHolder(paiementDTO.cmiCardHolder);

        // Mettre à jour le timestamp
        paiement.setUpdatedAt(LocalDateTime.now());
        paiement.setCmiUpdatedAt(LocalDateTime.now());

        try {
            Paiement updatedPaiement = paiementRepository.save(paiement);
            return toDto(updatedPaiement);
        } catch (Exception e) {
            throw new PaiementException("Erreur lors de la mise à jour du paiement", e);
        }
    }

    /**
     * Supprime un paiement
     */
    public void delete(Long id) {
        if (!paiementRepository.existsById(id)) {
            throw new PaiementException("Paiement non trouvé avec l'ID: " + id);
        }
        try {
            paiementRepository.deleteById(id);
        } catch (Exception e) {
            throw new PaiementException("Erreur lors de la suppression du paiement", e);
        }
    }

    public PaiementDTO findByOrderId(String orderId) {
        return paiementRepository.findByCmiOrderId(orderId).map(this::toDto).orElseThrow();
    }

    /**
     * Crée une demande de paiement CMI
     */
    public Map<String, String> createCmiPaymentRequest(PaiementDTO paiementDTO) {
        return cmiPaymentService.createPaymentRequest(paiementDTO, paiementDTO.clientIp);
    }

    /**
     * Obtient l'URL de la passerelle CMI
     */
    public String getCmiGatewayUrl() {
        return cmiPaymentService.getGatewayUrl();
    }

    /**
     * Traite la réponse CMI
     */
    public PaiementDTO processCmiPaymentResponse(Map<String, String> params) {
        return cmiPaymentService.processPaymentResponse(params);
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
        double todayAmount = todayPayments
            .stream()
            .mapToDouble(p -> {
                if (p.amount != null) {
                    if (p.amount instanceof Double) {
                        return (Double) p.amount;
                    } else if (p.amount instanceof String) {
                        try {
                            return Double.parseDouble((String) p.amount);
                        } catch (NumberFormatException e) {
                            return 0.0;
                        }
                    } else if (p.amount instanceof Number) {
                        return ((Number) p.amount).doubleValue();
                    }
                }
                return 0.0;
            })
            .sum();
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
            // Gestion de la date pour le PDF
            String dateStr = "N/A";
            if (paiement.date != null) {
                if (paiement.date instanceof LocalDate) {
                    dateStr = ((LocalDate) paiement.date).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                } else if (paiement.date instanceof String) {
                    try {
                        LocalDate localDate = LocalDate.parse((String) paiement.date);
                        dateStr = localDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                    } catch (Exception e) {
                        dateStr = "Date invalide";
                    }
                }
            }
            document.add(new Paragraph("Date: " + dateStr, normalFont));
            document.add(new Paragraph(" ")); // Espace

            // Informations du client
            document.add(new Paragraph("INFORMATIONS CLIENT", headerFont));
            document.add(new Paragraph("Utilisateur: " + paiement.user, normalFont));
            document.add(new Paragraph(" ")); // Espace

            // Détails du paiement
            document.add(new Paragraph("DÉTAILS DU PAIEMENT", headerFont));
            // Conversion du montant pour l'affichage
            String amountStr = "N/A";
            if (paiement.amount != null) {
                if (paiement.amount instanceof Double) {
                    amountStr = String.format("%.2f", (Double) paiement.amount);
                } else if (paiement.amount instanceof String) {
                    try {
                        double amountValue = Double.parseDouble((String) paiement.amount);
                        amountStr = String.format("%.2f", amountValue);
                    } catch (NumberFormatException e) {
                        amountStr = "Montant invalide";
                    }
                } else if (paiement.amount instanceof Number) {
                    amountStr = String.format("%.2f", ((Number) paiement.amount).doubleValue());
                }
            }
            document.add(new Paragraph("Montant: " + amountStr + " " + paiement.currency, normalFont));
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

    /**
     * Valide les données d'un paiement
     */
    private void validatePaiementData(PaiementDTO paiementDTO) {
        System.out.println("=== DEBUG: Validation des données ===");
        System.out.println(
            "Client: '" + paiementDTO.user + "' (longueur: " + (paiementDTO.user != null ? paiementDTO.user.length() : "null") + ")"
        );
        System.out.println(
            "Montant: " +
            paiementDTO.amount +
            " (type: " +
            (paiementDTO.amount != null ? paiementDTO.amount.getClass().getSimpleName() : "null") +
            ")"
        );
        System.out.println("Devise: '" + paiementDTO.currency + "'");
        System.out.println("Statut: '" + paiementDTO.status + "'");

        // Validation du client
        System.out.println("Validation du client...");
        if (!StringUtils.hasText(paiementDTO.user)) {
            System.out.println("ERREUR: Nom du client manquant");
            throw new PaiementException("Le nom du client est obligatoire");
        }
        if (paiementDTO.user.length() < 2) {
            System.out.println("ERREUR: Nom du client trop court (" + paiementDTO.user.length() + " caractères)");
            throw new PaiementException("Le nom du client doit contenir au moins 2 caractères");
        }
        System.out.println("Validation du client OK");

        // Validation du montant
        System.out.println("Validation du montant...");
        if (paiementDTO.amount == null) {
            System.out.println("ERREUR: Montant manquant");
            throw new PaiementException("Le montant est obligatoire");
        }

        // Conversion et validation du montant
        double amountValue = 0.0;
        if (paiementDTO.amount instanceof Double) {
            amountValue = (Double) paiementDTO.amount;
        } else if (paiementDTO.amount instanceof String) {
            try {
                amountValue = Double.parseDouble((String) paiementDTO.amount);
            } catch (NumberFormatException e) {
                System.out.println("ERREUR: Montant non numérique (" + paiementDTO.amount + ")");
                throw new PaiementException("Le montant doit être un nombre valide");
            }
        } else if (paiementDTO.amount instanceof Number) {
            amountValue = ((Number) paiementDTO.amount).doubleValue();
        } else {
            System.out.println("ERREUR: Type de montant non supporté (" + paiementDTO.amount.getClass().getSimpleName() + ")");
            throw new PaiementException("Type de montant non supporté");
        }

        if (amountValue <= 0) {
            System.out.println("ERREUR: Montant non positif (" + amountValue + ")");
            throw new PaiementException("Le montant doit être positif");
        }
        System.out.println("Validation du montant OK");

        // Validation de la devise
        if (StringUtils.hasText(paiementDTO.currency)) {
            String[] validCurrencies = { "MAD", "EUR", "USD", "GBP" };
            boolean isValidCurrency = false;
            for (String currency : validCurrencies) {
                if (currency.equals(paiementDTO.currency)) {
                    isValidCurrency = true;
                    break;
                }
            }
            if (!isValidCurrency) {
                throw new PaiementException("Devise non supportée. Utilisez MAD, EUR, USD ou GBP");
            }
        }

        // Validation du statut
        if (StringUtils.hasText(paiementDTO.status)) {
            String[] validStatuses = { "PENDING", "COMPLETED", "FAILED", "CANCELLED" };
            boolean isValidStatus = false;
            for (String status : validStatuses) {
                if (status.equals(paiementDTO.status)) {
                    isValidStatus = true;
                    break;
                }
            }
            if (!isValidStatus) {
                throw new PaiementException("Statut non valide. Utilisez PENDING, COMPLETED, FAILED ou CANCELLED");
            }
        }
    }
}
