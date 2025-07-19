package devtech.web.rest;

import devtech.service.PaiementService;
import devtech.service.dto.PaiementDTO;
import java.util.List;
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

    @GetMapping("/{id}/facture")
    public ResponseEntity<byte[]> getFacturePdf(@PathVariable Long id) {
        byte[] pdf = paiementService.generateFacturePdf(id);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=facture-paiement-" + id + ".pdf")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdf);
    }
}
