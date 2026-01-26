package devtechly.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;

/**
 * A Paiement entity for payments.
 */
@Entity
@Table(name = "paiement")
public class Paiement implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "amount")
    private Double amount;

    @Column(name = "methode_paiement")
    private String methodePaiement;

    @Column(name = "date_paiement")
    private Instant datePaiement;

    @Column(name = "ticket_id")
    private Long ticketId;

    @Column(name = "status")
    private String status = "PENDING";

    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "user_email")
    private String user;

    @Column(name = "date")
    private java.time.LocalDate date;

    @Column(name = "description")
    private String description;

    @Column(name = "client_ip")
    private String clientIp;

    @Column(name = "currency")
    private String currency;

    @Column(name = "cmi_merchant_id")
    private String cmiMerchantId;

    // Champs CMI supplémentaires
    @Column(name = "cmi_order_id")
    private String cmiOrderId;

    @Column(name = "cmi_transaction_id")
    private String cmiTransactionId;

    @Column(name = "cmi_response_code")
    private String cmiResponseCode;

    @Column(name = "cmi_response_message")
    private String cmiResponseMessage;

    @Column(name = "cmi_approval_code")
    private String cmiApprovalCode;

    @Column(name = "cmi_payment_method")
    private String cmiPaymentMethod;

    @Column(name = "cmi_card_type")
    private String cmiCardType;

    @Column(name = "cmi_card_number")
    private String cmiCardNumber;

    @Column(name = "cmi_card_holder")
    private String cmiCardHolder;

    @Column(name = "cmi_installment")
    private String cmiInstallment;

    @Column(name = "cmi_3d_secure")
    private String cmi3DSecure;

    @Column(name = "cmi_ip_address")
    private String cmiIpAddress;

    @Column(name = "cmi_user_agent")
    private String cmiUserAgent;

    @Column(name = "cmi_created_at")
    private java.time.LocalDateTime cmiCreatedAt;

    @Column(name = "cmi_updated_at")
    private java.time.LocalDateTime cmiUpdatedAt;

    @Column(name = "created_at")
    private java.time.LocalDateTime createdAt;

    @Column(name = "updated_at")
    private java.time.LocalDateTime updatedAt;

    // Constructors
    public Paiement() {}

    public Paiement(Long id, Double amount, String methodePaiement, Instant datePaiement) {
        this.id = id;
        this.amount = amount;
        this.methodePaiement = methodePaiement;
        this.datePaiement = datePaiement;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getMethodePaiement() {
        return methodePaiement;
    }

    public void setMethodePaiement(String methodePaiement) {
        this.methodePaiement = methodePaiement;
    }

    public Instant getDatePaiement() {
        return datePaiement;
    }

    public void setDatePaiement(Instant datePaiement) {
        this.datePaiement = datePaiement;
    }

    public Long getTicketId() {
        return ticketId;
    }

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public java.time.LocalDate getDate() {
        return date;
    }

    public void setDate(java.time.LocalDate date) {
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getCmiMerchantId() {
        return cmiMerchantId;
    }

    public void setCmiMerchantId(String cmiMerchantId) {
        this.cmiMerchantId = cmiMerchantId;
    }

    public String getCmiOrderId() {
        return cmiOrderId;
    }

    public void setCmiOrderId(String cmiOrderId) {
        this.cmiOrderId = cmiOrderId;
    }

    public String getCmiTransactionId() {
        return cmiTransactionId;
    }

    public void setCmiTransactionId(String cmiTransactionId) {
        this.cmiTransactionId = cmiTransactionId;
    }

    public String getCmiResponseCode() {
        return cmiResponseCode;
    }

    public void setCmiResponseCode(String cmiResponseCode) {
        this.cmiResponseCode = cmiResponseCode;
    }

    public String getCmiResponseMessage() {
        return cmiResponseMessage;
    }

    public void setCmiResponseMessage(String cmiResponseMessage) {
        this.cmiResponseMessage = cmiResponseMessage;
    }

    public String getCmiApprovalCode() {
        return cmiApprovalCode;
    }

    public void setCmiApprovalCode(String cmiApprovalCode) {
        this.cmiApprovalCode = cmiApprovalCode;
    }

    public String getCmiPaymentMethod() {
        return cmiPaymentMethod;
    }

    public void setCmiPaymentMethod(String cmiPaymentMethod) {
        this.cmiPaymentMethod = cmiPaymentMethod;
    }

    public String getCmiCardType() {
        return cmiCardType;
    }

    public void setCmiCardType(String cmiCardType) {
        this.cmiCardType = cmiCardType;
    }

    public String getCmiCardNumber() {
        return cmiCardNumber;
    }

    public void setCmiCardNumber(String cmiCardNumber) {
        this.cmiCardNumber = cmiCardNumber;
    }

    public String getCmiCardHolder() {
        return cmiCardHolder;
    }

    public void setCmiCardHolder(String cmiCardHolder) {
        this.cmiCardHolder = cmiCardHolder;
    }

    public String getCmiInstallment() {
        return cmiInstallment;
    }

    public void setCmiInstallment(String cmiInstallment) {
        this.cmiInstallment = cmiInstallment;
    }

    public String getCmi3DSecure() {
        return cmi3DSecure;
    }

    public void setCmi3DSecure(String cmi3DSecure) {
        this.cmi3DSecure = cmi3DSecure;
    }

    public String getCmiIpAddress() {
        return cmiIpAddress;
    }

    public void setCmiIpAddress(String cmiIpAddress) {
        this.cmiIpAddress = cmiIpAddress;
    }

    public String getCmiUserAgent() {
        return cmiUserAgent;
    }

    public void setCmiUserAgent(String cmiUserAgent) {
        this.cmiUserAgent = cmiUserAgent;
    }

    public java.time.LocalDateTime getCmiCreatedAt() {
        return cmiCreatedAt;
    }

    public void setCmiCreatedAt(java.time.LocalDateTime cmiCreatedAt) {
        this.cmiCreatedAt = cmiCreatedAt;
    }

    public java.time.LocalDateTime getCmiUpdatedAt() {
        return cmiUpdatedAt;
    }

    public void setCmiUpdatedAt(java.time.LocalDateTime cmiUpdatedAt) {
        this.cmiUpdatedAt = cmiUpdatedAt;
    }

    public java.time.LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(java.time.LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public java.time.LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(java.time.LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Paiement)) return false;
        Paiement paiement = (Paiement) o;
        return id != null && id.equals(paiement.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return (
            "Paiement{" +
            "id=" +
            id +
            "," +
            "amount=" +
            amount +
            "," +
            "methodePaiement='" +
            methodePaiement +
            "'" +
            "," +
            "datePaiement=" +
            datePaiement +
            "," +
            "ticketId=" +
            ticketId +
            "," +
            "status='" +
            status +
            "'" +
            "," +
            "transactionId='" +
            transactionId +
            "'" +
            "," +
            "user='" +
            user +
            "'" +
            "," +
            "date=" +
            date +
            "," +
            "description='" +
            description +
            "'" +
            "," +
            "clientIp='" +
            clientIp +
            "'" +
            "," +
            "currency='" +
            currency +
            "'" +
            "," +
            "cmiMerchantId='" +
            cmiMerchantId +
            "'" +
            "," +
            "cmiOrderId='" +
            cmiOrderId +
            "'" +
            "," +
            "cmiTransactionId='" +
            cmiTransactionId +
            "'" +
            "," +
            "cmiResponseCode='" +
            cmiResponseCode +
            "'" +
            "," +
            "cmiResponseMessage='" +
            cmiResponseMessage +
            "'" +
            "," +
            "cmiApprovalCode='" +
            cmiApprovalCode +
            "'" +
            "," +
            "cmiPaymentMethod='" +
            cmiPaymentMethod +
            "'" +
            "," +
            "cmiCardType='" +
            cmiCardType +
            "'" +
            "," +
            "cmiCardNumber='" +
            cmiCardNumber +
            "'" +
            "," +
            "cmiCardHolder='" +
            cmiCardHolder +
            "'" +
            "," +
            "cmiInstallment='" +
            cmiInstallment +
            "'" +
            "," +
            "cmi3DSecure='" +
            cmi3DSecure +
            "'" +
            "," +
            "cmiIpAddress='" +
            cmiIpAddress +
            "'" +
            "," +
            "cmiUserAgent='" +
            cmiUserAgent +
            "'" +
            "," +
            "cmiCreatedAt=" +
            cmiCreatedAt +
            "," +
            "cmiUpdatedAt=" +
            cmiUpdatedAt +
            "," +
            "createdAt=" +
            createdAt +
            "," +
            "updatedAt=" +
            updatedAt +
            '}'
        );
    }
}
