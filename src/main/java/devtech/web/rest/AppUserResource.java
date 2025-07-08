package devtech.web.rest;

import devtech.domain.AppUser;
import devtech.repository.AppUserRepository;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/app-users")
public class AppUserResource {

    private final AppUserRepository appUserRepository;

    public AppUserResource(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @PostMapping
    public ResponseEntity<AppUser> register(@RequestBody AppUser appUser) {
        if (appUserRepository.existsByEmail(appUser.getEmail())) {
            return ResponseEntity.badRequest().build();
        }
        AppUser result = appUserRepository.save(appUser);
        return ResponseEntity.created(URI.create("/api/app-users/" + result.getId())).body(result);
    }

    @GetMapping
    public List<AppUser> getAll() {
        return appUserRepository.findAll();
    }
}
