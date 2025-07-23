package devtech.web.rest;

import devtech.config.ApplicationProperties;
import devtech.domain.Ticket;
import devtech.domain.TicketMessage;
import devtech.repository.TicketMessageRepository;
import devtech.repository.TicketRepository;
import devtech.security.SecurityUtils;
import devtech.service.MailService;
import devtech.service.NotificationService;
import devtech.service.TicketMessageService;
import jakarta.validation.Valid;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/tickets")
@Transactional
public class TicketResource {

    private static final Logger LOG = LoggerFactory.getLogger(TicketResource.class);

    private final TicketRepository ticketRepository;
    private final MailService mailService;
    private final NotificationService notificationService;
    private final TicketMessageService ticketMessageService;
    private final ApplicationProperties applicationProperties;
    private final TicketMessageRepository ticketMessageRepository;

    public TicketResource(
        TicketRepository ticketRepository,
        MailService mailService,
        NotificationService notificationService,
        TicketMessageService ticketMessageService,
        ApplicationProperties applicationProperties,
        TicketMessageRepository ticketMessageRepository
    ) {
        this.ticketRepository = ticketRepository;
        this.mailService = mailService;
        this.notificationService = notificationService;
        this.ticketMessageService = ticketMessageService;
        this.applicationProperties = applicationProperties;
        this.ticketMessageRepository = ticketMessageRepository;
    }

    // Lister les tickets de l'utilisateur connecté
    @GetMapping("")
    public ResponseEntity<List<TicketDTO>> getMyTickets() {
        try {
            String login = SecurityUtils.getCurrentUserLogin().orElse(null);
            if (login == null) return ResponseEntity.ok(List.of());

            boolean isAdmin = SecurityContextHolder.getContext()
                .getAuthentication()
                .getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(auth -> auth.equals("ROLE_ADMIN") || auth.equals("ROLE_MANAGER"));

            List<Ticket> tickets;
            if (isAdmin) {
                tickets = ticketRepository.findAll();
            } else {
                tickets = ticketRepository.findByCreatedBy(login);
            }

            // Convertir en DTOs pour éviter les problèmes de sérialisation
            List<TicketDTO> ticketDTOs = new ArrayList<>();
            for (Ticket ticket : tickets) {
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

                    ticketDTOs.add(new TicketDTO(ticket));
                } catch (Exception e) {
                    // Si il y a une erreur avec les messages, on continue sans messages
                    System.err.println("Erreur lors du chargement des messages pour le ticket " + ticket.getId() + ": " + e.getMessage());
                    ticket.setMessageStrings(new ArrayList<>());
                    ticketDTOs.add(new TicketDTO(ticket));
                }
            }

            return ResponseEntity.ok(ticketDTOs);
        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération des tickets: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    // Méthode utilitaire pour convertir les TicketMessage en chaînes
    private List<String> convertMessagesToStrings(List<TicketMessage> messages) {
        if (messages == null) return List.of();
        return messages
            .stream()
            .map(message -> {
                String prefix = message.getAuthorType() == TicketMessage.AuthorType.CLIENT ? "[CLIENT] " : "";
                return prefix + message.getContent();
            })
            .toList();
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

            return ResponseEntity.ok(new TicketDTO(ticket));
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

            // Charger les messages selon le rôle
            try {
                List<TicketMessage> messages;
                if (isAdmin) {
                    messages = ticketMessageService.getTicketMessages(id);
                } else {
                    messages = ticketMessageService.getPublicTicketMessages(id);
                }

                List<String> messageStrings = convertMessagesToStrings(messages);
                return ResponseEntity.ok(messageStrings);
            } catch (Exception e) {
                System.err.println("Erreur lors du chargement des messages pour le ticket " + id + ": " + e.getMessage());
                return ResponseEntity.ok(List.of());
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération des messages du ticket " + id + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    // Créer un ticket avec upload d'image
    @PostMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Ticket> createTicketWithImage(
        @RequestParam("type") String type,
        @RequestParam("description") String description,
        @RequestParam(value = "backofficeUrl", required = false) String backofficeUrl,
        @RequestParam(value = "backofficeLogin", required = false) String backofficeLogin,
        @RequestParam(value = "backofficePassword", required = false) String backofficePassword,
        @RequestParam(value = "hostingUrl", required = false) String hostingUrl,
        @RequestParam(value = "image", required = false) MultipartFile image
    ) throws URISyntaxException {
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
                return ResponseEntity.badRequest().build();
            }
        }

        Ticket result = ticketRepository.save(ticket);
        mailService.sendTicketCreatedEmail(result);
        notificationService.notifyUser(login, "Votre ticket a été créé (ID: " + result.getId() + ")", "TICKET_CREATED");
        notificationService.notifyUser("admin", "Nouveau ticket créé par " + login + " (ID: " + result.getId() + ")", "TICKET_CREATED");
        return ResponseEntity.created(new URI("/api/tickets/" + result.getId())).body(result);
    }

    // Créer un ticket (méthode existante pour compatibilité)
    @PostMapping(value = "", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Ticket> createTicket(@Valid @RequestBody Ticket ticket) throws URISyntaxException {
        String login = SecurityUtils.getCurrentUserLogin().orElse(null);
        if (login == null) return ResponseEntity.badRequest().build();
        ticket.setCreatedBy(login);
        ticket.setCreatedDate(java.time.Instant.now());
        Ticket result = ticketRepository.save(ticket);
        mailService.sendTicketCreatedEmail(result);
        notificationService.notifyUser(login, "Votre ticket a été créé (ID: " + result.getId() + ")", "TICKET_CREATED");
        notificationService.notifyUser("admin", "Nouveau ticket créé par " + login + " (ID: " + result.getId() + ")", "TICKET_CREATED");
        return ResponseEntity.created(new URI("/api/tickets/" + result.getId())).body(result);
    }

    // Mettre à jour le statut d'un ticket
    @PutMapping("/{id}")
    public ResponseEntity<Ticket> updateTicket(@PathVariable Long id, @Valid @RequestBody Ticket ticket) {
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

        Ticket updated = ticketRepository.save(existing);
        mailService.sendTicketUpdatedEmail(updated);
        notificationService.notifyUser(
            existing.getCreatedBy(),
            "Votre ticket (ID: " + updated.getId() + ") a été mis à jour.",
            "TICKET_UPDATED"
        );
        if (!existing.getCreatedBy().equals(login)) {
            notificationService.notifyUser(login, "Vous avez mis à jour le ticket (ID: " + updated.getId() + ")", "TICKET_UPDATED");
        }
        return ResponseEntity.ok(updated);
    }

    // Ajouter un message à un ticket
    @PostMapping("/{id}/messages")
    public ResponseEntity<TicketMessage> addMessage(@PathVariable Long id, @RequestBody MessageRequest request) {
        LOG.info("Tentative d'ajout de message pour le ticket {} avec le contenu: {}", id, request.content);

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

            LOG.info("Ticket trouvé: {}", ticket.getId());

            // Vérifier les permissions
            boolean isAdmin = SecurityContextHolder.getContext()
                .getAuthentication()
                .getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(auth -> auth.equals("ROLE_ADMIN") || auth.equals("ROLE_MANAGER"));

            LOG.info("Utilisateur est admin: {}", isAdmin);

            if (!isAdmin && !ticket.getCreatedBy().equals(login)) {
                LOG.error("Permission refusée pour l'utilisateur {} sur le ticket {}", login, id);
                return ResponseEntity.status(403).build();
            }

            TicketMessage message;
            if (isAdmin) {
                LOG.info("Ajout d'un message admin");
                message = ticketMessageService.addAdminMessage(id, request.content);
            } else {
                LOG.info("Ajout d'un message client");
                message = ticketMessageService.addClientMessage(id, request.content);
            }

            LOG.info("Message ajouté avec succès, ID: {}", message.getId());
            return ResponseEntity.ok(message);
        } catch (Exception e) {
            LOG.error("Erreur lors de l'ajout du message pour le ticket {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }

    // Endpoint de diagnostic complet pour vérifier l'état de la base de données
    @GetMapping("/diagnostic")
    public ResponseEntity<String> diagnostic() {
        try {
            LOG.info("=== DIAGNOSTIC DE LA BASE DE DONNÉES ===");

            StringBuilder result = new StringBuilder();
            result.append("=== DIAGNOSTIC DE LA BASE DE DONNÉES ===\n");

            // Vérifier la table ticket
            long ticketCount = ticketRepository.count();
            LOG.info("Nombre de tickets: {}", ticketCount);
            result.append("Nombre de tickets: ").append(ticketCount).append("\n");

            // Vérifier la table ticket_message
            long messageCount = ticketMessageRepository.count();
            LOG.info("Nombre de messages: {}", messageCount);
            result.append("Nombre de messages: ").append(messageCount).append("\n");

            // Vérifier les tickets existants
            List<Ticket> tickets = ticketRepository.findAll();
            result.append("Tickets existants:\n");
            for (Ticket ticket : tickets) {
                result
                    .append("- Ticket ID: ")
                    .append(ticket.getId())
                    .append(", Type: ")
                    .append(ticket.getType())
                    .append(", Créé par: ")
                    .append(ticket.getCreatedBy())
                    .append("\n");
            }

            // Vérifier les messages existants
            List<TicketMessage> messages = ticketMessageRepository.findAll();
            result.append("Messages existants:\n");
            for (TicketMessage message : messages) {
                result
                    .append("- Message ID: ")
                    .append(message.getId())
                    .append(", Ticket ID: ")
                    .append(message.getTicket().getId())
                    .append(", Contenu: ")
                    .append(message.getContent().substring(0, Math.min(50, message.getContent().length())))
                    .append("...\n");
            }

            LOG.info("Diagnostic terminé avec succès");
            return ResponseEntity.ok(result.toString());
        } catch (Exception e) {
            LOG.error("Erreur lors du diagnostic: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Erreur lors du diagnostic: " + e.getMessage());
        }
    }

    // Endpoint de test simple pour vérifier la base de données
    @GetMapping("/health-check")
    public ResponseEntity<String> healthCheck() {
        try {
            LOG.info("=== HEALTH CHECK ===");

            StringBuilder result = new StringBuilder();
            result.append("=== HEALTH CHECK ===\n");

            // Test de connexion à la base de données
            result.append("1. Test de connexion à la base de données: ");
            try {
                long ticketCount = ticketRepository.count();
                result.append("OK (tickets: ").append(ticketCount).append(")\n");
            } catch (Exception e) {
                result.append("ERREUR: ").append(e.getMessage()).append("\n");
                return ResponseEntity.status(500).body(result.toString());
            }

            // Test de la table ticket_message
            result.append("2. Test de la table ticket_message: ");
            try {
                long messageCount = ticketMessageRepository.count();
                result.append("OK (messages: ").append(messageCount).append(")\n");
            } catch (Exception e) {
                result.append("ERREUR: ").append(e.getMessage()).append("\n");
                return ResponseEntity.status(500).body(result.toString());
            }

            // Test de création d'un message (sans sauvegarder)
            result.append("3. Test de création d'un message: ");
            try {
                TicketMessage testMessage = new TicketMessage();
                testMessage.setContent("Test message");
                testMessage.setAuthorType(TicketMessage.AuthorType.CLIENT);
                testMessage.setAuthorLogin("test");
                result.append("OK\n");
            } catch (Exception e) {
                result.append("ERREUR: ").append(e.getMessage()).append("\n");
                return ResponseEntity.status(500).body(result.toString());
            }

            result.append("=== HEALTH CHECK TERMINÉ ===\n");
            LOG.info("Health check terminé avec succès");
            return ResponseEntity.ok(result.toString());
        } catch (Exception e) {
            LOG.error("Erreur lors du health check: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Erreur lors du health check: " + e.getMessage());
        }
    }

    // Endpoint de test simple sans authentification
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        LOG.info("Test endpoint appelé");
        return ResponseEntity.ok("API fonctionne correctement!");
    }

    // Endpoint de test pour vérifier la table ticket_message
    @GetMapping("/test-messages-table")
    public ResponseEntity<String> testMessagesTable() {
        try {
            LOG.info("Test de la table ticket_message");

            // Compter le nombre total de messages
            long count = ticketMessageRepository.count();
            LOG.info("Nombre total de messages dans la table: {}", count);

            // Essayer de récupérer tous les messages
            List<TicketMessage> allMessages = ticketMessageRepository.findAll();
            LOG.info("Nombre de messages récupérés: {}", allMessages.size());

            return ResponseEntity.ok("Table ticket_message fonctionne correctement. Nombre de messages: " + count);
        } catch (Exception e) {
            LOG.error("Erreur lors du test de la table ticket_message: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Erreur: " + e.getMessage());
        }
    }

    // Ajouter des messages de test (endpoint temporaire)
    @PostMapping("/{id}/add-test-message")
    public ResponseEntity<String> addTestMessage(@PathVariable Long id) {
        try {
            // Ajouter un message de test simple
            ticketMessageService.addAdminMessage(id, "Bonjour ! Nous avons bien reçu votre ticket et nous travaillons dessus.");
            return ResponseEntity.ok("Message de test ajouté");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erreur: " + e.getMessage());
        }
    }

    // Méthode privée pour sauvegarder l'image
    private String saveImage(MultipartFile file) throws IOException {
        // Créer le dossier d'upload s'il n'existe pas
        Path uploadDir = Paths.get(applicationProperties.getUpload().getPath());
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
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
            Path filePath = Paths.get(applicationProperties.getUpload().getPath()).resolve(filename);
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

    // Classe pour les requêtes de messages
    public static class MessageRequest {

        public String content;
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
        public Instant createdDate;
        public List<String> messageStrings;

        public TicketDTO(Ticket ticket) {
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
        }
    }
}
