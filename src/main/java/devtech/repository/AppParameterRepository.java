package devtechly.repository;

import devtechly.domain.AppParameter;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppParameterRepository extends JpaRepository<AppParameter, Long> {
    AppParameter findByKey(String key);

    List<AppParameter> findByType(String type);

    List<AppParameter> findByTypeNotIn(List<String> types);

    List<AppParameter> findByKeyContainingIgnoreCase(String key);

    List<AppParameter> findByValueContainingIgnoreCase(String value);

    List<AppParameter> findByDescriptionContainingIgnoreCase(String description);

    boolean existsByKey(String key);

    boolean existsByKeyAndIdNot(String key, Long id);
}
