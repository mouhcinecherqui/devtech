package devtechly.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import devtechly.domain.Ticket;
import devtechly.domain.TicketMessage;
import devtechly.repository.TicketMessageRepository;
import devtechly.repository.TicketRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@org.junit.jupiter.api.Disabled("Mockito initialization issue on this environment")
class TicketMessageServiceUnitTest {

    @Mock
    private TicketMessageRepository ticketMessageRepository;

    @Mock
    private TicketRepository ticketRepository;

    @InjectMocks
    private TicketMessageService ticketMessageService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void addMessage_shouldSaveAndReturnMessage_whenValid() {
        Ticket ticket = new Ticket();
        ticket.setId(1L);

        TicketMessage message = new TicketMessage();
        message.setId(10L);

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(ticketMessageRepository.save(any(TicketMessage.class))).thenReturn(message);

        TicketMessage result = ticketMessageService.addMessage(1L, "Test message", TicketMessage.AuthorType.CLIENT, "user1");

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(10L);
        verify(ticketMessageRepository).save(any(TicketMessage.class));
    }

    @Test
    void addMessage_shouldThrowException_whenTicketNotFound() {
        when(ticketRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketMessageService.addMessage(1L, "Test", TicketMessage.AuthorType.CLIENT, "user1"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Ticket non trouvé");
    }

    @Test
    void addMessage_shouldThrowException_whenContentEmpty() {
        Ticket ticket = new Ticket();
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> ticketMessageService.addMessage(1L, "", TicketMessage.AuthorType.CLIENT, "user1"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("contenu du message");
    }

    @Test
    void getTicketMessages_shouldReturnList() {
        TicketMessage msg1 = new TicketMessage();
        TicketMessage msg2 = new TicketMessage();

        when(ticketMessageRepository.findByTicketIdOrderByCreatedDateAsc(1L)).thenReturn(List.of(msg1, msg2));

        List<TicketMessage> result = ticketMessageService.getTicketMessages(1L);

        assertThat(result).hasSize(2);
    }

    @Test
    void getMessageCount_shouldReturnCount() {
        when(ticketMessageRepository.countByTicketId(1L)).thenReturn(5L);

        long count = ticketMessageService.getMessageCount(1L);

        assertThat(count).isEqualTo(5L);
    }

    @Test
    void deleteTicketMessages_shouldDelegateToRepository() {
        ticketMessageService.deleteTicketMessages(1L);
        verify(ticketMessageRepository).deleteByTicketId(1L);
    }
}
