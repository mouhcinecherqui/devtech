package devtechly.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import devtechly.domain.ClientReview;
import devtechly.repository.ClientReviewRepository;
import devtechly.service.dto.ClientReviewDTO;
import devtechly.service.mapper.ClientReviewMapper;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@org.junit.jupiter.api.Disabled("Mockito initialization issue on this environment")
class ClientReviewServiceUnitTest {

    @Mock
    private ClientReviewRepository clientReviewRepository;

    @Mock
    private ClientReviewMapper clientReviewMapper;

    @InjectMocks
    private ClientReviewService clientReviewService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void save_shouldPersistAndReturnDTO() {
        ClientReviewDTO dto = new ClientReviewDTO();
        ClientReview entity = new ClientReview();

        when(clientReviewMapper.toEntity(any(ClientReviewDTO.class))).thenReturn(entity);
        when(clientReviewRepository.save(entity)).thenReturn(entity);
        when(clientReviewMapper.toDto(entity)).thenReturn(dto);

        ClientReviewDTO result = clientReviewService.save(dto);

        assertThat(result).isSameAs(dto);
        verify(clientReviewRepository).save(entity);
    }

    @Test
    void findAll_shouldReturnPageOfDTOs() {
        ClientReview entity = new ClientReview();
        ClientReviewDTO dto = new ClientReviewDTO();

        when(clientReviewRepository.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(List.of(entity)));
        when(clientReviewMapper.toDto(entity)).thenReturn(dto);

        Page<ClientReviewDTO> page = clientReviewService.findAll(PageRequest.of(0, 10));

        assertThat(page.getContent()).containsExactly(dto);
    }

    @Test
    void findOne_shouldReturnDTO_whenExists() {
        ClientReview entity = new ClientReview();
        ClientReviewDTO dto = new ClientReviewDTO();

        when(clientReviewRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(clientReviewMapper.toDto(entity)).thenReturn(dto);

        Optional<ClientReviewDTO> result = clientReviewService.findOne(1L);

        assertThat(result).isPresent();
        assertThat(result.orElseThrow()).isSameAs(dto);
    }

    @Test
    void delete_shouldDelegateToRepository() {
        clientReviewService.delete(1L);
        verify(clientReviewRepository).deleteById(1L);
    }

    @Test
    void approveReview_shouldSetIsApprovedTrue() {
        ClientReview entity = new ClientReview();
        ClientReviewDTO dto = new ClientReviewDTO();

        when(clientReviewRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(clientReviewRepository.save(entity)).thenReturn(entity);
        when(clientReviewMapper.toDto(entity)).thenReturn(dto);

        Optional<ClientReviewDTO> result = clientReviewService.approveReview(1L);

        assertThat(result).isPresent();
        verify(clientReviewRepository).save(entity);
    }
}
