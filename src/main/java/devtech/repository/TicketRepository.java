package devtechly.repository;

import devtechly.domain.Ticket;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findByCreatedBy(String createdBy);

    /**
     * Trouve un ticket par son ID de paiement
     */
    Optional<Ticket> findByPaiementId(Long paiementId);

    /**
     * Trouve tous les tickets nécessitant un paiement
     */
    List<Ticket> findByPaymentRequiredTrue();

    /**
     * Trouve tous les tickets par statut de paiement
     */
    List<Ticket> findByPaymentStatus(String paymentStatus);
}
