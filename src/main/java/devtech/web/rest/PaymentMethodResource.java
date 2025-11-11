package devtech.web.rest;

import devtech.domain.PaymentMethod;
import devtech.repository.PaymentMethodRepository;
import devtech.security.SecurityUtils;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payment-methods")
public class PaymentMethodResource {

    private final PaymentMethodRepository repository;

    public PaymentMethodResource(PaymentMethodRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<PaymentMethod>> list() {
        String login = SecurityUtils.getCurrentUserLogin().orElse(null);
        if (login == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(repository.findByUserLoginOrderByIsDefaultDescCreatedDateDesc(login));
    }

    @PostMapping
    @Transactional
    public ResponseEntity<PaymentMethod> create(@RequestBody PaymentMethod method) {
        String login = SecurityUtils.getCurrentUserLogin().orElse(null);
        if (login == null) return ResponseEntity.status(401).build();

        method.setId(null);
        method.setUserLogin(login);

        // Ensure single default
        if (Boolean.TRUE.equals(method.getIsDefault())) {
            repository.findByUserLoginAndIsDefaultTrue(login).forEach(m -> m.setIsDefault(false));
        } else if (repository.findByUserLoginOrderByIsDefaultDescCreatedDateDesc(login).isEmpty()) {
            method.setIsDefault(true);
        }

        PaymentMethod saved = repository.save(method);
        return ResponseEntity.created(URI.create("/api/payment-methods/" + saved.getId())).body(saved);
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<PaymentMethod> update(@PathVariable Long id, @RequestBody PaymentMethod method) {
        String login = SecurityUtils.getCurrentUserLogin().orElse(null);
        if (login == null) return ResponseEntity.status(401).build();

        Optional<PaymentMethod> existingOpt = repository.findByIdAndUserLogin(id, login);
        if (existingOpt.isEmpty()) return ResponseEntity.notFound().build();

        PaymentMethod existing = existingOpt.get();
        existing.setHolderName(method.getHolderName());
        existing.setBrand(method.getBrand());
        existing.setExpMonth(method.getExpMonth());
        existing.setExpYear(method.getExpYear());
        existing.setType(method.getType());

        if (Boolean.TRUE.equals(method.getIsDefault())) {
            repository.findByUserLoginAndIsDefaultTrue(login).forEach(m -> m.setIsDefault(false));
            existing.setIsDefault(true);
        }

        return ResponseEntity.ok(existing);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        String login = SecurityUtils.getCurrentUserLogin().orElse(null);
        if (login == null) return ResponseEntity.status(401).build();

        Optional<PaymentMethod> existingOpt = repository.findByIdAndUserLogin(id, login);
        if (existingOpt.isEmpty()) return ResponseEntity.notFound().build();

        boolean wasDefault = Boolean.TRUE.equals(existingOpt.get().getIsDefault());
        repository.delete(existingOpt.get());

        // If deleted default, set another as default if exists
        if (wasDefault) {
            List<PaymentMethod> left = repository.findByUserLoginOrderByIsDefaultDescCreatedDateDesc(login);
            if (!left.isEmpty()) {
                left.get(0).setIsDefault(true);
            }
        }

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/set-default/{id}")
    @Transactional
    public ResponseEntity<Void> setDefault(@PathVariable Long id) {
        String login = SecurityUtils.getCurrentUserLogin().orElse(null);
        if (login == null) return ResponseEntity.status(401).build();

        Optional<PaymentMethod> methodOpt = repository.findByIdAndUserLogin(id, login);
        if (methodOpt.isEmpty()) return ResponseEntity.notFound().build();

        repository.findByUserLoginAndIsDefaultTrue(login).forEach(m -> m.setIsDefault(false));
        PaymentMethod m = methodOpt.get();
        m.setIsDefault(true);
        return ResponseEntity.noContent().build();
    }
}
