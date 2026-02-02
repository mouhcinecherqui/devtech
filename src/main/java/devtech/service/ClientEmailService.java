package devtechly.service;

import devtechly.domain.AppUser;
import devtechly.domain.Devis;
import devtechly.domain.Message;
import devtechly.domain.Paiement;
import devtechly.domain.Ticket;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Service for sending emails to clients for various events.
 */
@Service
public class ClientEmailService {

    private static final Logger log = LoggerFactory.getLogger(ClientEmailService.class);

    private final JavaMailSender javaMailSender;
    private final TemplateEngine templateEngine;

    public ClientEmailService(JavaMailSender javaMailSender, TemplateEngine templateEngine) {
        this.javaMailSender = javaMailSender;
        this.templateEngine = templateEngine;
    }

    /**
     * Send welcome email to new client.
     */
    public void sendWelcomeEmail(AppUser client) {
        String subject = "Bienvenue sur DevTechly - Votre compte a été créé avec succès";

        Context context = new Context();
        context.setVariable("client", client);

        String content = templateEngine.process("mail/welcomeEmail", context);

        sendHtmlEmail(client.getEmail(), subject, content);
    }

    /**
     * Send email when a ticket is created.
     */
    public void sendTicketCreatedEmail(AppUser client, Ticket ticket) {
        String subject = String.format("DevTechly - Nouveau ticket créé #%d", ticket.getId());

        Context context = new Context();
        context.setVariable("client", client);
        context.setVariable("ticket", ticket);
        context.setVariable("baseUrl", "https://www.devtechly.com");

        String content = templateEngine.process("mail/ticketCreatedEmail", context);

        sendHtmlEmail(client.getEmail(), subject, content);
    }

    /**
     * Send email when a ticket is updated.
     */
    public void sendTicketUpdatedEmail(AppUser client, Ticket ticket) {
        String subject = String.format("DevTechly - Mise à jour du ticket #%d", ticket.getId());

        Context context = new Context();
        context.setVariable("client", client);
        context.setVariable("ticket", ticket);

        // Créer un objet Devis temporaire si le ticket a des informations de paiement
        Devis devisForEmail = null;
        if (ticket.getPaymentAmount() != null && ticket.getPaymentAmount() > 0) {
            devisForEmail = new Devis();
            devisForEmail.setId(ticket.getId());
            devisForEmail.setAmount(ticket.getPaymentAmount());
            devisForEmail.setDescription(ticket.getDescription());
            devisForEmail.setDateValidation(ticket.getLastModifiedDate());
        }
        context.setVariable("devis", devisForEmail);
        context.setVariable("baseUrl", "https://www.devtechly.com");

        String content = templateEngine.process("mail/ticketUpdatedEmail", context);

        sendHtmlEmail(client.getEmail(), subject, content);
    }

    /**
     * Send email when a message is added to a ticket.
     */
    public void sendTicketMessageEmail(AppUser client, Ticket ticket, Message message) {
        String subject = String.format("DevTechly - Nouveau message sur le ticket #%d", ticket.getId());

        Context context = new Context();
        context.setVariable("client", client);
        context.setVariable("ticket", ticket);
        context.setVariable("message", message);
        context.setVariable("baseUrl", "https://www.devtechly.com");

        String content = templateEngine.process("mail/ticketMessageEmail", context);

        sendHtmlEmail(client.getEmail(), subject, content);
    }

    /**
     * Send email when a quote is sent to client.
     */
    public void sendQuoteSentEmail(AppUser client, Devis devis) {
        String subject = String.format("DevTechly - Nouveau devis reçu #%d", devis.getId());

        Context context = new Context();
        context.setVariable("client", client);
        context.setVariable("devis", devis);
        context.setVariable("baseUrl", "https://www.devtechly.com");

        String content = templateEngine.process("mail/quoteSentEmail", context);

        sendHtmlEmail(client.getEmail(), subject, content);
    }

    /**
     * Send email when a quote is validated.
     */
    public void sendQuoteValidatedEmail(AppUser client, Devis devis) {
        String subject = String.format("DevTechly - Devis validé #%d", devis.getId());

        Context context = new Context();
        context.setVariable("client", client);
        context.setVariable("devis", devis);
        context.setVariable("baseUrl", "https://www.devtechly.com");

        String content = templateEngine.process("mail/quoteValidatedEmail", context);

        sendHtmlEmail(client.getEmail(), subject, content);
    }

    /**
     * Send email when a payment is validated.
     */
    public void sendPaymentValidatedEmail(AppUser client, Paiement paiement) {
        String subject = String.format("DevTechly - Paiement validé #%d", paiement.getId());

        Context context = new Context();
        context.setVariable("client", client);
        context.setVariable("paiement", paiement);
        context.setVariable("baseUrl", "https://www.devtechly.com");

        String content = templateEngine.process("mail/paymentValidatedEmail", context);

        sendHtmlEmail(client.getEmail(), subject, content);
    }

    /**
     * Send email when ticket status changes.
     */
    public void sendTicketStatusChangedEmail(AppUser client, Ticket ticket, String oldStatus, String newStatus) {
        String subject = String.format("DevTechly - Changement de statut du ticket #%d", ticket.getId());

        Context context = new Context();
        context.setVariable("client", client);
        context.setVariable("ticket", ticket);
        context.setVariable("oldStatus", oldStatus);
        context.setVariable("newStatus", newStatus);
        context.setVariable("baseUrl", "https://www.devtechly.com");

        String content = templateEngine.process("mail/ticketStatusChangedEmail", context);

        sendHtmlEmail(client.getEmail(), subject, content);
    }

    /**
     * Generic method to send plain text email.
     */
    private void sendEmail(String to, String subject, String content) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);
            message.setFrom("noreply@devtechly.com");

            javaMailSender.send(message);
            log.info("Email sent successfully to '{}' with subject '{}'", to, subject);
        } catch (Exception e) {
            log.error("Failed to send email to '{}': {}", to, e.getMessage(), e);
        }
    }

    /**
     * Generic method to send HTML email.
     */
    private void sendHtmlEmail(String to, String subject, String content) {
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper message = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());

            message.setTo(to);
            message.setSubject(subject);
            message.setText(content, true); // true indicates HTML
            message.setFrom("noreply@devtechly.com");

            // Logo inline pour les templates (cid:dt-logo.png)
            try {
                ClassPathResource logo = new ClassPathResource("static/content/images/dt-logo.png");
                if (logo.exists()) {
                    message.addInline("dt-logo.png", logo, "image/png");
                }
            } catch (Exception e) {
                log.debug("Logo not attached to email: {}", e.getMessage());
            }

            javaMailSender.send(mimeMessage);
            log.info("HTML email sent successfully to '{}' with subject '{}'", to, subject);
        } catch (MessagingException e) {
            log.error("Failed to send HTML email to '{}': {}", to, e.getMessage(), e);
        }
    }
}
