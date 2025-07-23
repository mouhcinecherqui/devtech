package devtech.repository;

import devtech.domain.TicketMessage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketMessageRepository extends JpaRepository<TicketMessage, Long> {
    /**
     * Trouve tous les messages d'un ticket, ordonnés par date de création
     */
    @Query("SELECT tm FROM TicketMessage tm WHERE tm.ticket.id = :ticketId ORDER BY tm.createdDate ASC")
    List<TicketMessage> findByTicketIdOrderByCreatedDateAsc(@Param("ticketId") Long ticketId);

    /**
     * Trouve tous les messages d'un ticket visibles par le client
     */
    @Query("SELECT tm FROM TicketMessage tm WHERE tm.ticket.id = :ticketId AND tm.isInternal = false ORDER BY tm.createdDate ASC")
    List<TicketMessage> findPublicMessagesByTicketIdOrderByCreatedDateAsc(@Param("ticketId") Long ticketId);

    /**
     * Compte le nombre de messages d'un ticket
     */
    @Query("SELECT COUNT(tm) FROM TicketMessage tm WHERE tm.ticket.id = :ticketId")
    long countByTicketId(@Param("ticketId") Long ticketId);

    /**
     * Trouve les derniers messages de tous les tickets d'un utilisateur
     */
    @Query(
        "SELECT tm FROM TicketMessage tm WHERE tm.ticket.createdBy = :userLogin AND tm.isInternal = false " +
        "AND tm.id = (SELECT MAX(tm2.id) FROM TicketMessage tm2 WHERE tm2.ticket.id = tm.ticket.id)"
    )
    List<TicketMessage> findLatestMessagesByUser(@Param("userLogin") String userLogin);

    /**
     * Supprime tous les messages d'un ticket
     */
    void deleteByTicketId(Long ticketId);
}
