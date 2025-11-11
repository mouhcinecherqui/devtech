package devtech.repository;

import devtech.domain.Activity;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Activity entity.
 */
@SuppressWarnings("unused")
@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long>, JpaSpecificationExecutor<Activity> {
    /**
     * Find recent activities for a specific user, ordered by timestamp descending
     */
    @Query("select activity from Activity activity where activity.userId = :userId order by activity.timestamp desc")
    Page<Activity> findRecentActivitiesByUser(@Param("userId") Long userId, Pageable pageable);

    /**
     * Find all recent activities, ordered by timestamp descending
     */
    @Query("select activity from Activity activity order by activity.timestamp desc")
    Page<Activity> findRecentActivities(Pageable pageable);

    /**
     * Find recent activities for a specific user without pagination
     */
    @Query("select activity from Activity activity where activity.userId = :userId order by activity.timestamp desc")
    List<Activity> findRecentActivitiesByUser(@Param("userId") Long userId);

    /**
     * Find all recent activities without pagination
     */
    @Query("select activity from Activity activity order by activity.timestamp desc")
    List<Activity> findRecentActivities();

    /**
     * Find activities by ticket ID
     */
    @Query("select activity from Activity activity where activity.ticketId = :ticketId order by activity.timestamp desc")
    List<Activity> findByTicketId(@Param("ticketId") Long ticketId);

    /**
     * Find activities by entity type and ID
     */
    @Query(
        "select activity from Activity activity where activity.entityType = :entityType and activity.entityId = :entityId order by activity.timestamp desc"
    )
    List<Activity> findByEntityTypeAndEntityId(@Param("entityType") String entityType, @Param("entityId") Long entityId);
}
