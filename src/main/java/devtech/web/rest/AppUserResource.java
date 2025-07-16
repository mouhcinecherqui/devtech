package devtech.web.rest;

import devtech.domain.AppUser;
import devtech.domain.Authority;
import devtech.domain.User;
import devtech.repository.AppUserRepository;
import devtech.repository.AuthorityRepository;
import devtech.repository.UserRepository;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/app-users")
public class AppUserResource {

    private static final Logger log = LoggerFactory.getLogger(AppUserResource.class);
    private final AppUserRepository appUserRepository;
    private final UserRepository userRepository;
    private final AuthorityRepository authorityRepository;
    private final PasswordEncoder passwordEncoder;

    public AppUserResource(
        AppUserRepository appUserRepository,
        UserRepository userRepository,
        AuthorityRepository authorityRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.appUserRepository = appUserRepository;
        this.userRepository = userRepository;
        this.authorityRepository = authorityRepository;
        this.passwordEncoder = passwordEncoder;
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
        return ResponseEntity.created(URI.create("/api/app-users/" + result.getId())).body(result);
    }

    @GetMapping
    public List<AppUser> getAll() {
        return appUserRepository.findAll();
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
}
