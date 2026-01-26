package devtechly.service;

import devtechly.domain.Ticket;
import devtechly.domain.User;
import devtechly.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.core.io.ClassPathResource;
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

    /**
     * Get the email of the logged in user based on their login.
     *
     * @param userLogin the login of the user.
     * @return the email of the user, or "unknown" if not found.
     */
    public String getLoggedUserEmail(String userLogin) {
        return userRepository.findOneByLogin(userLogin).map(User::getEmail).orElse("unknown");
    }

    @Async
    public void sendEmail(String to, String subject, String content, boolean isMultipart, boolean isHtml) {
        sendEmailSync(to, subject, content, isMultipart, isHtml);
    }

    private void sendEmailSync(String to, String subject, String content, boolean isMultipart, boolean isHtml) {
        LOG.info("Attempting to send email[multipart '{}' and html '{}'] to '{}' with subject '{}'", isMultipart, isHtml, to, subject);
        LOG.debug("Email content length: {} characters", content != null ? content.length() : 0);

        // Prepare message using a Spring helper
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper message = new MimeMessageHelper(mimeMessage, isMultipart, StandardCharsets.UTF_8.name());
            message.setTo(to);
            String fromEmail = jHipsterProperties.getMail().getFrom();
            LOG.debug("Sending email from: {}", fromEmail);
            message.setFrom(fromEmail);
            message.setSubject(subject);
            message.setText(content, isHtml);
            if (isHtml) {
                // Try to add the logo image as an inline resource if it exists
                try {
                    ClassPathResource logo = new ClassPathResource("static/content/images/dt-logo.png");
                    if (logo.exists()) {
                        message.addInline("dt-logo.png", logo, "image/png");
                    }
                } catch (Exception e) {
                    LOG.debug("Logo image not found, continuing without inline logo: {}", e.getMessage());
                }
            }
            javaMailSender.send(mimeMessage);
            LOG.info("Email sent successfully to User '{}'", to);
        } catch (MailException | MessagingException e) {
            LOG.error("Email could not be sent to user '{}'. Error: {}", to, e.getMessage(), e);
            if (javaMailSender instanceof org.springframework.mail.javamail.JavaMailSenderImpl) {
                org.springframework.mail.javamail.JavaMailSenderImpl mailSender =
                    (org.springframework.mail.javamail.JavaMailSenderImpl) javaMailSender;
                LOG.error(
                    "Email configuration - Host: {}, Port: {}, Username: {}, From: {}",
                    mailSender.getHost(),
                    mailSender.getPort(),
                    mailSender.getUsername(),
                    jHipsterProperties.getMail().getFrom()
                );
            }
            // Re-throw to allow caller to handle the error
            throw new RuntimeException("Failed to send email to " + to, e);
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
        Locale locale = Locale.forLanguageTag(user.getLangKey());
        Context context = new Context(locale);
        context.setVariable(USER, user);
        context.setVariable(BASE_URL, jHipsterProperties.getMail().getBaseUrl());
        String content = templateEngine.process("mail/activationEmail", context);
        String subject = messageSource.getMessage("email.activation.title", null, locale);
        sendEmailSync(user.getEmail(), subject, content, false, true);
    }

    @Async
    public void sendCreationEmail(User user) {
        LOG.debug("Sending creation email to '{}'", user.getEmail());
        Locale locale = Locale.forLanguageTag(user.getLangKey());
        Context context = new Context(locale);
        context.setVariable(USER, user);
        context.setVariable(BASE_URL, jHipsterProperties.getMail().getBaseUrl());
        String content = templateEngine.process("mail/creationEmail", context);
        String subject = messageSource.getMessage("email.creation.title", null, locale);
        sendEmailSync(user.getEmail(), subject, content, false, true);
    }

    @Async
    public void sendPasswordResetMail(User user) {
        LOG.info(
            "Sending password reset email to '{}' (login: '{}', resetKey: '{}')",
            user.getEmail(),
            user.getLogin(),
            user.getResetKey()
        );
        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            LOG.error("Cannot send password reset email: user email is null or empty for user '{}'", user.getLogin());
            throw new IllegalArgumentException("User email is required for password reset");
        }
        if (user.getResetKey() == null || user.getResetKey().isEmpty()) {
            LOG.error("Cannot send password reset email: reset key is null or empty for user '{}'", user.getLogin());
            throw new IllegalArgumentException("Reset key is required for password reset");
        }
        Locale locale = Locale.forLanguageTag(user.getLangKey());
        Context context = new Context(locale);
        context.setVariable(USER, user);
        String baseUrl = jHipsterProperties.getMail().getBaseUrl();
        LOG.debug("Using base URL for password reset: {}", baseUrl);
        context.setVariable(BASE_URL, baseUrl);
        String content = templateEngine.process("mail/passwordResetEmail", context);
        String subject = messageSource.getMessage("email.reset.title", null, locale);
        LOG.debug("Password reset email subject: {}", subject);
        sendEmailSync(user.getEmail(), subject, content, false, true);
    }

    @Async
    public void sendTicketCreatedEmail(Ticket ticket) {
        userRepository
            .findOneByLogin(ticket.getCreatedBy())
            .ifPresent(user -> {
                Locale locale = Locale.forLanguageTag(user.getLangKey());
                Context context = new Context(locale);
                context.setVariable(USER, user);
                context.setVariable("ticket", ticket);
                context.setVariable(BASE_URL, jHipsterProperties.getMail().getBaseUrl());
                String content = templateEngine.process("mail/ticketCreatedEmail", context);
                String subject = messageSource.getMessage("email.ticket.created.title", null, locale);
                sendEmailSync(user.getEmail(), subject, content, false, true);
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
                Locale locale = Locale.forLanguageTag(user.getLangKey());
                Context context = new Context(locale);
                context.setVariable(USER, user);
                context.setVariable("ticket", ticket);
                context.setVariable(BASE_URL, jHipsterProperties.getMail().getBaseUrl());
                String content = templateEngine.process("mail/ticketUpdatedEmail", context);
                String subject = messageSource.getMessage("email.ticket.updated.title", null, locale);
                sendEmailSync(user.getEmail(), subject, content, false, true);
            });
    }
}
