package devtechly.repository;

import devtechly.domain.PaymentMethod;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {
    List<PaymentMethod> findByUserLoginOrderByIsDefaultDescCreatedDateDesc(String userLogin);
    Optional<PaymentMethod> findByIdAndUserLogin(Long id, String userLogin);
    List<PaymentMethod> findByUserLoginAndIsDefaultTrue(String userLogin);
}
