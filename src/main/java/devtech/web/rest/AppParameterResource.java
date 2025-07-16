package devtech.web.rest;

import devtech.domain.AppParameter;
import devtech.service.AppParameterService;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/app-parameters")
public class AppParameterResource {

    private final Logger log = LoggerFactory.getLogger(AppParameterResource.class);
    private final AppParameterService service;

    public AppParameterResource(AppParameterService service) {
        this.service = service;
    }

    @GetMapping("")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    public List<AppParameter> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    public ResponseEntity<AppParameter> get(@PathVariable Long id) {
        Optional<AppParameter> param = service.findById(id);
        return param.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    public ResponseEntity<AppParameter> create(@RequestBody AppParameter param) throws URISyntaxException {
        if (param.getId() != null) {
            return ResponseEntity.badRequest().build();
        }
        AppParameter result = service.save(param);
        return ResponseEntity.created(new URI("/api/admin/app-parameters/" + result.getId())).body(result);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    public ResponseEntity<AppParameter> update(@PathVariable Long id, @RequestBody AppParameter param) {
        if (param.getId() == null || !param.getId().equals(id)) {
            return ResponseEntity.badRequest().build();
        }
        AppParameter result = service.save(param);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
