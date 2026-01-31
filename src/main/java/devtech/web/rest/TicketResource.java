package devtechly.web.rest;

import devtechly.config.ApplicationProperties;
import devtechly.domain.AppUser;
import devtechly.domain.Devis;
import devtechly.domain.Message;
import devtechly.domain.Paiement;
import devtechly.domain.Ticket;
import devtechly.domain.TicketMessage;
import devtechly.repository.AppUserRepository;
import devtechly.repository.TicketMessageRepository;
import devtechly.repository.TicketRepository;
import devtechly.repository.UserRepository;
import devtechly.security.SecurityUtils;
import devtechly.service.ActivityIntegrationService;
import devtechly.service.ClientEmailService;
import devtechly.service.MailService;
import devtechly.service.NotificationService;
import devtechly.service.TicketMessageService;
import jakarta.validation.Valid;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.PaginationUtil;

@RestController
@RequestMapping("/api/tickets")
public class TicketResource {

    private static final Logger LOG = LoggerFactory.getLogger(TicketResource.class);

    private final TicketRepository ticketRepository;
    private final MailService mailService;
    private final NotificationService notificationService;
    private final TicketMessageService ticketMessageService;
    private final ApplicationProperties applicationProperties;
    private final TicketMessageRepository ticketMessageRepository;
    private final UserRepository userRepository;
    private final ClientEmailService clientEmailService;
    private final AppUserRepository appUserRepository;
    private final ActivityIntegrationService activityIntegrationService;

    public TicketResource(
        TicketRepository ticketRepository,
        MailService mailService,
        NotificationService notificationService,
        TicketMessageService ticketMessageService,
        ApplicationProperties applicationProperties,
        TicketMessageRepository ticketMessageRepository,
        UserRepository userRepository,
        ClientEmailService clientEmailService,
        AppUserRepository appUserRepository,
        ActivityIntegrationService activityIntegrationService
    ) {
        this.ticketRepository = ticketRepository;
        this.mailService = mailService;
        this.notificationService = notificationService;
        this.ticketMessageService = ticketMessageService;
        this.applicationProperties = applicationProperties;
        this.ticketMessageRepository = ticketMessageRepository;
        this.userRepository = userRepository;
        this.clientEmailService = clientEmailService;
        this.appUserRepository = appUserRepository;
        this.activityIntegrationService = activityIntegrationService;
    }

    // Méthode utilitaire pour récupérer l'AppUser à partir du login
    private AppUser getAppUserByLogin(String login) {
        try {
            return appUserRepository.findByEmail(login).orElse(null);
        } catch (Exception e) {
            LOG.error("Erreur lors de la récupération de l'AppUser pour le login {}: {}", login, e.getMessage());
            return null;
        }
    }

    // Lister les tickets de l'utilisateur connecté
    @GetMapping("")
    public ResponseEntity<List<TicketDTO>> getMyTickets(@org.springdoc.core.annotations.ParameterObject Pageable pageable) {
        try {
            String login = SecurityUtils.getCurrentUserLogin().orElse(null);
            if (login == null) return ResponseEntity.ok(List.of());

            boolean isAdmin = SecurityContextHolder.getContext()
                .getAuthentication()
                .getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(auth -> auth.equals("ROLE_ADMIN") || auth.equals("ROLE_MANAGER"));

            Page<Ticket> ticketPage;
            if (isAdmin) {
                ticketPage = ticketRepository.findAll(pageable);
            } else {
                // Pour les utilisateurs non-admin, on doit implémenter une méthode de pagination personnalisée
                // Pour l'instant, on utilise findAll et on filtre côté serveur
                ticketPage = ticketRepository.findAll(pageable);
                // Filtrer les tickets de l'utilisateur
                List<Ticket> userTickets = ticketPage.getContent().stream().filter(ticket -> login.equals(ticket.getCreatedBy())).toList();
                // Créer une page personnalisée (simplifié pour l'instant)
                ticketPage = new org.springframework.data.domain.PageImpl<>(userTickets, pageable, userTickets.size());
            }

            // Convertir en DTOs pour éviter les problèmes de sérialisation
            List<TicketDTO> ticketDTOs = new ArrayList<>();
            for (Ticket ticket : ticketPage.getContent()) {
                try {
                    // Charger les messages selon le rôle
                    List<TicketMessage> messages;
                    if (isAdmin) {
                        messages = ticketMessageService.getTicketMessages(ticket.getId());
                    } else {
                        messages = ticketMessageService.getPublicTicketMessages(ticket.getId());
                    }

                    if (messages != null && !messages.isEmpty()) {
                        List<String> messageStrings = convertMessagesToStrings(messages);
                        ticket.setMessageStrings(messageStrings);
                    } else {
                        ticket.setMessageStrings(new ArrayList<>());
                    }

                    ticketDTOs.add(new TicketDTO(ticket, userRepository));
                } catch (Exception e) {
                    // Si il y a une erreur avec les messages, on continue sans messages
                    System.err.println("Erreur lors du chargement des messages pour le ticket " + ticket.getId() + ": " + e.getMessage());
                    ticket.setMessageStrings(new ArrayList<>());
                    ticketDTOs.add(new TicketDTO(ticket, userRepository));
                }
            }

            HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(
                ServletUriComponentsBuilder.fromCurrentRequest(),
                ticketPage
            );
            return new ResponseEntity<>(ticketDTOs, headers, HttpStatus.OK);
        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération des tickets: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    // Méthode utilitaire pour convertir les TicketMessage en chaînes
    private List<String> convertMessagesToStrings(List<TicketMessage> messages) {
        try {
            if (messages == null || messages.isEmpty()) {
                LOG.info("Aucun message à convertir");
                return List.of();
            }

            LOG.info("Conversion de {} messages en strings", messages.size());

            List<String> result = messages
                .stream()
                .filter(message -> message != null && message.getContent() != null)
                .map(message -> {
                    try {
                        String prefix = message.getAuthorType() == TicketMessage.AuthorType.CLIENT ? "[CLIENT] " : "";
                        return prefix + message.getContent();
                    } catch (Exception e) {
                        LOG.error("Erreur lors de la conversion du message {}: {}", message.getId(), e.getMessage());
                        return "[ERREUR] Message non lisible";
                    }
                })
                .toList();

            LOG.info("Conversion terminée: {} messages convertis", result.size());
            return result;
        } catch (Exception e) {
            LOG.error("Erreur lors de la conversion des messages: {}", e.getMessage(), e);
            return List.of();
        }
    }

    // Récupérer un ticket avec ses messages
    @GetMapping("/{id}")
    public ResponseEntity<TicketDTO> getTicket(@PathVariable Long id) {
        try {
            String login = SecurityUtils.getCurrentUserLogin().orElse(null);
            if (login == null) return ResponseEntity.badRequest().build();

            Ticket ticket = ticketRepository.findById(id).orElse(null);
            if (ticket == null) return ResponseEntity.notFound().build();

            boolean isAdmin = SecurityContextHolder.getContext()
                .getAuthentication()
                .getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(auth -> auth.equals("ROLE_ADMIN") || auth.equals("ROLE_MANAGER"));

            // Vérifier les permissions
            if (!isAdmin && !ticket.getCreatedBy().equals(login)) {
                return ResponseEntity.status(403).build();
            }

            // S'assurer que les messages ne sont pas chargés pour éviter les problèmes de sérialisation
            ticket.setMessages(new ArrayList<>());

            // Charger les messages selon le rôle (avec gestion d'erreur robuste)
            try {
                if (isAdmin) {
                    List<TicketMessage> messages = ticketMessageService.getTicketMessages(id);
                    if (messages != null) {
                        List<String> messageStrings = convertMessagesToStrings(messages);
                        ticket.setMessageStrings(messageStrings);
                    } else {
                        ticket.setMessageStrings(List.of());
                    }
                } else {
                    List<TicketMessage> messages = ticketMessageService.getPublicTicketMessages(id);
                    if (messages != null) {
                        List<String> messageStrings = convertMessagesToStrings(messages);
                        ticket.setMessageStrings(messageStrings);
                    } else {
                        ticket.setMessageStrings(List.of());
                    }
                }
            } catch (Exception e) {
                // Si il y a une erreur avec les messages (table inexistante, etc.), on continue sans messages
                System.err.println("Erreur lors du chargement des messages pour le ticket " + id + ": " + e.getMessage());
                ticket.setMessageStrings(List.of());
            }

            return ResponseEntity.ok(new TicketDTO(ticket, userRepository));
        } catch (Exception e) {
            // Log l'erreur pour le debugging
            System.err.println("Erreur lors de la récupération du ticket " + id + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    // Récupérer les messages d'un ticket
    @GetMapping("/{id}/messages")
    public ResponseEntity<List<String>> getTicketMessages(@PathVariable Long id) {
        try {
            LOG.info("Tentative de récupération des messages pour le ticket {}", id);

            String login = SecurityUtils.getCurrentUserLogin().orElse(null);
            if (login == null) {
                LOG.error("Utilisateur non connecté");
                return ResponseEntity.badRequest().build();
            }

            LOG.info("Utilisateur connecté: {}", login);

            Ticket ticket = ticketRepository.findById(id).orElse(null);
            if (ticket == null) {
                LOG.error("Ticket non trouvé avec l'ID: {}", id);
                return ResponseEntity.notFound().build();
            }

            LOG.info("Ticket trouvé: {}", ticket.getId());

            boolean isAdmin = SecurityContextHolder.getContext()
                .getAuthentication()
                .getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(auth -> auth.equals("ROLE_ADMIN") || auth.equals("ROLE_MANAGER"));

            LOG.info("Utilisateur est admin: {}", isAdmin);

            // Vérifier les permissions
            if (!isAdmin && !ticket.getCreatedBy().equals(login)) {
                LOG.error("Permission refusée pour l'utilisateur {} sur le ticket {}", login, id);
                return ResponseEntity.status(403).build();
            }

            // Charger les messages selon le rôle
            try {
                List<TicketMessage> messages;
                if (isAdmin) {
                    LOG.info("Chargement de tous les messages pour l'admin");
                    messages = ticketMessageService.getTicketMessages(id);
                } else {
                    LOG.info("Chargement des messages publics pour le client");
                    messages = ticketMessageService.getPublicTicketMessages(id);
                }

                LOG.info("Nombre de messages trouvés: {}", messages != null ? messages.size() : 0);

                List<String> messageStrings = convertMessagesToStrings(messages);
                LOG.info("Messages convertis en strings: {}", messageStrings.size());

                return ResponseEntity.ok(messageStrings);
            } catch (Exception e) {
                LOG.error("Erreur lors du chargement des messages pour le ticket {}: {}", id, e.getMessage(), e);
                // Retourner une liste vide au lieu d'une erreur 500
                return ResponseEntity.ok(List.of());
            }
        } catch (Exception e) {
            LOG.error("Erreur lors de la récupération des messages du ticket {}: {}", id, e.getMessage(), e);
            // Retourner une liste vide au lieu d'une erreur 500
            return ResponseEntity.ok(List.of());
        }
    }

    // Créer un ticket avec upload d'image
    @PostMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public ResponseEntity<Ticket> createTicketWithImage(
        @RequestParam("type") String type,
        @RequestParam("description") String description,
        @RequestParam(value = "backofficeUrl", required = false) String backofficeUrl,
        @RequestParam(value = "backofficeLogin", required = false) String backofficeLogin,
        @RequestParam(value = "backofficePassword", required = false) String backofficePassword,
        @RequestParam(value = "hostingUrl", required = false) String hostingUrl,
        @RequestParam(value = "image", required = false) MultipartFile image
    ) throws URISyntaxException {
        try {
            String login = SecurityUtils.getCurrentUserLogin().orElse(null);
            if (login == null) return ResponseEntity.badRequest().build();

            Ticket ticket = new Ticket();
            ticket.setType(type);
            ticket.setDescription(description);
            ticket.setBackofficeUrl(backofficeUrl);
            ticket.setBackofficeLogin(backofficeLogin);
            ticket.setBackofficePassword(backofficePassword);
            ticket.setHostingUrl(hostingUrl);
            ticket.setCreatedBy(login);
            ticket.setCreatedDate(java.time.Instant.now());

            // Gérer l'upload d'image
            if (image != null && !image.isEmpty()) {
                try {
                    // Vérifier la taille du fichier
                    long maxFileSize = applicationProperties.getUpload().getMaxFileSizeInBytes();
                    if (image.getSize() > maxFileSize) {
                        return ResponseEntity.badRequest().build();
                    }

                    String imageUrl = saveImage(image);
                    ticket.setImageUrl(imageUrl);
                } catch (IOException e) {
                    LOG.error("Erreur lors de l'upload de l'image: {}", e.getMessage());
                    return ResponseEntity.badRequest().build();
                }
            }

            Ticket result;
            try {
                result = ticketRepository.save(ticket);
                LOG.debug("Ticket sauvegardé avec succès, ID: {}", result.getId());
            } catch (Exception e) {
                LOG.error("Erreur lors de la sauvegarde du ticket en base de données: {}", e.getMessage(), e);
                LOG.error("Type d'erreur: {}", e.getClass().getSimpleName());
                if (e.getCause() != null) {
                    LOG.error("Cause: {}", e.getCause().getMessage());
                }
                // Re-lancer l'exception pour qu'elle soit catchée par le bloc catch externe
                throw e;
            }

            // Envoyer email au client
            AppUser client = getAppUserByLogin(login);
            if (client != null) {
                try {
                    clientEmailService.sendTicketCreatedEmail(client, result);
                    LOG.info("Email de création de ticket envoyé au client: {}", client.getEmail());
                } catch (Exception e) {
                    LOG.error(
                        "Erreur lors de l'envoi de l'email de création de ticket au client {}: {}",
                        client.getEmail(),
                        e.getMessage()
                    );
                }
            }

            // Envoyer email admin (ne pas bloquer la création si l'email échoue)
            try {
                mailService.sendTicketCreatedEmail(result);
            } catch (Exception e) {
                LOG.error("Erreur lors de l'envoi de l'email de création de ticket à l'admin: {}", e.getMessage());
            }

            // Notifications pour la création de tickets (ne pas bloquer si les notifications échouent)
            try {
                notificationService.notifyClient(
                    login,
                    "Votre ticket #" + result.getId() + " a été créé avec succès",
                    "TICKET_CREATED",
                    result.getId(),
                    "/tickets/" + result.getId()
                );
            } catch (Exception e) {
                LOG.error("Erreur lors de la notification au client: {}", e.getMessage());
            }

            try {
                notificationService.notifyAdmins(
                    "Nouveau ticket créé par " + login + " - #" + result.getId(),
                    "TICKET_CREATED",
                    result.getId(),
                    "/admin/tickets/" + result.getId()
                );
            } catch (Exception e) {
                LOG.error("Erreur lors de la notification aux admins: {}", e.getMessage());
            }

            // Créer une activité pour la création du ticket (ne pas bloquer si l'activité échoue)
            try {
                activityIntegrationService.createTicketCreatedActivity(result);
            } catch (Exception e) {
                LOG.error("Erreur lors de la création de l'activité pour le ticket: {}", e.getMessage());
            }

            return ResponseEntity.created(new URI("/api/tickets/" + result.getId())).body(result);
        } catch (URISyntaxException e) {
            LOG.error("Erreur URI lors de la création du ticket: {}", e.getMessage(), e);
            return ResponseEntity.status(500).build();
        } catch (Exception e) {
            LOG.error("Erreur lors de la création du ticket avec image: {}", e.getMessage(), e);
            LOG.error("Stack trace complet:", e);
            // Logs détaillés pour le débogage en production
            LOG.error("Type d'erreur: {}", e.getClass().getSimpleName());
            LOG.error("Message d'erreur: {}", e.getMessage());
            if (e.getCause() != null) {
                LOG.error("Cause: {}", e.getCause().getMessage());
            }
            return ResponseEntity.status(500).build();
        }
    }

    // Créer un ticket (méthode existante pour compatibilité)
    @PostMapping(value = "", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity<Ticket> createTicket(@Valid @RequestBody Ticket ticket) throws URISyntaxException {
        try {
            String login = SecurityUtils.getCurrentUserLogin().orElse(null);
            if (login == null) return ResponseEntity.badRequest().build();
            ticket.setCreatedBy(login);
            ticket.setCreatedDate(java.time.Instant.now());
            Ticket result = ticketRepository.save(ticket);

            // Envoyer email au client
            AppUser client = getAppUserByLogin(login);
            if (client != null) {
                try {
                    clientEmailService.sendTicketCreatedEmail(client, result);
                    LOG.info("Email de création de ticket envoyé au client: {}", client.getEmail());
                } catch (Exception e) {
                    LOG.error(
                        "Erreur lors de l'envoi de l'email de création de ticket au client {}: {}",
                        client.getEmail(),
                        e.getMessage()
                    );
                }
            }

            // Envoyer email admin (ne pas bloquer la création si l'email échoue)
            try {
                mailService.sendTicketCreatedEmail(result);
            } catch (Exception e) {
                LOG.error("Erreur lors de l'envoi de l'email de création de ticket à l'admin: {}", e.getMessage());
            }

            // Notifications pour la création de tickets (ne pas bloquer si les notifications échouent)
            try {
                notificationService.notifyClient(
                    login,
                    "Votre ticket #" + result.getId() + " a été créé avec succès",
                    "TICKET_CREATED",
                    result.getId(),
                    "/tickets/" + result.getId()
                );
            } catch (Exception e) {
                LOG.error("Erreur lors de la notification au client: {}", e.getMessage());
            }

            try {
                notificationService.notifyAdmins(
                    "Nouveau ticket créé par " + login + " - #" + result.getId(),
                    "TICKET_CREATED",
                    result.getId(),
                    "/admin/tickets/" + result.getId()
                );
            } catch (Exception e) {
                LOG.error("Erreur lors de la notification aux admins: {}", e.getMessage());
            }

            // Créer une activité pour la création du ticket (ne pas bloquer si l'activité échoue)
            try {
                activityIntegrationService.createTicketCreatedActivity(result);
            } catch (Exception e) {
                LOG.error("Erreur lors de la création de l'activité pour le ticket: {}", e.getMessage());
            }

            return ResponseEntity.created(new URI("/api/tickets/" + result.getId())).body(result);
        } catch (URISyntaxException e) {
            LOG.error("Erreur URI lors de la création du ticket: {}", e.getMessage(), e);
            return ResponseEntity.status(500).build();
        } catch (Exception e) {
            LOG.error("Erreur lors de la création du ticket: {}", e.getMessage(), e);
            LOG.error("Stack trace complet:", e);
            // Logs détaillés pour le débogage en production
            LOG.error("Type d'erreur: {}", e.getClass().getSimpleName());
            LOG.error("Message d'erreur: {}", e.getMessage());
            if (e.getCause() != null) {
                LOG.error("Cause: {}", e.getCause().getMessage());
            }
            return ResponseEntity.status(500).build();
        }
    }

    // Mettre à jour le statut d'un ticket
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<Ticket> updateTicket(@PathVariable Long id, @Valid @RequestBody Ticket ticket) {
        LOG.info("Tentative de mise à jour du ticket {} avec le statut: {}", id, ticket.getStatus());
        String login = SecurityUtils.getCurrentUserLogin().orElse(null);
        if (login == null) return ResponseEntity.badRequest().build();
        Ticket existing = ticketRepository.findById(id).orElse(null);
        if (existing == null) return ResponseEntity.notFound().build();

        boolean isAdmin = SecurityContextHolder.getContext()
            .getAuthentication()
            .getAuthorities()
            .stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(auth -> auth.equals("ROLE_ADMIN") || auth.equals("ROLE_MANAGER"));

        if (!isAdmin && !existing.getCreatedBy().equals(login)) {
            return ResponseEntity.status(403).build();
        }

        // Sauvegarder l'ancien statut pour l'email
        String oldStatus = existing.getStatus();

        // Mettre à jour le statut
        existing.setStatus(ticket.getStatus());

        // Gérer l'ajout de nouveaux messages
        if (ticket.getMessageStrings() != null && !ticket.getMessageStrings().isEmpty()) {
            try {
                // Récupérer les messages existants
                List<TicketMessage> existingMessages = isAdmin
                    ? ticketMessageService.getTicketMessages(id)
                    : ticketMessageService.getPublicTicketMessages(id);

                // Ajouter les nouveaux messages qui ne sont pas déjà présents
                for (String messageString : ticket.getMessageStrings()) {
                    boolean messageExists = existingMessages
                        .stream()
                        .anyMatch(msg -> {
                            String existingString = msg.getAuthorType() == TicketMessage.AuthorType.CLIENT
                                ? "[CLIENT] " + msg.getContent()
                                : msg.getContent();
                            return existingString.equals(messageString);
                        });

                    if (!messageExists) {
                        // Ajouter le nouveau message
                        if (isAdmin) {
                            ticketMessageService.addAdminMessage(id, messageString);
                        } else {
                            ticketMessageService.addClientMessage(id, messageString);
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Erreur lors de l'ajout de messages pour le ticket " + id + ": " + e.getMessage());
            }
        }

        try {
            Ticket updated = ticketRepository.save(existing);
            LOG.info("Ticket {} mis à jour avec succès", updated.getId());

            // S'assurer que les messages ne sont pas chargés pour éviter les problèmes de sérialisation
            updated.setMessages(new ArrayList<>());

            // Envoyer email au client si le statut a changé
            if (!oldStatus.equals(updated.getStatus())) {
                // Créer une activité pour le changement de statut
                activityIntegrationService.createTicketStatusUpdatedActivity(updated, oldStatus, updated.getStatus());

                AppUser client = getAppUserByLogin(existing.getCreatedBy());
                if (client != null) {
                    try {
                        clientEmailService.sendTicketStatusChangedEmail(client, updated, oldStatus, updated.getStatus());
                        LOG.info("Email de changement de statut envoyé au client: {}", client.getEmail());
                    } catch (Exception e) {
                        LOG.error(
                            "Erreur lors de l'envoi de l'email de changement de statut au client {}: {}",
                            client.getEmail(),
                            e.getMessage()
                        );
                    }
                }
            }

            // Envoyer email de mise à jour générale
            AppUser client = getAppUserByLogin(existing.getCreatedBy());
            if (client != null) {
                try {
                    clientEmailService.sendTicketUpdatedEmail(client, updated);
                    LOG.info("Email de mise à jour de ticket envoyé au client: {}", client.getEmail());
                } catch (Exception e) {
                    LOG.error(
                        "Erreur lors de l'envoi de l'email de mise à jour de ticket au client {}: {}",
                        client.getEmail(),
                        e.getMessage()
                    );
                }
            }

            mailService.sendTicketUpdatedEmail(updated);

            // Notifications pour les mises à jour de tickets
            if (!existing.getCreatedBy().equals(login)) {
                // Admin met à jour le ticket -> notifier le client
                notificationService.notifyClient(
                    existing.getCreatedBy(),
                    "Votre ticket #" +
                    updated.getId() +
                    " a été mis à jour par " +
                    login +
                    (updated.getStatus() != null ? " - Statut: " + updated.getStatus() : ""),
                    "TICKET_UPDATED",
                    updated.getId(),
                    "/tickets/" + updated.getId()
                );

                // Notifier l'admin qui a fait la mise à jour
                notificationService.notifyUser(
                    login,
                    "Vous avez mis à jour le ticket #" + updated.getId() + " de " + existing.getCreatedBy(),
                    "TICKET_UPDATED",
                    updated.getId(),
                    "/admin/tickets/" + updated.getId(),
                    null
                );
            } else {
                // Client met à jour son propre ticket -> notifier les admins
                notificationService.notifyAdmins(
                    "Le client " +
                    login +
                    " a mis à jour son ticket #" +
                    updated.getId() +
                    (updated.getStatus() != null ? " - Nouveau statut: " + updated.getStatus() : ""),
                    "TICKET_UPDATED",
                    updated.getId(),
                    "/admin/tickets/" + updated.getId()
                );
            }
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            LOG.error("Erreur lors de la sauvegarde du ticket {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }

    // Ajouter un message à un ticket
    @PostMapping(value = "/{id}/messages", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity<Map<String, Object>> addMessage(@PathVariable Long id, @RequestBody Map<String, Object> requestBody) {
        String content;

        // Extraire le contenu du message depuis l'objet JSON
        if (requestBody.containsKey("content")) {
            content = requestBody.get("content").toString();
        } else if (requestBody.containsKey("message")) {
            content = requestBody.get("message").toString();
        } else {
            // Si c'est un string direct dans le JSON
            content = requestBody.toString();
        }
        LOG.info("Tentative d'ajout de message pour le ticket {} avec le contenu: {}", id, content);
        LOG.info("RequestBody original: {}", requestBody);

        try {
            String login = SecurityUtils.getCurrentUserLogin().orElse(null);
            if (login == null) {
                LOG.error("Utilisateur non connecté");
                return ResponseEntity.badRequest().build();
            }

            LOG.info("Utilisateur connecté: {}", login);

            // Vérifier que le ticket existe
            Ticket ticket = ticketRepository.findById(id).orElse(null);
            if (ticket == null) {
                LOG.error("Ticket non trouvé avec l'ID: {}", id);
                return ResponseEntity.notFound().build();
            }

            LOG.info("Ticket trouvé: {} créé par: {}", ticket.getId(), ticket.getCreatedBy());

            // Vérifier les permissions
            var authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();

            LOG.info("Autorités de l'utilisateur {}: {}", login, authorities);

            boolean isAdmin = authorities
                .stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(auth -> auth.equals("ROLE_ADMIN") || auth.equals("ROLE_MANAGER"));

            LOG.info("Utilisateur est admin: {}", isAdmin);

            LOG.info(
                "Vérification des permissions - isAdmin: {}, ticket.getCreatedBy(): {}, login: {}",
                isAdmin,
                ticket.getCreatedBy(),
                login
            );
            if (!isAdmin && !ticket.getCreatedBy().equals(login)) {
                LOG.error("Permission refusée pour l'utilisateur {} sur le ticket {}", login, id);
                return ResponseEntity.status(403).build();
            }

            // Vérifier que le contenu n'est pas vide
            if (content == null || content.trim().isEmpty()) {
                LOG.error("Contenu du message vide");
                return ResponseEntity.badRequest().build();
            }

            // Créer le message avec gestion d'erreur robuste
            try {
                TicketMessage.AuthorType authorType = isAdmin ? TicketMessage.AuthorType.ADMIN : TicketMessage.AuthorType.CLIENT;
                LOG.info("Création du message avec authorType: {}, content: {}", authorType, content.trim());

                // Créer le message avec tous les champs requis
                TicketMessage message = new TicketMessage();
                message.setTicket(ticket);
                message.setContent(content.trim());
                message.setAuthorType(authorType);
                message.setAuthorLogin(login);
                message.setCreatedDate(Instant.now());
                message.setInternal(false);

                LOG.info("Message créé, tentative de sauvegarde...");
                TicketMessage savedMessage = ticketMessageRepository.save(message);
                LOG.info("Message sauvegardé avec succès, ID: {}", savedMessage.getId());

                // Créer les notifications appropriées
                if (isAdmin) {
                    // Message de l'admin -> notifier le client
                    notificationService.notifyClient(
                        ticket.getCreatedBy(),
                        "Réponse reçue sur votre ticket #" + id + " : " + content.substring(0, Math.min(content.length(), 50)) + "...",
                        "MESSAGE_RECEIVED",
                        id,
                        "/tickets/" + id
                    );

                    // Envoyer email au client
                    AppUser client = getAppUserByLogin(ticket.getCreatedBy());
                    if (client != null) {
                        try {
                            // Créer un objet Message temporaire pour l'email
                            Message messageForEmail = new Message();
                            messageForEmail.setSender("Équipe devtechly");
                            messageForEmail.setContent(content.trim());
                            messageForEmail.setCreatedDate(Instant.now());

                            clientEmailService.sendTicketMessageEmail(client, ticket, messageForEmail);
                            LOG.info("Email de nouveau message envoyé au client: {}", client.getEmail());
                        } catch (Exception e) {
                            LOG.error(
                                "Erreur lors de l'envoi de l'email de nouveau message au client {}: {}",
                                client.getEmail(),
                                e.getMessage()
                            );
                        }
                    }
                } else {
                    // Message du client -> notifier les admins
                    notificationService.notifyAdmins(
                        "Nouveau message de " +
                        login +
                        " sur le ticket #" +
                        id +
                        " : " +
                        content.substring(0, Math.min(content.length(), 50)) +
                        "...",
                        "MESSAGE_RECEIVED",
                        id,
                        "/admin/tickets/" + id
                    );
                }

                // Retourner un DTO simple pour éviter les problèmes de lazy loading
                Map<String, Object> response = new HashMap<>();
                response.put("id", savedMessage.getId());
                response.put("ticketId", savedMessage.getTicketId());
                response.put("content", savedMessage.getContent());
                response.put("authorType", savedMessage.getAuthorType());
                response.put("authorLogin", savedMessage.getAuthorLogin());
                response.put("createdDate", savedMessage.getCreatedDate());
                response.put("success", true);

                return ResponseEntity.ok(response);
            } catch (Exception dbError) {
                LOG.error("Erreur lors de la sauvegarde du message en base de données: {}", dbError.getMessage(), dbError);

                // Vérifier si c'est un problème de table
                try {
                    // Test simple pour vérifier l'état de la table
                    long count = ticketMessageRepository.count();
                    LOG.info("Nombre total de messages en base: {}", count);
                } catch (Exception countError) {
                    LOG.error("Impossible de compter les messages - problème de table: {}", countError.getMessage());
                }

                return ResponseEntity.status(500).build();
            }
        } catch (Exception e) {
            LOG.error("Erreur lors de l'ajout du message pour le ticket {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }

    // Méthode privée pour sauvegarder l'image
    @Transactional(readOnly = true)
    private String saveImage(MultipartFile file) throws IOException {
        // Créer le dossier d'upload s'il n'existe pas
        Path uploadDir = Path.of(applicationProperties.getUpload().getPath());
        LOG.info("Chemin d'upload configuré: {}", uploadDir.toAbsolutePath());

        if (!Files.exists(uploadDir)) {
            try {
                Files.createDirectories(uploadDir);
                LOG.info("Dossier d'upload créé: {}", uploadDir.toAbsolutePath());
            } catch (IOException e) {
                LOG.error("Impossible de créer le dossier d'upload {}: {}", uploadDir.toAbsolutePath(), e.getMessage());
                throw new IOException("Impossible de créer le dossier d'upload: " + e.getMessage(), e);
            }
        }

        // Vérifier les permissions d'écriture
        if (!Files.isWritable(uploadDir)) {
            LOG.error("Le dossier d'upload n'est pas accessible en écriture: {}", uploadDir.toAbsolutePath());
            throw new IOException("Le dossier d'upload n'est pas accessible en écriture: " + uploadDir.toAbsolutePath());
        }

        // Générer un nom de fichier unique
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = "";
        if (originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String filename = UUID.randomUUID().toString() + fileExtension;

        // Sauvegarder le fichier
        Path filePath = uploadDir.resolve(filename);
        Files.copy(file.getInputStream(), filePath);

        // Retourner l'URL relative pour l'accès
        return "/api/tickets/images/" + filename;
    }

    // Endpoint pour servir les images
    @GetMapping("/images/{filename:.+}")
    public ResponseEntity<byte[]> getImage(@PathVariable String filename) {
        try {
            Path filePath = Path.of(applicationProperties.getUpload().getPath()).resolve(filename);
            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }

            byte[] imageBytes = Files.readAllBytes(filePath);
            String contentType = determineContentType(filename);

            return ResponseEntity.ok().contentType(org.springframework.http.MediaType.parseMediaType(contentType)).body(imageBytes);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Déterminer le type MIME basé sur l'extension
    private String determineContentType(String filename) {
        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            default -> "application/octet-stream";
        };
    }

    private boolean isAdmin(String login) {
        return SecurityContextHolder.getContext()
            .getAuthentication()
            .getAuthorities()
            .stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(auth -> auth.equals("ROLE_ADMIN") || auth.equals("ROLE_MANAGER"));
    }

    // Endpoints pour la gestion des devis
    @PostMapping("/devis")
    @Transactional
    public ResponseEntity<Map<String, Object>> createDevis(@RequestBody Map<String, Object> devisData) {
        try {
            String login = SecurityUtils.getCurrentUserLogin().orElse(null);
            if (login == null || !isAdmin(login)) {
                return ResponseEntity.status(403).build();
            }

            Long ticketId = Long.valueOf(devisData.get("ticketId").toString());
            Double amount = Double.valueOf(devisData.get("amount").toString());
            String description = (String) devisData.get("description");

            Ticket ticket = ticketRepository.findById(ticketId).orElse(null);
            if (ticket == null) {
                return ResponseEntity.notFound().build();
            }

            // Mettre à jour le ticket avec les informations du devis
            ticket.setPaymentAmount(amount);
            ticket.setPaymentCurrency("MAD");
            ticket.setPaymentStatus("PENDING_VALIDATION");
            ticket.setPaymentType("DEVIS");
            ticket.setDescription(ticket.getDescription() + "\n\n--- DEVIS ---\nMontant: " + amount + " MAD\nDescription: " + description);

            ticketRepository.save(ticket);

            // Envoyer email au client pour informer de l'envoi du devis
            AppUser client = getAppUserByLogin(ticket.getCreatedBy());
            if (client != null) {
                try {
                    // Créer un objet Devis temporaire pour l'email
                    Devis devisForEmail = new Devis();
                    devisForEmail.setId(ticketId); // Utiliser l'ID du ticket comme ID du devis temporaire
                    devisForEmail.setAmount(amount);
                    devisForEmail.setDescription(description);
                    devisForEmail.setDateValidation(Instant.now());

                    clientEmailService.sendQuoteSentEmail(client, devisForEmail);
                    LOG.info("Email de devis envoyé au client: {}", client.getEmail());
                } catch (Exception e) {
                    LOG.error("Erreur lors de l'envoi de l'email de devis au client {}: {}", client.getEmail(), e.getMessage());
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Devis créé avec succès");
            response.put("ticketId", ticketId);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            LOG.error("Erreur lors de la création du devis", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Erreur lors de la création du devis");
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/{id}/validate-devis")
    @Transactional
    public ResponseEntity<Map<String, Object>> validateDevis(@PathVariable Long id) {
        try {
            String login = SecurityUtils.getCurrentUserLogin().orElse(null);
            if (login == null) {
                return ResponseEntity.status(401).build();
            }

            Ticket ticket = ticketRepository.findById(id).orElse(null);
            if (ticket == null) {
                return ResponseEntity.notFound().build();
            }

            // Vérifier que l'utilisateur est le créateur du ticket ou un admin
            if (!ticket.getCreatedBy().equals(login) && !isAdmin(login)) {
                return ResponseEntity.status(403).build();
            }

            // Mettre à jour le statut du ticket
            ticket.setStatus("Devis validé");
            ticket.setPaymentStatus("VALIDATED");
            ticketRepository.save(ticket);

            // Envoyer email au client
            AppUser client = getAppUserByLogin(ticket.getCreatedBy());
            if (client != null) {
                try {
                    // Créer un objet Devis temporaire pour l'email
                    Devis devisForEmail = new Devis();
                    devisForEmail.setId(id);
                    devisForEmail.setAmount(ticket.getPaymentAmount() != null ? ticket.getPaymentAmount() : 0.0);
                    devisForEmail.setDescription("Devis validé pour le ticket #" + id);
                    devisForEmail.setDateValidation(Instant.now());

                    clientEmailService.sendQuoteValidatedEmail(client, devisForEmail);
                    LOG.info("Email de validation de devis envoyé au client: {}", client.getEmail());
                } catch (Exception e) {
                    LOG.error(
                        "Erreur lors de l'envoi de l'email de validation de devis au client {}: {}",
                        client.getEmail(),
                        e.getMessage()
                    );
                }
            }

            // Envoyer une notification
            notificationService.notifyUser(ticket.getCreatedBy(), "Votre devis pour le ticket #" + id + " a été validé", "DEVIS_VALIDATED");

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Devis validé avec succès");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            LOG.error("Erreur lors de la validation du devis", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Erreur lors de la validation du devis");
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Endpoints pour la validation des paiements
    @PutMapping("/{id}/validate-payment")
    @Transactional
    public ResponseEntity<Map<String, Object>> validatePayment(@PathVariable Long id) {
        try {
            String login = SecurityUtils.getCurrentUserLogin().orElse(null);
            if (login == null || !isAdmin(login)) {
                return ResponseEntity.status(403).build();
            }

            Ticket ticket = ticketRepository.findById(id).orElse(null);
            if (ticket == null) {
                return ResponseEntity.notFound().build();
            }

            // Sauvegarder l'ancien statut pour l'activité
            String oldStatus = ticket.getStatus();

            // Mettre à jour le statut du ticket
            ticket.setStatus("Paiement validé");
            ticket.setPaymentStatus("COMPLETED");
            ticketRepository.save(ticket);

            // Créer une activité pour le paiement validé
            String amount = ticket.getPaymentAmount() != null
                ? ticket.getPaymentAmount() + " " + (ticket.getPaymentCurrency() != null ? ticket.getPaymentCurrency() : "MAD")
                : "montant inconnu";
            activityIntegrationService.createPaymentActivity(
                id,
                ticket.getPaymentType() != null ? ticket.getPaymentType() : "En ligne",
                amount,
                true
            );

            // Créer aussi une activité pour le changement de statut
            if (!oldStatus.equals("Paiement validé")) {
                activityIntegrationService.createTicketStatusUpdatedActivity(ticket, oldStatus, "Paiement validé");
            }

            // Envoyer email au client
            AppUser client = getAppUserByLogin(ticket.getCreatedBy());
            if (client != null) {
                try {
                    // Créer un objet Paiement temporaire pour l'email
                    Paiement paiementForEmail = new Paiement();
                    paiementForEmail.setId(id);
                    paiementForEmail.setAmount(ticket.getPaymentAmount() != null ? ticket.getPaymentAmount() : 0.0);
                    paiementForEmail.setMethodePaiement("En ligne");
                    paiementForEmail.setDatePaiement(Instant.now());

                    clientEmailService.sendPaymentValidatedEmail(client, paiementForEmail);
                    LOG.info("Email de validation de paiement envoyé au client: {}", client.getEmail());
                } catch (Exception e) {
                    LOG.error(
                        "Erreur lors de l'envoi de l'email de validation de paiement au client {}: {}",
                        client.getEmail(),
                        e.getMessage()
                    );
                }
            }

            // Envoyer une notification
            notificationService.notifyUser(
                ticket.getCreatedBy(),
                "Le paiement pour votre ticket #" + id + " a été validé",
                "PAYMENT_VALIDATED"
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Paiement validé avec succès");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            LOG.error("Erreur lors de la validation du paiement", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Erreur lors de la validation du paiement");
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Endpoint pour fermer un ticket
    @PutMapping("/{id}/close")
    @Transactional
    public ResponseEntity<Map<String, Object>> closeTicket(@PathVariable Long id) {
        try {
            String login = SecurityUtils.getCurrentUserLogin().orElse(null);
            if (login == null) {
                return ResponseEntity.status(401).build();
            }

            Ticket ticket = ticketRepository.findById(id).orElse(null);
            if (ticket == null) {
                return ResponseEntity.notFound().build();
            }

            // Vérifier que l'utilisateur est le créateur du ticket ou un admin
            if (!ticket.getCreatedBy().equals(login) && !isAdmin(login)) {
                return ResponseEntity.status(403).build();
            }

            // Vérifier que le ticket peut être fermé
            if (!"Paiement validé".equals(ticket.getStatus())) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Le ticket ne peut être fermé que si le paiement est validé");
                return ResponseEntity.badRequest().body(response);
            }

            // Mettre à jour le statut du ticket
            ticket.setStatus("Fermé");
            ticketRepository.save(ticket);

            // Envoyer une notification
            notificationService.notifyUser(ticket.getCreatedBy(), "Votre ticket #" + id + " a été fermé", "TICKET_CLOSED");

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Ticket fermé avec succès");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            LOG.error("Erreur lors de la fermeture du ticket", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Erreur lors de la fermeture du ticket");
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Endpoint de diagnostic pour vérifier l'état de la base de données
    @GetMapping("/diagnostic")
    public ResponseEntity<Map<String, Object>> diagnostic() {
        Map<String, Object> diagnostic = new HashMap<>();

        try {
            // Vérifier la connexion à la base de données
            diagnostic.put("databaseConnected", true);

            // Compter les tickets
            long ticketCount = ticketRepository.count();
            diagnostic.put("ticketCount", ticketCount);

            // Compter les messages
            try {
                long messageCount = ticketMessageRepository.count();
                diagnostic.put("messageCount", messageCount);
                diagnostic.put("messageTableExists", true);
            } catch (Exception e) {
                diagnostic.put("messageCount", 0);
                diagnostic.put("messageTableExists", false);
                diagnostic.put("messageTableError", e.getMessage());
            }

            // Vérifier les permissions de l'utilisateur
            String login = SecurityUtils.getCurrentUserLogin().orElse("non connecté");
            diagnostic.put("currentUser", login);

            var authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();

            boolean isAdmin = authorities
                .stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(auth -> auth.equals("ROLE_ADMIN") || auth.equals("ROLE_MANAGER"));

            diagnostic.put("isAdmin", isAdmin);
            diagnostic.put("authorities", authorities.stream().map(GrantedAuthority::getAuthority).toList());

            return ResponseEntity.ok(diagnostic);
        } catch (Exception e) {
            diagnostic.put("databaseConnected", false);
            diagnostic.put("error", e.getMessage());
            return ResponseEntity.ok(diagnostic);
        }
    }

    // Endpoint de test pour vérifier les permissions
    @GetMapping("/test-permissions")
    public ResponseEntity<Map<String, Object>> testPermissions() {
        try {
            String login = SecurityUtils.getCurrentUserLogin().orElse(null);
            if (login == null) {
                return ResponseEntity.status(401).build();
            }

            var authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();

            boolean isAdmin = authorities
                .stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(auth -> auth.equals("ROLE_ADMIN") || auth.equals("ROLE_MANAGER"));

            Map<String, Object> response = new HashMap<>();
            response.put("login", login);
            response.put("authorities", authorities.stream().map(GrantedAuthority::getAuthority).toList());
            response.put("isAdmin", isAdmin);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            LOG.error("Erreur lors du test des permissions", e);
            return ResponseEntity.status(500).build();
        }
    }

    // Endpoint pour accepter un devis (par le client)
    @PutMapping("/{id}/accept-devis")
    @Transactional
    public ResponseEntity<Map<String, Object>> acceptDevis(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        try {
            String login = SecurityUtils.getCurrentUserLogin().orElse(null);
            if (login == null) {
                return ResponseEntity.status(401).build();
            }

            Ticket ticket = ticketRepository.findById(id).orElse(null);
            if (ticket == null) {
                return ResponseEntity.notFound().build();
            }

            // Vérifier que l'utilisateur est le créateur du ticket
            if (!ticket.getCreatedBy().equals(login)) {
                return ResponseEntity.status(403).build();
            }

            // Vérifier que le ticket est au bon statut
            if (!"Nouveau".equals(ticket.getStatus())) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Le devis ne peut être accepté que si le ticket est au statut 'Nouveau'");
                return ResponseEntity.badRequest().body(response);
            }

            // Extraire les informations du devis
            Double amount = request.get("amount") != null ? Double.valueOf(request.get("amount").toString()) : null;
            String description = request.get("description") != null ? request.get("description").toString() : "";
            Integer devisIndex = request.get("devisIndex") != null ? Integer.valueOf(request.get("devisIndex").toString()) : null;

            // Mettre à jour le statut du ticket
            ticket.setStatus("Devis validé");
            ticket.setPaymentStatus("PENDING");

            // Ajouter un message de confirmation
            String confirmationMessage = String.format(
                "J'accepte le devis #%d d'un montant de %.2f MAD. %s",
                devisIndex != null ? devisIndex + 1 : 1,
                amount,
                description
            );
            ticketMessageService.addMessage(id, confirmationMessage, TicketMessage.AuthorType.CLIENT, login);

            ticketRepository.save(ticket);

            // Envoyer une notification à l'admin
            notificationService.notifyUser("admin", "Le client a accepté le devis pour le ticket #" + id, "DEVIS_ACCEPTED");

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Devis accepté avec succès");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            LOG.error("Erreur lors de l'acceptation du devis", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Erreur lors de l'acceptation du devis");
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Endpoint pour refuser un devis (par le client)
    @PutMapping("/{id}/reject-devis")
    @Transactional
    public ResponseEntity<Map<String, Object>> rejectDevis(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        try {
            String login = SecurityUtils.getCurrentUserLogin().orElse(null);
            if (login == null) {
                return ResponseEntity.status(401).build();
            }

            Ticket ticket = ticketRepository.findById(id).orElse(null);
            if (ticket == null) {
                return ResponseEntity.notFound().build();
            }

            // Vérifier que l'utilisateur est le créateur du ticket
            if (!ticket.getCreatedBy().equals(login)) {
                return ResponseEntity.status(403).build();
            }

            // Vérifier que le ticket est au bon statut
            if (!"Nouveau".equals(ticket.getStatus())) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Le devis ne peut être refusé que si le ticket est au statut 'Nouveau'");
                return ResponseEntity.badRequest().body(response);
            }

            // Extraire les informations du devis
            Double amount = request.get("amount") != null ? Double.valueOf(request.get("amount").toString()) : null;
            String description = request.get("description") != null ? request.get("description").toString() : "";
            Integer devisIndex = request.get("devisIndex") != null ? Integer.valueOf(request.get("devisIndex").toString()) : null;

            // Ajouter un message de refus
            String rejectionMessage = String.format(
                "Je refuse le devis #%d d'un montant de %.2f MAD. %s",
                devisIndex != null ? devisIndex + 1 : 1,
                amount,
                description
            );
            ticketMessageService.addMessage(id, rejectionMessage, TicketMessage.AuthorType.CLIENT, login);

            ticketRepository.save(ticket);

            // Envoyer une notification à l'admin
            notificationService.notifyUser("admin", "Le client a refusé le devis pour le ticket #" + id, "DEVIS_REJECTED");

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Devis refusé avec succès");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            LOG.error("Erreur lors du refus du devis", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Erreur lors du refus du devis");
            return ResponseEntity.badRequest().body(response);
        }
    }

    // DTO pour éviter les problèmes de sérialisation avec les collections lazy
    public static class TicketDTO {

        public Long id;
        public String type;
        public String description;
        public String backofficeUrl;
        public String backofficeLogin;
        public String backofficePassword;
        public String hostingUrl;
        public String imageUrl;
        public String status;
        public String createdBy;
        public String clientName;
        public Instant createdDate;
        public List<String> messageStrings;

        public TicketDTO(Ticket ticket, UserRepository userRepository) {
            this.id = ticket.getId();
            this.type = ticket.getType();
            this.description = ticket.getDescription();
            this.backofficeUrl = ticket.getBackofficeUrl();
            this.backofficeLogin = ticket.getBackofficeLogin();
            this.backofficePassword = ticket.getBackofficePassword();
            this.hostingUrl = ticket.getHostingUrl();
            this.imageUrl = ticket.getImageUrl();
            this.status = ticket.getStatus();
            this.createdBy = ticket.getCreatedBy();
            this.createdDate = ticket.getCreatedDate();
            this.messageStrings = ticket.getMessageStrings();

            // Récupérer le nom complet du client
            if (ticket.getCreatedBy() != null) {
                try {
                    var user = userRepository.findOneByLogin(ticket.getCreatedBy());
                    if (user.isPresent()) {
                        var u = user.orElseThrow();
                        if (u.getFirstName() != null && u.getLastName() != null) {
                            this.clientName = u.getFirstName() + " " + u.getLastName();
                        } else if (u.getFirstName() != null) {
                            this.clientName = u.getFirstName();
                        } else if (u.getLastName() != null) {
                            this.clientName = u.getLastName();
                        } else {
                            this.clientName = u.getLogin();
                        }
                    } else {
                        this.clientName = ticket.getCreatedBy();
                    }
                } catch (Exception e) {
                    this.clientName = ticket.getCreatedBy();
                }
            } else {
                this.clientName = "Client inconnu";
            }
        }
    }
}
