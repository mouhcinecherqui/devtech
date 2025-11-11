package devtech.web.rest;

import devtech.service.OAuth2JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/api/oauth2")
public class OAuth2Controller {

    private final OAuth2JwtService oauth2JwtService;
    private final Environment environment;

    public OAuth2Controller(OAuth2JwtService oauth2JwtService, Environment environment) {
        this.oauth2JwtService = oauth2JwtService;
        this.environment = environment;
    }

    private String getFrontendUrl(HttpServletRequest request) {
        // 1. Try to get from request parameter (if passed from frontend)
        String redirectUri = request.getParameter("redirect_uri");
        if (redirectUri != null && !redirectUri.isEmpty()) {
            return redirectUri;
        }

        // 2. Try to get from session (if stored before OAuth redirect)
        Object sessionRedirectUri = request.getSession().getAttribute("oauth2_redirect_uri");
        if (sessionRedirectUri != null) {
            return sessionRedirectUri.toString();
        }

        // 3. Try to get from Referer header
        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isEmpty()) {
            try {
                java.net.URL url = new java.net.URL(referer);
                return url.getProtocol() + "://" + url.getAuthority();
            } catch (Exception e) {
                // Ignore malformed URL
            }
        }

        // 4. Try to get from Origin header
        String origin = request.getHeader("Origin");
        if (origin != null && !origin.isEmpty()) {
            return origin;
        }

        // 5. Default based on environment
        if (environment.acceptsProfiles(org.springframework.core.env.Profiles.of("dev", "development"))) {
            // Development: try common dev ports
            String host = request.getServerName();
            if ("localhost".equals(host) || "127.0.0.1".equals(host)) {
                // Check common Angular dev ports
                return "http://localhost:4200";
            }
        }

        // 6. Fallback: use request scheme and host, but with port 4200 for Angular dev server
        String scheme = request.getScheme();
        String host = request.getServerName();
        if ("localhost".equals(host) || "127.0.0.1".equals(host)) {
            return scheme + "://" + host + ":4200";
        }

        // 7. Last fallback: use same host as backend (for production)
        return (
            scheme + "://" + host + (request.getServerPort() != 80 && request.getServerPort() != 443 ? ":" + request.getServerPort() : "")
        );
    }

    @GetMapping("/success")
    public void oauth2Success(
        HttpServletRequest request,
        HttpServletResponse response,
        @RequestParam(required = false) String redirect_uri
    ) throws IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String frontendUrl = getFrontendUrl(request);

        if (authentication != null && authentication.getPrincipal() instanceof OAuth2User) {
            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();

            // Générer un JWT pour l'utilisateur OAuth2
            String jwt = oauth2JwtService.generateJwtFromOAuth2User(oauth2User);

            // Get user attributes safely
            String name = oauth2User.getAttribute("name");
            String email = oauth2User.getAttribute("email");

            // Rediriger vers la page du front avec le JWT
            String redirectUrl =
                frontendUrl +
                "/login?oauth2=success&token=" +
                java.net.URLEncoder.encode(jwt, "UTF-8") +
                (name != null ? "&name=" + java.net.URLEncoder.encode(name, "UTF-8") : "") +
                (email != null ? "&email=" + java.net.URLEncoder.encode(email, "UTF-8") : "");

            response.sendRedirect(redirectUrl);
        } else {
            // Rediriger vers la page de login en cas d'erreur
            response.sendRedirect(frontendUrl + "/login?error=oauth2_failed");
        }
    }

    @GetMapping("/api/success")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> oauth2SuccessApi() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        Map<String, Object> response = new HashMap<>();

        if (authentication != null && authentication.getPrincipal() instanceof OAuth2User) {
            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();

            response.put("success", true);
            response.put("message", "Authentification Google réussie");
            response.put(
                "user",
                Map.of(
                    "name",
                    oauth2User.getAttribute("name"),
                    "email",
                    oauth2User.getAttribute("email"),
                    "picture",
                    oauth2User.getAttribute("picture")
                )
            );
        } else {
            response.put("success", false);
            response.put("message", "Authentification échouée");
        }

        return ResponseEntity.ok(response);
    }
}
