package devtech.service;

import devtech.domain.ClientReview;
import devtech.repository.ClientReviewRepository;
import devtech.service.dto.ClientReviewDTO;
import devtech.service.mapper.ClientReviewMapper;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link ClientReview}.
 */
@Service
@Transactional
public class ClientReviewService {

    private final Logger log = LoggerFactory.getLogger(ClientReviewService.class);

    private final ClientReviewRepository clientReviewRepository;

    private final ClientReviewMapper clientReviewMapper;

    public ClientReviewService(ClientReviewRepository clientReviewRepository, ClientReviewMapper clientReviewMapper) {
        this.clientReviewRepository = clientReviewRepository;
        this.clientReviewMapper = clientReviewMapper;
    }

    /**
     * Save a clientReview.
     *
     * @param clientReviewDTO the entity to save.
     * @return the persisted entity.
     */
    public ClientReviewDTO save(ClientReviewDTO clientReviewDTO) {
        log.debug("Request to save ClientReview : {}", clientReviewDTO);
        ClientReview clientReview = clientReviewMapper.toEntity(clientReviewDTO);
        clientReview = clientReviewRepository.save(clientReview);
        return clientReviewMapper.toDto(clientReview);
    }

    /**
     * Update a clientReview.
     *
     * @param clientReviewDTO the entity to save.
     * @return the persisted entity.
     */
    public ClientReviewDTO update(ClientReviewDTO clientReviewDTO) {
        log.debug("Request to update ClientReview : {}", clientReviewDTO);
        ClientReview clientReview = clientReviewMapper.toEntity(clientReviewDTO);
        clientReview = clientReviewRepository.save(clientReview);
        return clientReviewMapper.toDto(clientReview);
    }

    /**
     * Partially update a clientReview.
     *
     * @param clientReviewDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<ClientReviewDTO> partialUpdate(ClientReviewDTO clientReviewDTO) {
        log.debug("Request to partially update ClientReview : {}", clientReviewDTO);

        return clientReviewRepository
            .findById(clientReviewDTO.getId())
            .map(existingClientReview -> {
                clientReviewMapper.partialUpdate(existingClientReview, clientReviewDTO);

                return existingClientReview;
            })
            .map(clientReviewRepository::save)
            .map(clientReviewMapper::toDto);
    }

    /**
     * Get all the clientReviews.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<ClientReviewDTO> findAll(Pageable pageable) {
        log.debug("Request to get all ClientReviews");
        return clientReviewRepository.findAll(pageable).map(clientReviewMapper::toDto);
    }

    /**
     * Get all the clientReviews with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public List<ClientReviewDTO> findAllWithEagerRelationships() {
        return clientReviewRepository.findAll().stream().map(clientReviewMapper::toDto).collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get one clientReview by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<ClientReviewDTO> findOne(Long id) {
        log.debug("Request to get ClientReview : {}", id);
        return clientReviewRepository.findById(id).map(clientReviewMapper::toDto);
    }

    /**
     * Delete the clientReview by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        log.debug("Request to delete ClientReview : {}", id);
        clientReviewRepository.deleteById(id);
    }

    /**
     * Find review by ticket ID.
     *
     * @param ticketId the ticket ID.
     * @return the review if exists.
     */
    @Transactional(readOnly = true)
    public Optional<ClientReviewDTO> findByTicketId(Long ticketId) {
        log.debug("Request to get ClientReview by ticket ID : {}", ticketId);
        return clientReviewRepository.findByTicketId(ticketId).map(clientReviewMapper::toDto);
    }

    /**
     * Get all approved reviews.
     *
     * @return the list of approved reviews.
     */
    @Transactional(readOnly = true)
    public List<ClientReviewDTO> findApprovedReviews() {
        log.debug("Request to get all approved ClientReviews");
        return clientReviewRepository.findApprovedReviews().stream().map(clientReviewMapper::toDto).collect(Collectors.toList());
    }

    /**
     * Get all approved reviews with pagination.
     *
     * @param pageable the pagination information.
     * @return the page of approved reviews.
     */
    @Transactional(readOnly = true)
    public Page<ClientReviewDTO> findApprovedReviews(Pageable pageable) {
        log.debug("Request to get all approved ClientReviews with pagination");
        return clientReviewRepository.findApprovedReviews(pageable).map(clientReviewMapper::toDto);
    }

    /**
     * Get average rating of all approved reviews.
     *
     * @return the average rating.
     */
    @Transactional(readOnly = true)
    public Double getAverageRating() {
        log.debug("Request to get average rating");
        return clientReviewRepository.getAverageRating();
    }

    /**
     * Get total number of approved reviews.
     *
     * @return the total count.
     */
    @Transactional(readOnly = true)
    public Long getTotalApprovedReviews() {
        log.debug("Request to get total approved reviews count");
        return clientReviewRepository.getTotalApprovedReviews();
    }

    /**
     * Get all pending reviews (not approved).
     *
     * @return the list of pending reviews.
     */
    @Transactional(readOnly = true)
    public List<ClientReviewDTO> findPendingReviews() {
        log.debug("Request to get all pending ClientReviews");
        return clientReviewRepository.findPendingReviews().stream().map(clientReviewMapper::toDto).collect(Collectors.toList());
    }

    /**
     * Approve a review.
     *
     * @param id the review ID.
     * @return the updated review.
     */
    public Optional<ClientReviewDTO> approveReview(Long id) {
        log.debug("Request to approve ClientReview : {}", id);
        return clientReviewRepository
            .findById(id)
            .map(review -> {
                review.setIsApproved(true);
                return clientReviewMapper.toDto(clientReviewRepository.save(review));
            });
    }

    /**
     * Reject a review.
     *
     * @param id the review ID.
     * @return the updated review.
     */
    public Optional<ClientReviewDTO> rejectReview(Long id) {
        log.debug("Request to reject ClientReview : {}", id);
        return clientReviewRepository
            .findById(id)
            .map(review -> {
                review.setIsApproved(false);
                return clientReviewMapper.toDto(clientReviewRepository.save(review));
            });
    }
}
