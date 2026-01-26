package devtechly.web.rest;

import devtechly.domain.AppUser;
import devtechly.domain.Authority;
import devtechly.domain.User;
import devtechly.repository.AppUserRepository;
import devtechly.repository.AuthorityRepository;
import devtechly.repository.UserRepository;
import devtechly.service.ClientEmailService;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.PaginationUtil;

@RestController
@RequestMapping("/api/app-users")
public class AppUserResource {

    private static final Logger log = LoggerFactory.getLogger(AppUserResource.class);
    private final AppUserRepository appUserRepository;
    private final UserRepository userRepository;
    private final AuthorityRepository authorityRepository;
    private final PasswordEncoder passwordEncoder;
    private final ClientEmailService clientEmailService;

    public AppUserResource(
        AppUserRepository appUserRepository,
        UserRepository userRepository,
        AuthorityRepository authorityRepository,
        PasswordEncoder passwordEncoder,
        ClientEmailService clientEmailService
    ) {
        this.appUserRepository = appUserRepository;
        this.userRepository = userRepository;
        this.authorityRepository = authorityRepository;
        this.passwordEncoder = passwordEncoder;
        this.clientEmailService = clientEmailService;
    }

    @PostMapping
    public ResponseEntity<AppUser> register(@RequestBody AppUser appUser) {
        log.info("Création client: email={}, password={}", appUser.getEmail(), appUser.getPassword());
        if (appUserRepository.existsByEmail(appUser.getEmail())) {
            return ResponseEntity.badRequest().build();
        }
        AppUser result = appUserRepository.save(appUser);

        // Always create a User with ROLE_CLIENT
        if (!userRepository.findOneByLogin(appUser.getEmail().toLowerCase()).isPresent()) {
            User user = new User();
            user.setLogin(appUser.getEmail().toLowerCase());
            user.setFirstName(appUser.getFirstName());
            user.setLastName(appUser.getLastName());
            user.setEmail(appUser.getEmail().toLowerCase());
            user.setPassword(passwordEncoder.encode(appUser.getPassword()));
            user.setActivated(true);
            user.setLangKey("fr"); // or Constants.DEFAULT_LANGUAGE
            HashSet<Authority> authorities = new HashSet<>();
            Authority clientAuthority = authorityRepository.findById("ROLE_CLIENT").orElse(null);
            if (clientAuthority == null) {
                clientAuthority = new Authority();
                clientAuthority.setName("ROLE_CLIENT");
                authorityRepository.save(clientAuthority);
                log.info("Created missing ROLE_CLIENT authority in database.");
            }
            authorities.add(clientAuthority);
            log.info("Assigning ROLE_CLIENT to user {}", appUser.getEmail());
            user.setAuthorities(authorities);
            userRepository.save(user);
            log.info("Created User for client {} with authorities: {}", appUser.getEmail(), authorities);
        }

        // Send welcome email to the client
        try {
            clientEmailService.sendWelcomeEmail(result);
            log.info("Welcome email sent to client: {}", result.getEmail());
        } catch (Exception e) {
            log.error("Failed to send welcome email to client {}: {}", result.getEmail(), e.getMessage());
        }

        return ResponseEntity.created(URI.create("/api/app-users/" + result.getId())).body(result);
    }

    @GetMapping
    public ResponseEntity<List<AppUser>> getAll(@org.springdoc.core.annotations.ParameterObject Pageable pageable) {
        log.debug("REST request to get all AppUsers with pagination");
        final Page<AppUser> page = appUserRepository.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AppUser> update(@PathVariable Long id, @RequestBody AppUser appUser) {
        if (!appUserRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        appUser.setId(id);
        AppUser result = appUserRepository.save(appUser);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!appUserRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        appUserRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<AppUser> updateRole(@PathVariable Long id, @RequestBody RoleUpdateRequest request) {
        Optional<AppUser> userOpt = appUserRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        AppUser user = userOpt.orElseThrow();
        String newRole = request.getType();

        // Valider le rôle
        if (!isValidRole(newRole)) {
            return ResponseEntity.badRequest().build();
        }

        // Mettre à jour le rôle dans AppUser
        user.setType(newRole);
        AppUser updatedUser = appUserRepository.save(user);

        // Mettre à jour le rôle dans User (pour l'authentification)
        Optional<User> systemUserOpt = userRepository.findOneByLogin(user.getEmail().toLowerCase());
        if (systemUserOpt.isPresent()) {
            User systemUser = systemUserOpt.orElseThrow();
            HashSet<Authority> authorities = new HashSet<>();

            // Créer ou récupérer l'autorité correspondante
            String authorityName = "ROLE_" + newRole;
            Authority authority = authorityRepository.findById(authorityName).orElse(null);
            if (authority == null) {
                authority = new Authority();
                authority.setName(authorityName);
                authorityRepository.save(authority);
                log.info("Created missing {} authority in database.", authorityName);
            }
            authorities.add(authority);
            systemUser.setAuthorities(authorities);
            userRepository.save(systemUser);
            log.info("Updated role for user {} to {}", user.getEmail(), newRole);
        }

        return ResponseEntity.ok(updatedUser);
    }

    private boolean isValidRole(String role) {
        return role != null && (role.equals("ADMIN") || role.equals("MANAGER") || role.equals("CLIENT"));
    }

    @PostMapping("/login")
    public ResponseEntity<AppUser> login(@RequestBody LoginRequest loginRequest) {
        Optional<AppUser> userOpt = appUserRepository
            .findAll()
            .stream()
            .filter(u -> u.getEmail().equals(loginRequest.getEmail()) && u.getPassword().equals(loginRequest.getPassword()))
            .findFirst();
        return userOpt.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.status(401).build());
    }

    public static class LoginRequest {

        private String email;
        private String password;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class RoleUpdateRequest {

        private String type;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }
}
