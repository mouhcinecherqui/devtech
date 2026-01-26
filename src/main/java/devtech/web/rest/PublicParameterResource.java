package devtechly.web.rest;

import devtechly.domain.AppParameter;
import devtechly.service.AppParameterService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for public parameters accessible by clients.
 */
@RestController
@RequestMapping("/api/public/parameters")
public class PublicParameterResource {

    private final Logger log = LoggerFactory.getLogger(PublicParameterResource.class);
    private final AppParameterService service;

    public PublicParameterResource(AppParameterService service) {
        this.service = service;
    }

    /**
     * {@code GET /api/public/parameters} : get all public parameters for clients.
     *
     * @return the list of public parameters.
     */
    @GetMapping("")
    public List<AppParameter> getPublicParameters() {
        log.debug("REST request to get public parameters for clients");
        List<AppParameter> allParams = service.findAll();
        // Filtrer seulement les paramètres nécessaires aux clients
        return allParams
            .stream()
            .filter(
                p ->
                    p.getType() != null &&
                    (p.getType().equals("ticket-status") || p.getType().equals("ticket-type") || p.getType().equals("ticket-priority"))
            )
            .toList();
    }

    /**
     * {@code GET /api/public/parameters/ticket-statuses} : get ticket statuses.
     *
     * @return the list of ticket statuses.
     */
    @GetMapping("/ticket-statuses")
    public List<AppParameter> getTicketStatuses() {
        log.debug("REST request to get public ticket statuses");
        return service.getTicketStatuses();
    }

    /**
     * {@code GET /api/public/parameters/ticket-types} : get ticket types.
     *
     * @return the list of ticket types.
     */
    @GetMapping("/ticket-types")
    public List<AppParameter> getTicketTypes() {
        log.debug("REST request to get public ticket types");
        return service.getTicketTypes();
    }

    /**
     * {@code GET /api/public/parameters/ticket-priorities} : get ticket priorities.
     *
     * @return the list of ticket priorities.
     */
    @GetMapping("/ticket-priorities")
    public List<AppParameter> getTicketPriorities() {
        log.debug("REST request to get public ticket priorities");
        return service.getTicketPriorities();
    }

    /**
     * {@code GET /api/public/parameters/defaults/ticket-status} : get default ticket status.
     *
     * @return the default ticket status.
     */
    @GetMapping("/defaults/ticket-status")
    public String getDefaultTicketStatus() {
        log.debug("REST request to get default ticket status");
        return service.getDefaultTicketStatus();
    }

    /**
     * {@code GET /api/public/parameters/defaults/ticket-type} : get default ticket type.
     *
     * @return the default ticket type.
     */
    @GetMapping("/defaults/ticket-type")
    public String getDefaultTicketType() {
        log.debug("REST request to get default ticket type");
        return service.getDefaultTicketType();
    }

    /**
     * {@code GET /api/public/parameters/defaults/ticket-priority} : get default ticket priority.
     *
     * @return the default ticket priority.
     */
    @GetMapping("/defaults/ticket-priority")
    public String getDefaultTicketPriority() {
        log.debug("REST request to get default ticket priority");
        return service.getDefaultTicketPriority();
    }
}
