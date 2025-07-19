package devtech.service;

// PDF
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;
import devtech.domain.Paiement;
import devtech.repository.PaiementRepository;
import devtech.service.dto.PaiementDTO;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PaiementService {

    private final PaiementRepository paiementRepository;

    public PaiementService(PaiementRepository paiementRepository) {
        this.paiementRepository = paiementRepository;
    }

    public List<PaiementDTO> findAll() {
        return paiementRepository.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    public PaiementDTO findById(Long id) {
        return paiementRepository.findById(id).map(this::toDto).orElseThrow();
    }

    public byte[] generateFacturePdf(Long paiementId) {
        PaiementDTO paiement = findById(paiementId);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();
            document.add(new Paragraph("Facture Paiement #" + paiement.id));
            document.add(new Paragraph("Utilisateur : " + paiement.user));
            document.add(new Paragraph("Montant : " + paiement.amount + " EUR"));
            document.add(new Paragraph("Date : " + paiement.date.format(DateTimeFormatter.ISO_DATE)));
            document.add(new Paragraph("Statut : " + paiement.status));
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
        dto.date = p.getDate();
        dto.status = p.getStatus();
        return dto;
    }
}
