package devtechly.service;

import devtechly.domain.AppUser;
import devtechly.domain.Authority;
import devtechly.domain.User;
import devtechly.repository.AppUserRepository;
import devtechly.repository.AuthorityRepository;
import devtechly.repository.UserRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClientToUserMigrationService {

    private static final Logger log = LoggerFactory.getLogger(ClientToUserMigrationService.class);

    private final AppUserRepository appUserRepository;
    private final UserRepository userRepository;
    private final AuthorityRepository authorityRepository;
    private final PasswordEncoder passwordEncoder;

    public ClientToUserMigrationService(
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

    @Transactional
    public void migrateAllClientsToUsers() {
        List<AppUser> clients = appUserRepository.findAll();
        Authority clientAuthority = authorityRepository
            .findById("ROLE_CLIENT")
            .orElseThrow(() -> new RuntimeException("ROLE_CLIENT not found"));
        for (AppUser client : clients) {
            // Use email as login
            String login = client.getEmail().toLowerCase();
            Optional<User> existing = userRepository.findOneByLogin(login);
            if (existing.isPresent()) {
                log.info("User with login {} already exists, skipping migration for this client.", login);
                continue;
            }
            User user = new User();
            user.setLogin(login);
            user.setFirstName(client.getFirstName());
            user.setLastName(client.getLastName());
            user.setEmail(client.getEmail().toLowerCase());
            user.setPassword(passwordEncoder.encode(client.getPassword()));
            user.setActivated(true);
            user.setLangKey("fr"); // or default language
            HashSet<Authority> authorities = new HashSet<>();
            authorities.add(clientAuthority);
            user.setAuthorities(authorities);
            userRepository.save(user);
            log.info("Migrated client {} to user {}", client.getEmail(), login);
        }
    }
}
