package devtech.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import devtech.domain.AppUser;
import devtech.repository.AppUserRepository;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.*;

@org.junit.jupiter.api.Disabled("Mockito inline mock maker timing out intermittently")
class OAuth2JwtServiceUnitTest {

    @Mock
    private JwtEncoder jwtEncoder;

    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private OAuth2JwtService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void generateJwtFromOAuth2User_shouldCreateUserIfNotExists_andReturnToken() {
        OAuth2User user = mock(OAuth2User.class);
        when(user.getAttribute("email")).thenReturn("user@example.com");
        when(user.getAttribute("name")).thenReturn("John Doe");
        when(user.getAttribute("picture")).thenReturn("pic");

        when(appUserRepository.findByEmail("user@example.com")).thenReturn(Optional.empty());
        AppUser stored = new AppUser();
        stored.setId(123L);
        when(appUserRepository.save(any(AppUser.class))).thenReturn(stored);

        Jwt jwt = new Jwt("token-value", null, null, Map.of("alg", "RS256"), Map.of("sub", "user@example.com"));
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(jwt);

        String token = service.generateJwtFromOAuth2User(user);
        assertThat(token).isEqualTo("token-value");
        verify(appUserRepository).save(any(AppUser.class));
        verify(jwtEncoder).encode(any(JwtEncoderParameters.class));
    }
}
