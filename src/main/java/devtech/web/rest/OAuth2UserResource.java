package devtech.web.rest;

import devtech.domain.AppUser;
import devtech.repository.AppUserRepository;
import devtech.security.SecurityUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/oauth2")
public class OAuth2UserResource {

    private final AppUserRepository appUserRepository;

    public OAuth2UserResource(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    /**
     * Récupère les informations de l'utilisateur connecté via OAuth2
     */
    @GetMapping("/user")
    public ResponseEntity<Map<String, Object>> getOAuth2User() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        Map<String, Object> response = new HashMap<>();

        if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
            Jwt jwt = (Jwt) authentication.getPrincipal();

            // Récupérer les informations du JWT
            String email = jwt.getClaimAsString("email");
            String name = jwt.getClaimAsString("name");
            String picture = jwt.getClaimAsString("picture");
            Long appUserId = null;
            Object appUserIdClaim = jwt.getClaim("appUserId");
            if (appUserIdClaim instanceof Number) {
                appUserId = ((Number) appUserIdClaim).longValue();
            } else if (appUserIdClaim != null) {
                try {
                    appUserId = Long.parseLong(appUserIdClaim.toString());
                } catch (NumberFormatException ignored) {
                    appUserId = null;
                }
            }

            Boolean isOAuth2 = false;
            Object oauth2Claim = jwt.getClaim("oauth2");
            if (oauth2Claim instanceof Boolean) {
                isOAuth2 = (Boolean) oauth2Claim;
            } else if (oauth2Claim != null) {
                isOAuth2 = Boolean.parseBoolean(oauth2Claim.toString());
            }

            response.put("success", true);
            response.put("email", email);
            response.put("name", name);
            response.put("picture", picture);
            response.put("appUserId", appUserId);
            response.put("oauth2", isOAuth2);

            // Récupérer les informations complètes de l'AppUser
            if (appUserId != null) {
                Optional<AppUser> appUser = appUserRepository.findById(appUserId);
                if (appUser.isPresent()) {
                    AppUser user = appUser.get();
                    Map<String, Object> userInfo = new HashMap<>();
                    userInfo.put("id", user.getId());
                    userInfo.put("firstName", user.getFirstName());
                    userInfo.put("lastName", user.getLastName());
                    userInfo.put("email", user.getEmail());
                    userInfo.put("phone", user.getPhone());
                    userInfo.put("type", user.getType());
                    userInfo.put("createdDate", user.getCreatedDate());

                    response.put("appUser", userInfo);
                }
            }
        } else {
            response.put("success", false);
            response.put("message", "Utilisateur non authentifié via OAuth2");
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Met à jour les informations de l'utilisateur OAuth2
     */
    @GetMapping("/user/update")
    public ResponseEntity<Map<String, Object>> updateOAuth2User() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        Map<String, Object> response = new HashMap<>();

        if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
            Jwt jwt = (Jwt) authentication.getPrincipal();
            Long appUserId = null;
            Object appUserIdClaim = jwt.getClaim("appUserId");
            if (appUserIdClaim instanceof Number) {
                appUserId = ((Number) appUserIdClaim).longValue();
            } else if (appUserIdClaim != null) {
                try {
                    appUserId = Long.parseLong(appUserIdClaim.toString());
                } catch (NumberFormatException ignored) {
                    appUserId = null;
                }
            }

            if (appUserId != null) {
                Optional<AppUser> appUserOpt = appUserRepository.findById(appUserId);
                if (appUserOpt.isPresent()) {
                    AppUser appUser = appUserOpt.get();

                    // Mettre à jour les informations depuis le JWT
                    String name = jwt.getClaimAsString("name");
                    if (name != null && !name.isEmpty()) {
                        String[] nameParts = name.split(" ", 2);
                        appUser.setFirstName(nameParts[0]);
                        if (nameParts.length > 1) {
                            appUser.setLastName(nameParts[1]);
                        }
                    }

                    AppUser savedUser = appUserRepository.save(appUser);

                    response.put("success", true);
                    response.put("message", "Utilisateur mis à jour avec succès");
                    response.put(
                        "user",
                        Map.of(
                            "id",
                            savedUser.getId(),
                            "firstName",
                            savedUser.getFirstName(),
                            "lastName",
                            savedUser.getLastName(),
                            "email",
                            savedUser.getEmail(),
                            "type",
                            savedUser.getType()
                        )
                    );
                } else {
                    response.put("success", false);
                    response.put("message", "Utilisateur non trouvé");
                }
            } else {
                response.put("success", false);
                response.put("message", "ID utilisateur non trouvé dans le token");
            }
        } else {
            response.put("success", false);
            response.put("message", "Utilisateur non authentifié via OAuth2");
        }

        return ResponseEntity.ok(response);
    }
}
