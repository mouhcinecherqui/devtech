package devtechly.service;

import devtechly.domain.AppUser;
import devtechly.repository.AppUserRepository;
import devtechly.security.SecurityUtils;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OAuth2JwtService {

    private final JwtEncoder jwtEncoder;
    private final AppUserRepository appUserRepository;
    private final ClientEmailService clientEmailService;

    public OAuth2JwtService(JwtEncoder jwtEncoder, AppUserRepository appUserRepository, ClientEmailService clientEmailService) {
        this.jwtEncoder = jwtEncoder;
        this.appUserRepository = appUserRepository;
        this.clientEmailService = clientEmailService;
    }

    @Transactional
    public String generateJwtFromOAuth2User(OAuth2User oauth2User) {
        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");
        String picture = oauth2User.getAttribute("picture");

        // Créer ou récupérer l'AppUser
        AppUser appUser = createOrUpdateAppUser(email, name, picture);

        // Créer des autorités selon le type d'utilisateur
        Collection<GrantedAuthority> authorities;
        if (isAdminEmail(email)) {
            authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"));
        } else {
            authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_CLIENT"));
        }

        String authoritiesString = authorities
            .stream()
            .map(GrantedAuthority::getAuthority)
            .collect(java.util.stream.Collectors.joining(" "));

        Instant now = Instant.now();
        Instant validity = now.plus(24, ChronoUnit.HOURS); // Token valide 24h

        JwtClaimsSet.Builder builder = JwtClaimsSet.builder()
            .issuedAt(now)
            .expiresAt(validity)
            .subject(email) // Utiliser l'email comme subject
            .claim(SecurityUtils.AUTHORITIES_CLAIM, authoritiesString)
            .claim("name", name)
            .claim("email", email)
            .claim("picture", picture)
            .claim("appUserId", appUser.getId())
            .claim("oauth2", true);

        JwsHeader jwsHeader = JwsHeader.with(SecurityUtils.JWT_ALGORITHM).build();
        return this.jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, builder.build())).getTokenValue();
    }

    /**
     * Crée ou met à jour un AppUser à partir des informations OAuth2
     */
    private AppUser createOrUpdateAppUser(String email, String name, String picture) {
        Optional<AppUser> existingUser = appUserRepository.findByEmail(email);

        if (existingUser.isPresent()) {
            // Mettre à jour l'utilisateur existant
            AppUser user = existingUser.orElseThrow();
            if (name != null && !name.isEmpty()) {
                String[] nameParts = name.split(" ", 2);
                user.setFirstName(nameParts[0]);
                if (nameParts.length > 1) {
                    user.setLastName(nameParts[1]);
                }
            }
            return appUserRepository.save(user);
        } else {
            // Créer un nouvel utilisateur
            AppUser newUser = new AppUser();
            newUser.setEmail(email);
            newUser.setType(isAdminEmail(email) ? "ADMIN" : "CLIENT");
            newUser.setPassword(""); // Pas de mot de passe pour OAuth2

            if (name != null && !name.isEmpty()) {
                String[] nameParts = name.split(" ", 2);
                newUser.setFirstName(nameParts[0]);
                if (nameParts.length > 1) {
                    newUser.setLastName(nameParts[1]);
                } else {
                    newUser.setLastName("");
                }
            } else {
                newUser.setFirstName("Utilisateur");
                newUser.setLastName("Google");
            }

            // Téléphone par défaut (peut être modifié plus tard)
            newUser.setPhone("0000000000");

            AppUser savedUser = appUserRepository.save(newUser);

            // Envoyer un email de bienvenue pour les nouveaux utilisateurs OAuth2
            try {
                clientEmailService.sendWelcomeEmail(savedUser);
            } catch (Exception e) {
                // Log l'erreur mais ne pas faire échouer l'authentification
                System.err.println("Failed to send welcome email to OAuth2 user " + email + ": " + e.getMessage());
            }

            return savedUser;
        }
    }

    /**
     * Vérifie si un email correspond à un administrateur
     */
    private boolean isAdminEmail(String email) {
        if (email == null) return false;

        // Liste des emails d'administration (vous pouvez la modifier selon vos besoins)
        String[] adminEmails = { "admin@devtechly.com", "fatimazahra@devtechly.com", "devtechly@gmail.com", "mouhcinecherqui@gmail.com" };

        for (String adminEmail : adminEmails) {
            if (adminEmail.equalsIgnoreCase(email)) {
                return true;
            }
        }

        return false;
    }
}
