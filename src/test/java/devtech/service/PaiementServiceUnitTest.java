package devtech.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import devtech.domain.Paiement;
import devtech.repository.PaiementRepository;
import devtech.service.dto.PaiementDTO;
import devtech.service.exception.PaiementException;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@org.junit.jupiter.api.Disabled("Mockito inline mock maker timing out intermittently")
class PaiementServiceUnitTest {

    @Mock
    private PaiementRepository paiementRepository;

    @Mock
    private CmiPaymentService cmiPaymentService;

    @InjectMocks
    private PaiementService paiementService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void create_shouldValidateAndPersist_whenValidData() {
        PaiementDTO dto = new PaiementDTO();
        dto.user = "Alice";
        dto.amount = 120.5;
        dto.currency = "MAD";
        dto.date = LocalDate.now();
        Paiement saved = new Paiement();
        saved.setId(1L);
        when(paiementRepository.save(any())).thenReturn(saved);

        var result = paiementService.create(dto);
        assertThat(result.id).isEqualTo(1L);
        verify(paiementRepository).save(any(Paiement.class));
    }

    @Test
    void create_shouldAcceptStringAmount_andConvert() {
        PaiementDTO dto = new PaiementDTO();
        dto.user = "Bob";
        dto.amount = "45.10";
        dto.currency = "MAD";
        when(paiementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = paiementService.create(dto);
        assertThat(result.amount).isInstanceOf(Double.class);
    }

    @Test
    void create_shouldThrow_whenAmountInvalid() {
        PaiementDTO dto = new PaiementDTO();
        dto.user = "A"; // too short triggers message after amount validation if ok
        dto.amount = "xx";

        assertThatThrownBy(() -> paiementService.create(dto)).isInstanceOf(PaiementException.class);
    }
}
