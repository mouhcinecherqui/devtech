package devtech.service;

import devtech.domain.AppParameter;
import devtech.repository.AppParameterRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
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

    public AppParameter save(AppParameter param) {
        return repository.save(param);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}
