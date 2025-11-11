package devtech.web.rest;

import devtech.service.MailService;
import devtech.service.dto.ContactRequestDTO;
import devtech.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing contact requests from clients.
 */
@RestController
@RequestMapping("/api/contact")
public class ContactResource {

    private static final Logger LOG = LoggerFactory.getLogger(ContactResource.class);

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    @Value("${admin.mail:admin@localhost}")
    private String adminEmail;

    private final MailService mailService;

    public ContactResource(MailService mailService) {
        this.mailService = mailService;
    }

    /**
     * {@code POST  /contact/send-email} : Send contact email to admin.
     *
     * @param contactRequest the contact request data.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and confirmation message.
     * @throws BadRequestAlertException {@code 400 (Bad Request)} if the request is invalid.
     */
    @PostMapping("/send-email")
    @PreAuthorize("hasAnyAuthority('ROLE_CLIENT')")
    public ResponseEntity<Map<String, String>> sendContactEmail(@Valid @RequestBody ContactRequestDTO contactRequest) {
        LOG.debug("REST request to send contact email: {}", contactRequest);

        try {
            // Prepare email content
            String subject = String.format(
                "[%s] Contact Client - %s",
                contactRequest.getPriority().toUpperCase(),
                contactRequest.getSubject()
            );

            String content = buildEmailContent(contactRequest);

            // Send email to admin
            mailService.sendEmail(adminEmail, subject, content, false, true);

            // Send confirmation email to client (optional)
            // mailService.sendEmail(clientEmail, "Confirmation de réception", confirmationContent, false, true);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Votre message a été envoyé avec succès");
            response.put("timestamp", LocalDateTime.now().toString());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            LOG.error("Error sending contact email", e);
            throw new BadRequestAlertException("Erreur lors de l'envoi de l'email", "contact", "emailError");
        }
    }

    /**
     * Build the email content for the contact request.
     */
    private String buildEmailContent(ContactRequestDTO contactRequest) {
        StringBuilder content = new StringBuilder();
        content.append("<html><body>");
        content.append("<h2>Nouvelle demande de contact client</h2>");
        content.append("<div style='background-color: #f8f9fa; padding: 20px; border-radius: 5px; margin: 20px 0;'>");

        content.append("<h3>Informations de la demande</h3>");
        content.append("<p><strong>Priorité:</strong> ").append(contactRequest.getPriority()).append("</p>");
        content.append("<p><strong>Sujet:</strong> ").append(contactRequest.getSubject()).append("</p>");
        content.append("<p><strong>Date:</strong> ").append(contactRequest.getTimestamp()).append("</p>");

        content.append("<h3>Message</h3>");
        content.append("<div style='background-color: white; padding: 15px; border-left: 4px solid #007bff;'>");
        content.append("<p>").append(contactRequest.getMessage().replace("\n", "<br>")).append("</p>");
        content.append("</div>");

        content.append("<hr style='margin: 20px 0;'>");
        content.append("<p style='color: #6c757d; font-size: 0.9em;'>");
        content.append("Ce message a été envoyé depuis l'interface client de ").append(applicationName);
        content.append("</p>");

        content.append("</div>");
        content.append("</body></html>");

        return content.toString();
    }
}
