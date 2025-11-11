package devtech.repository;

import devtech.domain.ClientReview;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the ClientReview entity.
 */
@SuppressWarnings("unused")
@Repository
public interface ClientReviewRepository extends JpaRepository<ClientReview, Long>, JpaSpecificationExecutor<ClientReview> {
    @Query("select clientReview from ClientReview clientReview where clientReview.ticket.id =:ticketId")
    Optional<ClientReview> findByTicketId(@Param("ticketId") Long ticketId);

    @Query("select clientReview from ClientReview clientReview where clientReview.isApproved = true order by clientReview.createdDate desc")
    List<ClientReview> findApprovedReviews();

    @Query("select clientReview from ClientReview clientReview where clientReview.isApproved = true order by clientReview.createdDate desc")
    Page<ClientReview> findApprovedReviews(Pageable pageable);

    @Query("select avg(clientReview.rating) from ClientReview clientReview where clientReview.isApproved = true")
    Double getAverageRating();

    @Query("select count(clientReview) from ClientReview clientReview where clientReview.isApproved = true")
    Long getTotalApprovedReviews();

    @Query(
        "select clientReview from ClientReview clientReview where clientReview.isApproved = false order by clientReview.createdDate desc"
    )
    List<ClientReview> findPendingReviews();
}
