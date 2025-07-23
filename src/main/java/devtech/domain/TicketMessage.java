package devtech.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "ticket_message")
public class TicketMessage implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "author_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private AuthorType authorType;

    @Column(name = "author_login", nullable = false)
    private String authorLogin;

    @Column(name = "created_date", nullable = false)
    private Instant createdDate = Instant.now();

    @Column(name = "is_internal", nullable = false)
    private boolean isInternal = false;

    public enum AuthorType {
        CLIENT,
        ADMIN,
        MANAGER,
    }

    // Constructeurs
    public TicketMessage() {}

    public TicketMessage(Ticket ticket, String content, AuthorType authorType, String authorLogin) {
        this.ticket = ticket;
        this.content = content;
        this.authorType = authorType;
        this.authorLogin = authorLogin;
    }

    // Getters et setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Ticket getTicket() {
        return ticket;
    }

    public void setTicket(Ticket ticket) {
        this.ticket = ticket;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public AuthorType getAuthorType() {
        return authorType;
    }

    public void setAuthorType(AuthorType authorType) {
        this.authorType = authorType;
    }

    public String getAuthorLogin() {
        return authorLogin;
    }

    public void setAuthorLogin(String authorLogin) {
        this.authorLogin = authorLogin;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    public boolean isInternal() {
        return isInternal;
    }

    public void setInternal(boolean internal) {
        isInternal = internal;
    }

    // Méthodes utilitaires
    public boolean isFromClient() {
        return authorType == AuthorType.CLIENT;
    }

    public boolean isFromAdmin() {
        return authorType == AuthorType.ADMIN || authorType == AuthorType.MANAGER;
    }

    public String getAuthorDisplayName() {
        switch (authorType) {
            case CLIENT:
                return "Vous";
            case ADMIN:
                return "Équipe DevTech";
            case MANAGER:
                return "Manager DevTech";
            default:
                return authorLogin;
        }
    }
}
