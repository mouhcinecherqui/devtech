package devtechly.web.rest;

import devtechly.domain.Notification;
import devtechly.repository.NotificationRepository;
import devtechly.security.SecurityUtils;
import devtechly.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link devtechly.domain.Notification}.
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationResource {

    private final Logger log = LoggerFactory.getLogger(NotificationResource.class);

    private static final String ENTITY_NAME = "notification";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final NotificationRepository notificationRepository;

    public NotificationResource(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /**
     * {@code POST  /notifications} : Create a new notification.
     *
     * @param notification the notification to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new notification, or with status {@code 400 (Bad Request)} if the notification has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<Notification> createNotification(@Valid @RequestBody Notification notification) throws URISyntaxException {
        log.debug("REST request to save Notification : {}", notification);
        if (notification.getId() != null) {
            throw new BadRequestAlertException("A new notification cannot already have an ID", ENTITY_NAME, "idexists");
        }
        Notification result = notificationRepository.save(notification);
        return ResponseEntity.created(new URI("/api/notifications/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * {@code PUT  /notifications/:id} : Updates an existing notification.
     *
     * @param id the id of the notification to save.
     * @param notification the notification to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated notification,
     * or with status {@code 400 (Bad Request)} if the notification is not valid,
     * or with status {@code 500 (Internal Server Error)} if the notification couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Notification> updateNotification(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody Notification notification
    ) throws URISyntaxException {
        log.debug("REST request to update Notification : {}, {}", id, notification);
        if (notification.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, notification.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!notificationRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Notification result = notificationRepository.save(notification);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, notification.getId().toString()))
            .body(result);
    }

    /**
     * {@code PATCH  /notifications/:id} : Partial updates given fields of an existing notification, field will ignore if it is null
     *
     * @param id the id of the notification to save.
     * @param notification the notification to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated notification,
     * or with status {@code 400 (Bad Request)} if the notification is not valid,
     * or with status {@code 404 (Not Found)} if the notification is not found,
     * or with status {@code 500 (Internal Server Error)} if the notification couldn't be updated.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<Notification> partialUpdateNotification(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody Notification notification
    ) throws URISyntaxException {
        log.debug("REST request to partial update Notification partially : {}, {}", id, notification);
        if (notification.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, notification.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!notificationRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<Notification> result = notificationRepository
            .findById(notification.getId())
            .map(existingNotification -> {
                if (notification.getTitle() != null) {
                    existingNotification.setTitle(notification.getTitle());
                }
                if (notification.getMessage() != null) {
                    existingNotification.setMessage(notification.getMessage());
                }
                if (notification.getType() != null) {
                    existingNotification.setType(notification.getType());
                }
                if (notification.getTimestamp() != null) {
                    existingNotification.setTimestamp(notification.getTimestamp());
                }
                if (notification.getRead() != null) {
                    existingNotification.setRead(notification.getRead());
                }
                if (notification.getUserId() != null) {
                    existingNotification.setUserId(notification.getUserId());
                }
                if (notification.getTicketId() != null) {
                    existingNotification.setTicketId(notification.getTicketId());
                }
                if (notification.getPaymentId() != null) {
                    existingNotification.setPaymentId(notification.getPaymentId());
                }
                if (notification.getActionUrl() != null) {
                    existingNotification.setActionUrl(notification.getActionUrl());
                }

                return existingNotification;
            })
            .map(notificationRepository::save);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, notification.getId().toString())
        );
    }

    /**
     * {@code GET  /notifications} : get all the notifications.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of notifications in body.
     */
    @GetMapping("")
    public ResponseEntity<List<Notification>> getAllNotifications(@org.springdoc.core.annotations.ParameterObject Pageable pageable) {
        String login = SecurityUtils.getCurrentUserLogin().orElse(null);
        log.debug("REST request to get notifications for user: {}", login);

        if (login == null) {
            return ResponseEntity.status(401).build();
        }

        Page<Notification> page = notificationRepository.findByUserLoginOrderByTimestampDesc(login, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /notifications/:id} : get the "id" notification.
     *
     * @param id the id of the notification to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the notification, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Notification> getNotification(@PathVariable Long id) {
        String login = SecurityUtils.getCurrentUserLogin().orElse(null);
        log.debug("REST request to get Notification : {} for user {}", id, login);

        if (login == null) {
            return ResponseEntity.status(401).build();
        }

        Optional<Notification> notification = notificationRepository.findByIdAndUserLogin(id, login);
        return ResponseUtil.wrapOrNotFound(notification);
    }

    /**
     * {@code DELETE  /notifications/:id} : delete the "id" notification.
     *
     * @param id the id of the notification to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (No Content)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long id) {
        log.debug("REST request to delete Notification : {}", id);
        notificationRepository.deleteById(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    /**
     * {@code PUT  /notifications/:id/read} : Mark a notification as read.
     *
     * @param id the id of the notification to mark as read.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)}.
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markNotificationAsRead(@PathVariable Long id) {
        String login = SecurityUtils.getCurrentUserLogin().orElse(null);
        log.debug("REST request to mark Notification as read : {} for user {}", id, login);

        if (login == null) {
            return ResponseEntity.status(401).build();
        }

        Optional<Notification> notificationOpt = notificationRepository.findByIdAndUserLogin(id, login);
        notificationOpt.ifPresent(notification -> {
            notification.setRead(true);
            notificationRepository.save(notification);
        });
        return ResponseEntity.ok().build();
    }

    /**
     * {@code PUT  /notifications/read-all} : Mark all notifications as read.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)}.
     */
    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllNotificationsAsRead() {
        String login = SecurityUtils.getCurrentUserLogin().orElse(null);
        log.debug("REST request to mark all Notifications as read for user {}", login);

        if (login == null) {
            return ResponseEntity.status(401).build();
        }

        List<Notification> notifications = notificationRepository.findByUserLoginAndReadFalse(login);
        for (Notification notification : notifications) {
            notification.setRead(true);
            notificationRepository.save(notification);
        }
        return ResponseEntity.ok().build();
    }
}
