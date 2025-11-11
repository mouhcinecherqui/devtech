package devtech.web.rest;

import devtech.service.ClientReviewService;
import devtech.service.dto.ClientReviewDTO;
import devtech.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
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
 * REST controller for managing {@link devtech.domain.ClientReview}.
 */
@RestController
@RequestMapping("/api/client-reviews")
public class ClientReviewResource {

    private final Logger log = LoggerFactory.getLogger(ClientReviewResource.class);

    private static final String ENTITY_NAME = "clientReview";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final ClientReviewService clientReviewService;

    public ClientReviewResource(ClientReviewService clientReviewService) {
        this.clientReviewService = clientReviewService;
    }

    /**
     * {@code POST  /client-reviews} : Create a new clientReview.
     *
     * @param clientReviewDTO the clientReviewDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new clientReviewDTO, or with status {@code 400 (Bad Request)} if the clientReview has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping
    public ResponseEntity<ClientReviewDTO> createClientReview(@Valid @RequestBody ClientReviewDTO clientReviewDTO)
        throws URISyntaxException {
        log.debug("REST request to save ClientReview : {}", clientReviewDTO);
        if (clientReviewDTO.getId() != null) {
            throw new BadRequestAlertException("A new clientReview cannot already have an ID", ENTITY_NAME, "idexists");
        }
        ClientReviewDTO result = clientReviewService.save(clientReviewDTO);
        return ResponseEntity.created(new URI("/api/client-reviews/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * {@code PUT  /client-reviews/:id} : Updates an existing clientReview.
     *
     * @param id the id of the clientReviewDTO to save.
     * @param clientReviewDTO the clientReviewDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated clientReviewDTO,
     * or with status {@code 400 (Bad Request)} if the clientReviewDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the clientReviewDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ClientReviewDTO> updateClientReview(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody ClientReviewDTO clientReviewDTO
    ) throws URISyntaxException {
        log.debug("REST request to update ClientReview : {}, {}", id, clientReviewDTO);
        if (clientReviewDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, clientReviewDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        ClientReviewDTO result = clientReviewService.update(clientReviewDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, clientReviewDTO.getId().toString()))
            .body(result);
    }

    /**
     * {@code PATCH  /client-reviews/:id} : Partial updates given fields of an existing clientReview, field will ignore if it is null
     *
     * @param id the id of the clientReviewDTO to save.
     * @param clientReviewDTO the clientReviewDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated clientReviewDTO,
     * or with status {@code 400 (Bad Request)} if the clientReviewDTO is not valid,
     * or with status {@code 404 (Not Found)} if the clientReviewDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the clientReviewDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<ClientReviewDTO> partialUpdateClientReview(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody ClientReviewDTO clientReviewDTO
    ) throws URISyntaxException {
        log.debug("REST request to partial update ClientReview partially : {}, {}", id, clientReviewDTO);
        if (clientReviewDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, clientReviewDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        Optional<ClientReviewDTO> result = clientReviewService.partialUpdate(clientReviewDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, clientReviewDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /client-reviews} : get all the clientReviews.
     *
     * @param pageable the pagination information.
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of clientReviews in body.
     */
    @GetMapping
    public ResponseEntity<List<ClientReviewDTO>> getAllClientReviews(
        @org.springdoc.core.annotations.ParameterObject Pageable pageable,
        @RequestParam(required = false, defaultValue = "false") boolean eagerload
    ) {
        log.debug("REST request to get a page of ClientReviews");
        if (eagerload) {
            List<ClientReviewDTO> clientReviews = clientReviewService.findAllWithEagerRelationships();
            return ResponseEntity.ok().body(clientReviews);
        } else {
            Page<ClientReviewDTO> page = clientReviewService.findAll(pageable);
            HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
            return ResponseEntity.ok().headers(headers).body(page.getContent());
        }
    }

    /**
     * {@code GET  /client-reviews/:id} : get the "id" clientReview.
     *
     * @param id the id of the clientReviewDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the clientReviewDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ClientReviewDTO> getClientReview(@PathVariable Long id) {
        log.debug("REST request to get ClientReview : {}", id);
        Optional<ClientReviewDTO> clientReviewDTO = clientReviewService.findOne(id);
        return ResponseUtil.wrapOrNotFound(clientReviewDTO);
    }

    /**
     * {@code DELETE  /client-reviews/:id} : delete the "id" clientReview.
     *
     * @param id the id of the clientReviewDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClientReview(@PathVariable Long id) {
        log.debug("REST request to delete ClientReview : {}", id);
        clientReviewService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    /**
     * {@code GET  /client-reviews/ticket/:ticketId} : get the review for a specific ticket.
     *
     * @param ticketId the ticket ID.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the clientReviewDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/ticket/{ticketId}")
    public ResponseEntity<ClientReviewDTO> getClientReviewByTicketId(@PathVariable Long ticketId) {
        log.debug("REST request to get ClientReview by ticket ID : {}", ticketId);
        Optional<ClientReviewDTO> clientReviewDTO = clientReviewService.findByTicketId(ticketId);
        return ResponseUtil.wrapOrNotFound(clientReviewDTO);
    }

    /**
     * {@code GET  /client-reviews/approved} : get all approved reviews.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of approved reviews in body.
     */
    @GetMapping("/approved")
    public ResponseEntity<List<ClientReviewDTO>> getApprovedReviews(@org.springdoc.core.annotations.ParameterObject Pageable pageable) {
        log.debug("REST request to get approved ClientReviews");
        Page<ClientReviewDTO> page = clientReviewService.findApprovedReviews(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /client-reviews/approved/all} : get all approved reviews without pagination.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of approved reviews in body.
     */
    @GetMapping("/approved/all")
    public ResponseEntity<List<ClientReviewDTO>> getAllApprovedReviews() {
        log.debug("REST request to get all approved ClientReviews");
        List<ClientReviewDTO> reviews = clientReviewService.findApprovedReviews();
        return ResponseEntity.ok().body(reviews);
    }

    /**
     * {@code GET  /client-reviews/public/approved} : get all approved reviews for public display (no authentication required).
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of approved reviews in body.
     */
    @GetMapping("/public/approved")
    public ResponseEntity<List<ClientReviewDTO>> getPublicApprovedReviews() {
        log.debug("REST request to get public approved ClientReviews");
        List<ClientReviewDTO> reviews = clientReviewService.findApprovedReviews();
        return ResponseEntity.ok().body(reviews);
    }

    /**
     * {@code GET  /client-reviews/stats} : get review statistics.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the review statistics in body.
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getReviewStats() {
        log.debug("REST request to get ClientReview statistics");
        Double averageRating = clientReviewService.getAverageRating();
        Long totalReviews = clientReviewService.getTotalApprovedReviews();

        Map<String, Object> stats = new HashMap<>();
        stats.put("averageRating", averageRating != null ? averageRating : 0.0);
        stats.put("totalReviews", totalReviews != null ? totalReviews : 0L);

        return ResponseEntity.ok().body(stats);
    }

    /**
     * {@code GET  /client-reviews/public/stats} : get review statistics for public display (no authentication required).
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the review statistics in body.
     */
    @GetMapping("/public/stats")
    public ResponseEntity<Map<String, Object>> getPublicReviewStats() {
        log.debug("REST request to get public ClientReview statistics");
        Double averageRating = clientReviewService.getAverageRating();
        Long totalReviews = clientReviewService.getTotalApprovedReviews();

        Map<String, Object> stats = new HashMap<>();
        stats.put("averageRating", averageRating != null ? averageRating : 0.0);
        stats.put("totalReviews", totalReviews != null ? totalReviews : 0L);

        return ResponseEntity.ok().body(stats);
    }

    /**
     * {@code GET  /client-reviews/pending} : get all pending reviews.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of pending reviews in body.
     */
    @GetMapping("/pending")
    public ResponseEntity<List<ClientReviewDTO>> getPendingReviews() {
        log.debug("REST request to get pending ClientReviews");
        List<ClientReviewDTO> reviews = clientReviewService.findPendingReviews();
        return ResponseEntity.ok().body(reviews);
    }

    /**
     * {@code POST  /client-reviews/:id/approve} : approve a review.
     *
     * @param id the review ID.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated clientReviewDTO.
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<ClientReviewDTO> approveReview(@PathVariable Long id) {
        log.debug("REST request to approve ClientReview : {}", id);
        Optional<ClientReviewDTO> result = clientReviewService.approveReview(id);
        return ResponseUtil.wrapOrNotFound(result, HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, id.toString()));
    }

    /**
     * {@code POST  /client-reviews/:id/reject} : reject a review.
     *
     * @param id the review ID.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated clientReviewDTO.
     */
    @PostMapping("/{id}/reject")
    public ResponseEntity<ClientReviewDTO> rejectReview(@PathVariable Long id) {
        log.debug("REST request to reject ClientReview : {}", id);
        Optional<ClientReviewDTO> result = clientReviewService.rejectReview(id);
        return ResponseUtil.wrapOrNotFound(result, HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, id.toString()));
    }
}
