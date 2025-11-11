package devtech.web.rest;

import devtech.domain.Message;
import devtech.repository.MessageRepository;
import devtech.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link devtech.domain.Message}.
 */
@RestController
@RequestMapping("/api/messages")
public class MessageResource {

    private final Logger log = LoggerFactory.getLogger(MessageResource.class);

    private static final String ENTITY_NAME = "message";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final MessageRepository messageRepository;

    public MessageResource(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    /**
     * {@code POST  /messages} : Create a new message.
     *
     * @param message the message to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new message, or with status {@code 400 (Bad Request)} if the message has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<Message> createMessage(@Valid @RequestBody Message message) throws URISyntaxException {
        log.debug("REST request to save Message : {}", message);
        if (message.getId() != null) {
            throw new BadRequestAlertException("A new message cannot already have an ID", ENTITY_NAME, "idexists");
        }
        Message result = messageRepository.save(message);
        return ResponseEntity.created(new URI("/api/messages/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * {@code PUT  /messages/:id} : Updates an existing message.
     *
     * @param id the id of the message to save.
     * @param message the message to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated message,
     * or with status {@code 400 (Bad Request)} if the message is not valid,
     * or with status {@code 500 (Internal Server Error)} if the message couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Message> updateMessage(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody Message message
    ) throws URISyntaxException {
        log.debug("REST request to update Message : {}, {}", id, message);
        if (message.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, message.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!messageRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Message result = messageRepository.save(message);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, message.getId().toString()))
            .body(result);
    }

    /**
     * {@code PATCH  /messages/:id} : Partial updates given fields of an existing message, field will ignore if it is null
     *
     * @param id the id of the message to save.
     * @param message the message to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated message,
     * or with status {@code 400 (Bad Request)} if the message is not valid,
     * or with status {@code 404 (Not Found)} if the message is not found,
     * or with status {@code 500 (Internal Server Error)} if the message couldn't be updated.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<Message> partialUpdateMessage(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody Message message
    ) throws URISyntaxException {
        log.debug("REST request to partial update Message partially : {}, {}", id, message);
        if (message.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, message.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!messageRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<Message> result = messageRepository
            .findById(message.getId())
            .map(existingMessage -> {
                if (message.getContent() != null) {
                    existingMessage.setContent(message.getContent());
                }
                if (message.getTimestamp() != null) {
                    existingMessage.setTimestamp(message.getTimestamp());
                }
                if (message.getTicketId() != null) {
                    existingMessage.setTicketId(message.getTicketId());
                }
                if (message.getSenderId() != null) {
                    existingMessage.setSenderId(message.getSenderId());
                }
                if (message.getSenderName() != null) {
                    existingMessage.setSenderName(message.getSenderName());
                }
                if (message.getSenderEmail() != null) {
                    existingMessage.setSenderEmail(message.getSenderEmail());
                }
                if (message.getIsFromClient() != null) {
                    existingMessage.setIsFromClient(message.getIsFromClient());
                }

                return existingMessage;
            })
            .map(messageRepository::save);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, message.getId().toString())
        );
    }

    /**
     * {@code GET  /messages} : get all the messages.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of messages in body.
     */
    @GetMapping("")
    public ResponseEntity<List<Message>> getAllMessages(@org.springdoc.core.annotations.ParameterObject Pageable pageable) {
        log.debug("REST request to get a page of Messages");
        Page<Message> page = messageRepository.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /messages/:id} : get the "id" message.
     *
     * @param id the id of the message to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the message, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Message> getMessage(@PathVariable Long id) {
        log.debug("REST request to get Message : {}", id);
        Optional<Message> message = messageRepository.findById(id);
        return ResponseUtil.wrapOrNotFound(message);
    }

    /**
     * {@code DELETE  /messages/:id} : delete the "id" message.
     *
     * @param id the id of the message to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (No Content)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMessage(@PathVariable Long id) {
        log.debug("REST request to delete Message : {}", id);
        messageRepository.deleteById(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    /**
     * {@code GET  /messages/recent} : get recent messages since a specific timestamp.
     *
     * @param since the timestamp to get messages since.
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of recent messages in body.
     */
    @GetMapping("/recent")
    public ResponseEntity<List<Message>> getRecentMessages(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant since,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        log.debug("REST request to get recent Messages since: {}", since);
        List<Message> messages = messageRepository.findByTimestampAfterOrderByTimestampDesc(since, pageable);
        return ResponseEntity.ok().body(messages);
    }

    /**
     * {@code GET  /messages/ticket/:ticketId} : get all messages for a specific ticket.
     *
     * @param ticketId the id of the ticket.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of messages in body.
     */
    @GetMapping("/ticket/{ticketId}")
    public ResponseEntity<List<Message>> getMessagesByTicket(@PathVariable Long ticketId) {
        log.debug("REST request to get Messages for ticket: {}", ticketId);
        List<Message> messages = messageRepository.findByTicketIdOrderByTimestampAsc(ticketId);
        return ResponseEntity.ok().body(messages);
    }

    /**
     * {@code GET  /messages/client-recent} : get recent messages from clients only.
     *
     * @param since the timestamp to get messages since.
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of recent client messages in body.
     */
    @GetMapping("/client-recent")
    public ResponseEntity<List<Message>> getRecentClientMessages(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant since,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        log.debug("REST request to get recent client Messages since: {}", since);
        List<Message> messages = messageRepository.findByTimestampAfterAndIsFromClientTrueOrderByTimestampDesc(since, pageable);
        return ResponseEntity.ok().body(messages);
    }
}
