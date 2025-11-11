package devtech.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
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

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "status", nullable = false)
    private String status = "Nouveau";

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "created_date", nullable = false)
    private Instant createdDate = Instant.now();

    @Column(name = "last_modified_date")
    private Instant lastModifiedDate = Instant.now();

    // Champs de paiement
    @Column(name = "payment_required")
    private Boolean paymentRequired = false;

    @Column(name = "payment_amount")
    private Double paymentAmount;

    @Column(name = "payment_currency", length = 3)
    private String paymentCurrency = "MAD";

    @Column(name = "payment_status", length = 20)
    private String paymentStatus = "PENDING";

    @Column(name = "payment_type", length = 50)
    private String paymentType; // "TICKET_CREATION", "TICKET_UPGRADE", "PRIORITY_ACCESS"

    @Column(name = "paiement_id")
    private Long paiementId;

    @JsonIgnore
    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TicketMessage> messages = new ArrayList<>();

    // Champ temporaire pour la compatibilité avec le frontend
    @Transient
    private List<String> messageStrings = new ArrayList<>();

    // Méthode pour éviter l'accès aux messages lazy-loaded lors de la sérialisation
    @JsonIgnore
    public List<TicketMessage> getMessagesForSerialization() {
        return null; // Retourne null pour éviter la sérialisation
    }

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

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
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

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(Instant lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    // Getters et setters pour les champs de paiement
    public Boolean getPaymentRequired() {
        return paymentRequired;
    }

    public void setPaymentRequired(Boolean paymentRequired) {
        this.paymentRequired = paymentRequired;
    }

    public Double getPaymentAmount() {
        return paymentAmount;
    }

    public void setPaymentAmount(Double paymentAmount) {
        this.paymentAmount = paymentAmount;
    }

    public String getPaymentCurrency() {
        return paymentCurrency;
    }

    public void setPaymentCurrency(String paymentCurrency) {
        this.paymentCurrency = paymentCurrency;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }

    public Long getPaiementId() {
        return paiementId;
    }

    public void setPaiementId(Long paiementId) {
        this.paiementId = paiementId;
    }

    @JsonIgnore
    public List<TicketMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<TicketMessage> messages) {
        this.messages = messages;
    }

    // Méthode pour accéder aux messages de manière sécurisée
    public List<TicketMessage> getMessagesSafely() {
        return messages;
    }

    public List<String> getMessageStrings() {
        return messageStrings;
    }

    public void setMessageStrings(List<String> messageStrings) {
        this.messageStrings = messageStrings;
    }

    // Méthodes utilitaires
    public void addMessage(TicketMessage message) {
        this.messages.add(message);
        message.setTicket(this);
    }

    public List<TicketMessage> getPublicMessages() {
        return this.messages.stream().filter(message -> !message.isInternal()).toList();
    }

    public long getMessageCount() {
        return this.messages.size();
    }

    public long getPublicMessageCount() {
        return this.messages.stream().filter(message -> !message.isInternal()).count();
    }
}
