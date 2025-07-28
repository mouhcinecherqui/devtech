package devtech.repository;

import devtech.domain.Paiement;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PaiementRepository extends JpaRepository<Paiement, Long> {
    /**
     * Trouve un paiement par son ID de commande CMI
     */
    Optional<Paiement> findByCmiOrderId(String cmiOrderId);

    /**
     * Trouve un paiement par son ID de transaction CMI
     */
    Optional<Paiement> findByCmiTransactionId(String cmiTransactionId);

    /**
     * Trouve tous les paiements d'un utilisateur
     */
    List<Paiement> findByUserOrderByCreatedAtDesc(String user);

    /**
     * Trouve tous les paiements par statut
     */
    List<Paiement> findByStatusOrderByCreatedAtDesc(String status);

    /**
     * Trouve tous les paiements entre deux dates
     */
    List<Paiement> findByDateBetweenOrderByDateDesc(LocalDate startDate, LocalDate endDate);

    /**
     * Trouve tous les paiements d'un utilisateur par statut
     */
    List<Paiement> findByUserAndStatusOrderByCreatedAtDesc(String user, String status);

    /**
     * Compte le nombre de paiements par statut
     */
    @Query("SELECT p.status, COUNT(p) FROM Paiement p GROUP BY p.status")
    List<Object[]> countByStatus();

    /**
     * Trouve les paiements récents (derniers 30 jours)
     */
    @Query("SELECT p FROM Paiement p WHERE p.createdAt >= :startDate ORDER BY p.createdAt DESC")
    List<Paiement> findRecentPayments(@Param("startDate") LocalDate startDate);

    /**
     * Trouve les paiements par montant minimum
     */
    @Query("SELECT p FROM Paiement p WHERE p.amount >= :minAmount ORDER BY p.amount DESC")
    List<Paiement> findByAmountGreaterThanEqual(@Param("minAmount") Double minAmount);

    /**
     * Trouve les paiements par devise
     */
    List<Paiement> findByCurrencyOrderByCreatedAtDesc(String currency);

    /**
     * Trouve les paiements échoués récents
     */
    @Query("SELECT p FROM Paiement p WHERE p.status = 'FAILED' AND p.cmiUpdatedAt >= :startDate ORDER BY p.cmiUpdatedAt DESC")
    List<Paiement> findRecentFailedPayments(@Param("startDate") LocalDate startDate);

    /**
     * Trouve les paiements en attente
     */
    @Query("SELECT p FROM Paiement p WHERE p.status = 'PENDING' ORDER BY p.createdAt ASC")
    List<Paiement> findPendingPayments();

    /**
     * Trouve les paiements complétés aujourd'hui
     */
    @Query("SELECT p FROM Paiement p WHERE p.status = 'COMPLETED' AND p.date = :today ORDER BY p.createdAt DESC")
    List<Paiement> findCompletedPaymentsToday(@Param("today") LocalDate today);
}
