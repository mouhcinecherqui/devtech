package devtech.web.rest;

import static devtech.security.SecurityUtils.JWT_ALGORITHM;

import devtech.domain.User;
import devtech.security.AuthoritiesConstants;
import devtech.service.UserService;
import devtech.service.dto.AdminUserDTO;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/oauth2")
public class OAuth2Controller {

    private final UserService userService;
    private final JwtEncoder jwtEncoder;

    @Value("${jhipster.security.authentication.jwt.token-validity-in-seconds:0}")
    private long tokenValidityInSeconds;

    public OAuth2Controller(UserService userService, JwtEncoder jwtEncoder) {
        this.userService = userService;
        this.jwtEncoder = jwtEncoder;
    }

    @GetMapping("/success")
    public void oauth2Success(@AuthenticationPrincipal OAuth2User principal, HttpServletResponse response) throws IOException {
        // Extract user info from OAuth2User
        final String email = principal.getAttribute("email");
        final String rawFirstName = principal.getAttribute("given_name");
        final String rawLastName = principal.getAttribute("family_name");
        final String firstName = (rawFirstName == null && principal.getAttribute("localizedFirstName") != null)
            ? principal.getAttribute("localizedFirstName")
            : rawFirstName;
        final String lastName = (rawLastName == null && principal.getAttribute("localizedLastName") != null)
            ? principal.getAttribute("localizedLastName")
            : rawLastName;
        final String imageUrlRaw = principal.getAttribute("picture");
        final String imageUrl = imageUrlRaw == null ? principal.getAttribute("profilePicture") : imageUrlRaw;
        final String login = email != null ? email : UUID.randomUUID().toString();

        // Try to find user by email
        Optional<User> userOpt = userService.getUserWithAuthoritiesByLogin(login);
        User user = userOpt.orElseGet(() -> {
            // Register new user (activated, random password, USER authority)
            AdminUserDTO userDTO = new AdminUserDTO();
            userDTO.setLogin(login);
            userDTO.setEmail(email);
            userDTO.setFirstName(firstName);
            userDTO.setLastName(lastName);
            userDTO.setImageUrl(imageUrl);
            userDTO.setLangKey("fr");
            userDTO.setAuthorities(Set.of(AuthoritiesConstants.USER));
            userDTO.setActivated(true);
            return userService.createUser(userDTO);
        });

        // Issue JWT
        List<GrantedAuthority> authorities = user
            .getAuthorities()
            .stream()
            .map(a -> new SimpleGrantedAuthority(a.getName()))
            .collect(Collectors.toList());
        String jwt = createToken(user.getLogin(), authorities);

        // Redirect to frontend with JWT
        String redirectUrl = "/?jwt=" + jwt;
        response.sendRedirect(redirectUrl);
    }

    private String createToken(String username, List<GrantedAuthority> authorities) {
        String authoritiesStr = authorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.joining(" "));
        Instant now = Instant.now();
        Instant validity = now.plus(this.tokenValidityInSeconds, ChronoUnit.SECONDS);
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuedAt(now)
            .expiresAt(validity)
            .subject(username)
            .claim("auth", authoritiesStr)
            .build();
        JwsHeader jwsHeader = JwsHeader.with(JWT_ALGORITHM).build();
        return this.jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
    }
}
