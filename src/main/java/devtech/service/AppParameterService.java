package devtechly.service;

import devtechly.domain.AppParameter;
import devtechly.repository.AppParameterRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AppParameterService {

    private final AppParameterRepository repository;

    public AppParameterService(AppParameterRepository repository) {
        this.repository = repository;
    }

    public List<AppParameter> findAll() {
        return repository.findAll();
    }

    public Optional<AppParameter> findById(Long id) {
        return repository.findById(id);
    }

    public Optional<AppParameter> findByKey(String key) {
        return Optional.ofNullable(repository.findByKey(key));
    }

    public List<AppParameter> findByType(String type) {
        return repository.findByType(type);
    }

    public AppParameter save(AppParameter param) {
        // Validation des contraintes
        validateParameter(param);

        // Vérifier si la clé existe déjà (sauf en mode édition)
        if (param.getId() == null) {
            Optional<AppParameter> existing = findByKey(param.getKey());
            if (existing.isPresent()) {
                throw new IllegalArgumentException("La clé '" + param.getKey() + "' existe déjà");
            }
        }

        return repository.save(param);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    // Méthodes utilitaires pour les paramètres spécifiques
    public List<AppParameter> getTicketStatuses() {
        return findByType("ticket-status");
    }

    public List<AppParameter> getTicketTypes() {
        return findByType("ticket-type");
    }

    public List<AppParameter> getTicketPriorities() {
        return findByType("ticket-priority");
    }

    public List<AppParameter> getOtherParameters() {
        return repository.findByTypeNotIn(List.of("ticket-status", "ticket-type", "ticket-priority"));
    }

    // Méthodes pour obtenir des valeurs spécifiques
    public Optional<String> getValueByKey(String key) {
        return findByKey(key).map(AppParameter::getValue);
    }

    public String getValueByKeyOrDefault(String key, String defaultValue) {
        return getValueByKey(key).orElse(defaultValue);
    }

    // Méthodes pour les valeurs par défaut
    public String getDefaultTicketStatus() {
        return getValueByKeyOrDefault("default_status", "open");
    }

    public String getDefaultTicketType() {
        return getValueByKeyOrDefault("default_type", "support");
    }

    public String getDefaultTicketPriority() {
        return getValueByKeyOrDefault("default_priority", "normal");
    }

    public int getMaxTicketsPerUser() {
        String value = getValueByKeyOrDefault("max_tickets_per_user", "10");
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 10;
        }
    }

    public int getTicketAutoCloseDays() {
        String value = getValueByKeyOrDefault("ticket_auto_close_days", "30");
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 30;
        }
    }

    public boolean isNotificationEmailEnabled() {
        String value = getValueByKeyOrDefault("notification_email_enabled", "true");
        return Boolean.parseBoolean(value);
    }

    public String getSupportEmail() {
        return getValueByKeyOrDefault("support_email", "contact.devtechly@gmail.com");
    }

    public String getCompanyName() {
        return getValueByKeyOrDefault("company_name", "devtechly");
    }

    // Validation des paramètres
    private void validateParameter(AppParameter param) {
        if (param.getKey() == null || param.getKey().trim().isEmpty()) {
            throw new IllegalArgumentException("La clé du paramètre ne peut pas être vide");
        }

        if (param.getKey().length() > 100) {
            throw new IllegalArgumentException("La clé du paramètre ne peut pas dépasser 100 caractères");
        }

        if (param.getValue() == null || param.getValue().trim().isEmpty()) {
            throw new IllegalArgumentException("La valeur du paramètre ne peut pas être vide");
        }

        if (param.getValue().length() > 500) {
            throw new IllegalArgumentException("La valeur du paramètre ne peut pas dépasser 500 caractères");
        }

        if (param.getDescription() != null && param.getDescription().length() > 255) {
            throw new IllegalArgumentException("La description du paramètre ne peut pas dépasser 255 caractères");
        }
    }

    // Méthodes pour la gestion en lot
    public List<AppParameter> saveAll(List<AppParameter> parameters) {
        for (AppParameter param : parameters) {
            validateParameter(param);
        }
        return repository.saveAll(parameters);
    }

    // Méthodes pour la recherche
    public List<AppParameter> searchByKeyContaining(String key) {
        return repository.findByKeyContainingIgnoreCase(key);
    }

    public List<AppParameter> searchByValueContaining(String value) {
        return repository.findByValueContainingIgnoreCase(value);
    }

    public List<AppParameter> searchByDescriptionContaining(String description) {
        return repository.findByDescriptionContainingIgnoreCase(description);
    }
}
