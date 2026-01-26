package devtechly.service;

import devtechly.domain.Activity;
import devtechly.repository.ActivityRepository;
import devtechly.service.dto.ActivityDTO;
import devtechly.service.mapper.ActivityMapper;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link Activity}.
 */
@Service
@Transactional
public class ActivityService {

    private final Logger log = LoggerFactory.getLogger(ActivityService.class);

    private final ActivityRepository activityRepository;

    private final ActivityMapper activityMapper;

    public ActivityService(ActivityRepository activityRepository, ActivityMapper activityMapper) {
        this.activityRepository = activityRepository;
        this.activityMapper = activityMapper;
    }

    /**
     * Save a activity.
     *
     * @param activityDTO the entity to save.
     * @return the persisted entity.
     */
    public ActivityDTO save(ActivityDTO activityDTO) {
        log.debug("Request to save Activity : {}", activityDTO);
        Activity activity = activityMapper.toEntity(activityDTO);
        activity = activityRepository.save(activity);
        return activityMapper.toDto(activity);
    }

    /**
     * Update a activity.
     *
     * @param activityDTO the entity to save.
     * @return the persisted entity.
     */
    public ActivityDTO update(ActivityDTO activityDTO) {
        log.debug("Request to update Activity : {}", activityDTO);
        Activity activity = activityMapper.toEntity(activityDTO);
        activity = activityRepository.save(activity);
        return activityMapper.toDto(activity);
    }

    /**
     * Partially update a activity.
     *
     * @param activityDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<ActivityDTO> partialUpdate(ActivityDTO activityDTO) {
        log.debug("Request to partially update Activity : {}", activityDTO);

        return activityRepository
            .findById(activityDTO.getId())
            .map(existingActivity -> {
                // Update only non-null fields
                if (activityDTO.getActivityType() != null) {
                    existingActivity.setActivityType(activityDTO.getActivityType());
                }
                if (activityDTO.getTitle() != null) {
                    existingActivity.setTitle(activityDTO.getTitle());
                }
                if (activityDTO.getDescription() != null) {
                    existingActivity.setDescription(activityDTO.getDescription());
                }
                if (activityDTO.getTimestamp() != null) {
                    existingActivity.setTimestamp(activityDTO.getTimestamp());
                }
                if (activityDTO.getIcon() != null) {
                    existingActivity.setIcon(activityDTO.getIcon());
                }
                if (activityDTO.getUserId() != null) {
                    existingActivity.setUserId(activityDTO.getUserId());
                }
                if (activityDTO.getTicketId() != null) {
                    existingActivity.setTicketId(activityDTO.getTicketId());
                }
                if (activityDTO.getEntityType() != null) {
                    existingActivity.setEntityType(activityDTO.getEntityType());
                }
                if (activityDTO.getEntityId() != null) {
                    existingActivity.setEntityId(activityDTO.getEntityId());
                }

                return existingActivity;
            })
            .map(activityRepository::save)
            .map(activityMapper::toDto);
    }

    /**
     * Get all the activities.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<ActivityDTO> findAll(Pageable pageable) {
        log.debug("Request to get all Activities");
        return activityRepository.findAll(pageable).map(activityMapper::toDto);
    }

    /**
     * Get one activity by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<ActivityDTO> findOne(Long id) {
        log.debug("Request to get Activity : {}", id);
        return activityRepository.findById(id).map(activityMapper::toDto);
    }

    /**
     * Delete the activity by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        log.debug("Request to delete Activity : {}", id);
        activityRepository.deleteById(id);
    }

    /**
     * Get recent activities for a specific user.
     *
     * @param userId the user ID.
     * @param pageable the pagination information.
     * @return the list of recent activities.
     */
    @Transactional(readOnly = true)
    public Page<ActivityDTO> findRecentActivitiesByUser(Long userId, Pageable pageable) {
        log.debug("Request to get recent Activities for user : {}", userId);
        return activityRepository.findRecentActivitiesByUser(userId, pageable).map(activityMapper::toDto);
    }

    /**
     * Get all recent activities.
     *
     * @param pageable the pagination information.
     * @return the list of recent activities.
     */
    @Transactional(readOnly = true)
    public Page<ActivityDTO> findRecentActivities(Pageable pageable) {
        log.debug("Request to get all recent Activities");
        return activityRepository.findRecentActivities(pageable).map(activityMapper::toDto);
    }

    /**
     * Get recent activities for a specific user without pagination.
     *
     * @param userId the user ID.
     * @return the list of recent activities.
     */
    @Transactional(readOnly = true)
    public List<ActivityDTO> findRecentActivitiesByUser(Long userId) {
        log.debug("Request to get recent Activities for user : {}", userId);
        return activityRepository.findRecentActivitiesByUser(userId).stream().map(activityMapper::toDto).toList();
    }

    /**
     * Get all recent activities without pagination.
     *
     * @return the list of recent activities.
     */
    @Transactional(readOnly = true)
    public List<ActivityDTO> findRecentActivities() {
        log.debug("Request to get all recent Activities");
        return activityRepository.findRecentActivities().stream().map(activityMapper::toDto).toList();
    }

    /**
     * Get activities by ticket ID.
     *
     * @param ticketId the ticket ID.
     * @return the list of activities.
     */
    @Transactional(readOnly = true)
    public List<ActivityDTO> findByTicketId(Long ticketId) {
        log.debug("Request to get Activities by ticket ID : {}", ticketId);
        return activityRepository.findByTicketId(ticketId).stream().map(activityMapper::toDto).toList();
    }

    /**
     * Get activities by entity type and ID.
     *
     * @param entityType the entity type.
     * @param entityId the entity ID.
     * @return the list of activities.
     */
    @Transactional(readOnly = true)
    public List<ActivityDTO> findByEntityTypeAndEntityId(String entityType, Long entityId) {
        log.debug("Request to get Activities by entity type : {} and entity ID : {}", entityType, entityId);
        return activityRepository.findByEntityTypeAndEntityId(entityType, entityId).stream().map(activityMapper::toDto).toList();
    }

    /**
     * Create a new activity.
     *
     * @param activityType the activity type.
     * @param title the title.
     * @param description the description.
     * @param userId the user ID.
     * @param ticketId the ticket ID (optional).
     * @param entityType the entity type (optional).
     * @param entityId the entity ID (optional).
     * @return the created activity.
     */
    public ActivityDTO createActivity(
        devtechly.domain.ActivityType activityType,
        String title,
        String description,
        Long userId,
        Long ticketId,
        String entityType,
        Long entityId
    ) {
        log.debug("Request to create Activity : {} - {}", activityType, title);

        ActivityDTO activityDTO = new ActivityDTO();
        activityDTO.setActivityType(activityType);
        activityDTO.setTitle(title);
        activityDTO.setDescription(description);
        activityDTO.setTimestamp(java.time.Instant.now());
        activityDTO.setIcon(getIconForActivityType(activityType));
        activityDTO.setUserId(userId);
        activityDTO.setTicketId(ticketId);
        activityDTO.setEntityType(entityType);
        activityDTO.setEntityId(entityId);

        return save(activityDTO);
    }

    /**
     * Get icon for activity type.
     *
     * @param activityType the activity type.
     * @return the icon name.
     */
    private String getIconForActivityType(devtechly.domain.ActivityType activityType) {
        return switch (activityType) {
            case SUCCESS -> "check";
            case INFO -> "info";
            case WARNING -> "warning";
            case ERROR -> "error";
        };
    }
}
