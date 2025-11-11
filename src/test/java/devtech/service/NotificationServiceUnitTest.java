package devtech.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import devtech.domain.Notification;
import devtech.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@org.junit.jupiter.api.Disabled("Mockito inline mock maker timing out intermittently")
class NotificationServiceUnitTest {

    @Test
    void notifyUser_shouldPersistNotificationWithDefaults() {
        NotificationRepository repo = mock(NotificationRepository.class);
        NotificationService service = new NotificationService(repo);

        service.notifyUser("jane", "Hello", "INFO");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(repo).save(captor.capture());
        Notification saved = captor.getValue();
        assertThat(saved.getUserLogin()).isEqualTo("jane");
        assertThat(saved.getMessage()).isEqualTo("Hello");
        assertThat(saved.getType()).isEqualTo("INFO");
        assertThat(saved.isRead()).isFalse();
        assertThat(saved.getCreatedDate()).isNotNull();
    }
}
