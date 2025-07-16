package devtech.repository;

import devtech.domain.AppParameter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppParameterRepository extends JpaRepository<AppParameter, Long> {
    AppParameter findByKey(String key);
}
