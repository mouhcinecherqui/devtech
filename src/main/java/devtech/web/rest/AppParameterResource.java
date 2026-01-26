package devtechly.web.rest;

import devtechly.domain.AppParameter;
import devtechly.service.AppParameterService;
import devtechly.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
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
        log.debug("REST request to get all AppParameters");
        return service.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    public ResponseEntity<AppParameter> get(@PathVariable Long id) {
        log.debug("REST request to get AppParameter : {}", id);
        Optional<AppParameter> param = service.findById(id);
        return param.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    public ResponseEntity<AppParameter> create(@Valid @RequestBody AppParameter param) throws URISyntaxException {
        log.debug("REST request to save AppParameter : {}", param);
        if (param.getId() != null) {
            throw new BadRequestAlertException("Un nouveau paramètre ne peut pas avoir d'ID", "appParameter", "idexists");
        }

        try {
            AppParameter result = service.save(param);
            return ResponseEntity.created(new URI("/api/admin/app-parameters/" + result.getId())).body(result);
        } catch (IllegalArgumentException e) {
            throw new BadRequestAlertException(e.getMessage(), "appParameter", "validationerror");
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    public ResponseEntity<AppParameter> update(@PathVariable Long id, @Valid @RequestBody AppParameter param) {
        log.debug("REST request to update AppParameter : {}", param);
        if (param.getId() == null || !param.getId().equals(id)) {
            throw new BadRequestAlertException("ID invalide", "appParameter", "idinvalid");
        }

        try {
            AppParameter result = service.save(param);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            throw new BadRequestAlertException(e.getMessage(), "appParameter", "validationerror");
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.debug("REST request to delete AppParameter : {}", id);
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    // Nouvelles méthodes pour les paramètres spécifiques
    @GetMapping("/ticket-statuses")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    public List<AppParameter> getTicketStatuses() {
        log.debug("REST request to get all ticket statuses");
        return service.getTicketStatuses();
    }

    @GetMapping("/ticket-types")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    public List<AppParameter> getTicketTypes() {
        log.debug("REST request to get all ticket types");
        return service.getTicketTypes();
    }

    @GetMapping("/ticket-priorities")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    public List<AppParameter> getTicketPriorities() {
        log.debug("REST request to get all ticket priorities");
        return service.getTicketPriorities();
    }

    @GetMapping("/other")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    public List<AppParameter> getOtherParameters() {
        log.debug("REST request to get all other parameters");
        return service.getOtherParameters();
    }

    // Méthodes pour obtenir des valeurs spécifiques
    @GetMapping("/value/{key}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    public ResponseEntity<String> getValueByKey(@PathVariable String key) {
        log.debug("REST request to get value for key : {}", key);
        Optional<String> value = service.getValueByKey(key);
        return value.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/defaults/ticket-status")
    public ResponseEntity<String> getDefaultTicketStatus() {
        log.debug("REST request to get default ticket status");
        return ResponseEntity.ok(service.getDefaultTicketStatus());
    }

    @GetMapping("/defaults/ticket-type")
    public ResponseEntity<String> getDefaultTicketType() {
        log.debug("REST request to get default ticket type");
        return ResponseEntity.ok(service.getDefaultTicketType());
    }

    @GetMapping("/defaults/ticket-priority")
    public ResponseEntity<String> getDefaultTicketPriority() {
        log.debug("REST request to get default ticket priority");
        return ResponseEntity.ok(service.getDefaultTicketPriority());
    }

    @GetMapping("/config/max-tickets-per-user")
    public ResponseEntity<Integer> getMaxTicketsPerUser() {
        log.debug("REST request to get max tickets per user");
        return ResponseEntity.ok(service.getMaxTicketsPerUser());
    }

    @GetMapping("/config/ticket-auto-close-days")
    public ResponseEntity<Integer> getTicketAutoCloseDays() {
        log.debug("REST request to get ticket auto close days");
        return ResponseEntity.ok(service.getTicketAutoCloseDays());
    }

    @GetMapping("/config/notification-email-enabled")
    public ResponseEntity<Boolean> isNotificationEmailEnabled() {
        log.debug("REST request to check if notification email is enabled");
        return ResponseEntity.ok(service.isNotificationEmailEnabled());
    }

    @GetMapping("/config/support-email")
    public ResponseEntity<String> getSupportEmail() {
        log.debug("REST request to get support email");
        return ResponseEntity.ok(service.getSupportEmail());
    }

    @GetMapping("/config/company-name")
    public ResponseEntity<String> getCompanyName() {
        log.debug("REST request to get company name");
        return ResponseEntity.ok(service.getCompanyName());
    }

    // Méthodes de recherche
    @GetMapping("/search/key/{key}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    public List<AppParameter> searchByKey(@PathVariable String key) {
        log.debug("REST request to search parameters by key : {}", key);
        return service.searchByKeyContaining(key);
    }

    @GetMapping("/search/value/{value}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    public List<AppParameter> searchByValue(@PathVariable String value) {
        log.debug("REST request to search parameters by value : {}", value);
        return service.searchByValueContaining(value);
    }

    @GetMapping("/search/description/{description}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    public List<AppParameter> searchByDescription(@PathVariable String description) {
        log.debug("REST request to search parameters by description : {}", description);
        return service.searchByDescriptionContaining(description);
    }

    // Méthode pour vérifier l'existence d'une clé
    @GetMapping("/exists/{key}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    public ResponseEntity<Boolean> existsByKey(@PathVariable String key) {
        log.debug("REST request to check if parameter exists with key : {}", key);
        return ResponseEntity.ok(service.findByKey(key).isPresent());
    }
}
