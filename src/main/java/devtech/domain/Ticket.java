package devtech.domain;

import devtech.domain.util.StringListConverter;
import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "ticket")
public class Ticket implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "backoffice_url")
    private String backofficeUrl;

    @Column(name = "backoffice_login")
    private String backofficeLogin;

    @Column(name = "backoffice_password")
    private String backofficePassword;

    @Column(name = "hosting_url")
    private String hostingUrl;

    @Column(name = "status", nullable = false)
    private String status = "Nouveau";

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "created_date", nullable = false)
    private Instant createdDate = Instant.now();

    @Column(name = "messages", columnDefinition = "json")
    @Convert(converter = StringListConverter.class)
    private List<String> messages;

    // Getters et setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBackofficeUrl() {
        return backofficeUrl;
    }

    public void setBackofficeUrl(String backofficeUrl) {
        this.backofficeUrl = backofficeUrl;
    }

    public String getBackofficeLogin() {
        return backofficeLogin;
    }

    public void setBackofficeLogin(String backofficeLogin) {
        this.backofficeLogin = backofficeLogin;
    }

    public String getBackofficePassword() {
        return backofficePassword;
    }

    public void setBackofficePassword(String backofficePassword) {
        this.backofficePassword = backofficePassword;
    }

    public String getHostingUrl() {
        return hostingUrl;
    }

    public void setHostingUrl(String hostingUrl) {
        this.hostingUrl = hostingUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    public List<String> getMessages() {
        return messages;
    }

    public void setMessages(List<String> messages) {
        this.messages = messages;
    }
}
