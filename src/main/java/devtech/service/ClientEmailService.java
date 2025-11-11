package devtech.service;

import devtech.domain.AppUser;
import devtech.domain.Devis;
import devtech.domain.Message;
import devtech.domain.Paiement;
import devtech.domain.Ticket;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
        String subject = String.format("DevTech - Mise à jour du ticket #%d", ticket.getId());
        String content = String.format(
            "Bonjour %s %s,\n\n" +
            "Votre ticket de support a été mis à jour.\n\n" +
            "Détails du ticket :\n" +
            "• Numéro : #%d\n" +
            "• Type : %s\n" +
            "• Description : %s\n" +
            "• Nouveau statut : %s\n" +
            "• Dernière mise à jour : %s\n\n" +
            "Vous pouvez consulter tous les détails depuis votre espace client.\n\n" +
            "Cordialement,\n" +
            "L'équipe DevTech",
            client.getFirstName(),
            client.getLastName(),
            ticket.getId(),
            ticket.getType(),
            ticket.getDescription(),
            ticket.getStatus(),
            ticket.getLastModifiedDate()
        );

        sendEmail(client.getEmail(), subject, content);
    }

    /**
     * Send email when a message is added to a ticket.
     */
    public void sendTicketMessageEmail(AppUser client, Ticket ticket, Message message) {
        String subject = String.format("DevTech - Nouveau message sur le ticket #%d", ticket.getId());
        String content = String.format(
            "Bonjour %s %s,\n\n" +
            "Un nouveau message a été ajouté à votre ticket de support.\n\n" +
            "Détails :\n" +
            "• Ticket : #%d\n" +
            "• Expéditeur : %s\n" +
            "• Message : %s\n" +
            "• Date : %s\n\n" +
            "Vous pouvez répondre directement depuis votre espace client.\n\n" +
            "Cordialement,\n" +
            "L'équipe DevTech",
            client.getFirstName(),
            client.getLastName(),
            ticket.getId(),
            message.getSender(),
            message.getContent(),
            message.getCreatedDate()
        );

        sendEmail(client.getEmail(), subject, content);
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
        String subject = String.format("DevTech - Devis validé #%d", devis.getId());
        String content = String.format(
            "Bonjour %s %s,\n\n" +
            "Votre devis a été validé avec succès.\n\n" +
            "Détails du devis :\n" +
            "• Numéro : #%d\n" +
            "• Montant : %.2f MAD\n" +
            "• Description : %s\n" +
            "• Date de validation : %s\n\n" +
            "Vous pouvez maintenant procéder au paiement depuis votre espace client.\n\n" +
            "Cordialement,\n" +
            "L'équipe DevTech",
            client.getFirstName(),
            client.getLastName(),
            devis.getId(),
            devis.getAmount(),
            devis.getDescription(),
            devis.getDateValidation()
        );

        sendEmail(client.getEmail(), subject, content);
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
        String subject = String.format("DevTech - Changement de statut du ticket #%d", ticket.getId());
        String content = String.format(
            "Bonjour %s %s,\n\n" +
            "Le statut de votre ticket de support a été modifié.\n\n" +
            "Détails :\n" +
            "• Ticket : #%d\n" +
            "• Ancien statut : %s\n" +
            "• Nouveau statut : %s\n" +
            "• Date de modification : %s\n\n" +
            "Vous pouvez suivre l'avancement depuis votre espace client.\n\n" +
            "Cordialement,\n" +
            "L'équipe DevTech",
            client.getFirstName(),
            client.getLastName(),
            ticket.getId(),
            oldStatus,
            newStatus,
            ticket.getLastModifiedDate()
        );

        sendEmail(client.getEmail(), subject, content);
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

            javaMailSender.send(mimeMessage);
            log.info("HTML email sent successfully to '{}' with subject '{}'", to, subject);
        } catch (MessagingException e) {
            log.error("Failed to send HTML email to '{}': {}", to, e.getMessage(), e);
        }
    }
}
