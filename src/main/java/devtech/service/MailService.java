package devtech.service;

import devtech.domain.Ticket;
import devtech.domain.User;
import devtech.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import tech.jhipster.config.JHipsterProperties;

/**
 * Service for sending emails asynchronously.
 * <p>
 * We use the {@link Async} annotation to send emails asynchronously.
 */
@Service
public class MailService {

    private static final Logger LOG = LoggerFactory.getLogger(MailService.class);

    private static final String USER = "user";

    private static final String BASE_URL = "baseUrl";

    private final JHipsterProperties jHipsterProperties;

    private final JavaMailSender javaMailSender;

    private final MessageSource messageSource;

    private final SpringTemplateEngine templateEngine;

    private final UserRepository userRepository;

    @Value("${admin.mail:admin@localhost}")
    private String adminEmail;

    public MailService(
        JHipsterProperties jHipsterProperties,
        JavaMailSender javaMailSender,
        MessageSource messageSource,
        SpringTemplateEngine templateEngine,
        UserRepository userRepository
    ) {
        this.jHipsterProperties = jHipsterProperties;
        this.javaMailSender = javaMailSender;
        this.messageSource = messageSource;
        this.templateEngine = templateEngine;
        this.userRepository = userRepository;
    }

    @Async
    public void sendEmail(String to, String subject, String content, boolean isMultipart, boolean isHtml) {
        sendEmailSync(to, subject, content, isMultipart, isHtml);
    }

    private void sendEmailSync(String to, String subject, String content, boolean isMultipart, boolean isHtml) {
        LOG.debug(
            "Send email[multipart '{}' and html '{}'] to '{}' with subject '{}' and content={}",
            isMultipart,
            isHtml,
            to,
            subject,
            content
        );

        // Prepare message using a Spring helper
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper message = new MimeMessageHelper(mimeMessage, isMultipart, StandardCharsets.UTF_8.name());
            message.setTo(to);
            message.setFrom(jHipsterProperties.getMail().getFrom());
            message.setSubject(subject);
            message.setText(content, isHtml);
            javaMailSender.send(mimeMessage);
            LOG.debug("Sent email to User '{}'", to);
        } catch (MailException | MessagingException e) {
            LOG.warn("Email could not be sent to user '{}'", to, e);
        }
    }

    @Async
    public void sendEmailFromTemplate(User user, String templateName, String titleKey) {
        sendEmailFromTemplateSync(user, templateName, titleKey);
    }

    private void sendEmailFromTemplateSync(User user, String templateName, String titleKey) {
        if (user.getEmail() == null) {
            LOG.debug("Email doesn't exist for user '{}'", user.getLogin());
            return;
        }
        Locale locale = Locale.forLanguageTag(user.getLangKey());
        Context context = new Context(locale);
        context.setVariable(USER, user);
        context.setVariable(BASE_URL, jHipsterProperties.getMail().getBaseUrl());
        String content = templateEngine.process(templateName, context);
        String subject = messageSource.getMessage(titleKey, null, locale);
        sendEmailSync(user.getEmail(), subject, content, false, true);
    }

    @Async
    public void sendActivationEmail(User user) {
        LOG.debug("Sending activation email to '{}'", user.getEmail());
        sendEmailFromTemplateSync(user, "mail/activationEmail", "email.activation.title");
    }

    @Async
    public void sendCreationEmail(User user) {
        LOG.debug("Sending creation email to '{}'", user.getEmail());
        sendEmailFromTemplateSync(user, "mail/creationEmail", "email.activation.title");
    }

    @Async
    public void sendPasswordResetMail(User user) {
        LOG.debug("Sending password reset email to '{}'", user.getEmail());
        sendEmailFromTemplateSync(user, "mail/passwordResetEmail", "email.reset.title");
    }

    @Async
    public void sendTicketCreatedEmail(Ticket ticket) {
        userRepository
            .findOneByLogin(ticket.getCreatedBy())
            .ifPresent(user -> {
                String subject = "Votre ticket a été créé";
                String content =
                    "Bonjour " +
                    user.getFirstName() +
                    ",\n\nVotre ticket (ID: " +
                    ticket.getId() +
                    ") a bien été créé.\n\nDescription : " +
                    ticket.getDescription() +
                    "\n\nStatut : " +
                    ticket.getStatus() +
                    "\n\nMerci de votre confiance.";
                sendEmail(user.getEmail(), subject, content, false, false);
            });
        // Notifier l'admin
        String subjectAdmin = "Nouveau ticket créé (ID: " + ticket.getId() + ")";
        String contentAdmin = "Un nouveau ticket a été créé par " + ticket.getCreatedBy() + ".\nDescription : " + ticket.getDescription();
        sendEmail(adminEmail, subjectAdmin, contentAdmin, false, false);
    }

    @Async
    public void sendTicketUpdatedEmail(Ticket ticket) {
        userRepository
            .findOneByLogin(ticket.getCreatedBy())
            .ifPresent(user -> {
                String subject = "Mise à jour de votre ticket (ID: " + ticket.getId() + ")";
                String content =
                    "Bonjour " +
                    user.getFirstName() +
                    ",\n\nVotre ticket a été mis à jour.\n\nNouveau statut : " +
                    ticket.getStatus() +
                    "\n\nDernier message : " +
                    (ticket.getMessages() != null && !ticket.getMessages().isEmpty()
                            ? ticket.getMessages().get(ticket.getMessages().size() - 1)
                            : "") +
                    "\n\nMerci de votre confiance.";
                sendEmail(user.getEmail(), subject, content, false, false);
            });
    }
}
