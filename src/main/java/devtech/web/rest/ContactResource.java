package devtechly.web.rest;

import devtechly.security.SecurityUtils;
import devtechly.service.MailService;
import devtechly.service.dto.ContactRequestDTO;
import devtechly.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

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
    private final SpringTemplateEngine templateEngine;

    public ContactResource(MailService mailService, SpringTemplateEngine templateEngine) {
        this.mailService = mailService;
        this.templateEngine = templateEngine;
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

            // Get current authenticated user details
            String userLogin = SecurityUtils.getCurrentUserLogin().orElse("unknown");
            String userEmail = mailService.getLoggedUserEmail(userLogin);

            // Build email content using Thymeleaf template
            String content = buildEmailContentFromTemplate(contactRequest, userLogin, userEmail);

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
     * Build the email content for the contact request using Thymeleaf template.
     */
    private String buildEmailContentFromTemplate(ContactRequestDTO contactRequest, String clientLogin, String clientEmail) {
        Locale locale = Locale.FRENCH;
        Context context = new Context(locale);
        context.setVariable("contactRequest", contactRequest);
        context.setVariable("clientLogin", clientLogin);
        context.setVariable("clientEmail", clientEmail);
        return templateEngine.process("mail/contactAdminEmail", context);
    }

    /**
     * {@code POST  /contact/test-email} : Test email sending (Admin only).
     * This endpoint is for testing SMTP configuration.
     *
     * @param email the email address to send test email to.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and confirmation message.
     */
    @PostMapping("/test-email")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    public ResponseEntity<Map<String, String>> testEmail(@RequestParam(required = false) String email) {
        LOG.debug("REST request to test email sending");

        try {
            String testEmail = email != null && !email.isEmpty() ? email : adminEmail;

            String subject = "Test Email - Configuration SMTP DevTechly";
            String content =
                "<html><body>" +
                "<h2>Test d'envoi d'email</h2>" +
                "<p>Ceci est un email de test pour vérifier la configuration SMTP Gmail.</p>" +
                "<p><strong>Date:</strong> " +
                LocalDateTime.now() +
                "</p>" +
                "<p><strong>Expéditeur:</strong> contact.devtechly@gmail.com</p>" +
                "<p>Si vous recevez cet email, la configuration SMTP fonctionne correctement !</p>" +
                "<hr>" +
                "<p style='color: #6c757d; font-size: 0.9em;'>DevTechly - Test Email</p>" +
                "</body></html>";

            mailService.sendEmail(testEmail, subject, content, false, true);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Email de test envoyé avec succès à " + testEmail);
            response.put("timestamp", LocalDateTime.now().toString());
            response.put("status", "success");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            LOG.error("Error sending test email", e);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Erreur lors de l'envoi de l'email de test: " + e.getMessage());
            response.put("timestamp", LocalDateTime.now().toString());
            response.put("status", "error");
            return ResponseEntity.status(500).body(response);
        }
    }
}
