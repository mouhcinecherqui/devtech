package devtech.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "paiement")
public class Paiement implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_login")
    private String user;

    @Column(name = "amount")
    private Double amount;

    @Column(name = "currency", length = 3)
    private String currency = "MAD";

    @Column(name = "date")
    private LocalDate date;

    @Column(name = "status", length = 50)
    private String status;

    // Champs CMI
    @Column(name = "cmi_transaction_id", length = 100)
    private String cmiTransactionId;

    @Column(name = "cmi_order_id", length = 100)
    private String cmiOrderId;

    @Column(name = "cmi_response_code", length = 10)
    private String cmiResponseCode;

    @Column(name = "cmi_response_message", length = 500)
    private String cmiResponseMessage;

    @Column(name = "cmi_approval_code", length = 50)
    private String cmiApprovalCode;

    @Column(name = "cmi_merchant_id", length = 50)
    private String cmiMerchantId;

    @Column(name = "cmi_payment_method", length = 50)
    private String cmiPaymentMethod;

    @Column(name = "cmi_card_type", length = 50)
    private String cmiCardType;

    @Column(name = "cmi_card_number", length = 20)
    private String cmiCardNumber;

    @Column(name = "cmi_card_holder", length = 100)
    private String cmiCardHolder;

    @Column(name = "cmi_installment", length = 10)
    private String cmiInstallment;

    @Column(name = "cmi_3d_secure", length = 10)
    private String cmi3DSecure;

    @Column(name = "cmi_ip_address", length = 45)
    private String cmiIpAddress;

    @Column(name = "cmi_user_agent", length = 500)
    private String cmiUserAgent;

    @Column(name = "cmi_created_at")
    private LocalDateTime cmiCreatedAt;

    @Column(name = "cmi_updated_at")
    private LocalDateTime cmiUpdatedAt;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "client_ip", length = 45)
    private String clientIp;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructeurs
    public Paiement() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.cmiCreatedAt = LocalDateTime.now();
        this.cmiUpdatedAt = LocalDateTime.now();
    }

    // Getters et setters existants
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // Getters et setters CMI
    public String getCmiTransactionId() {
        return cmiTransactionId;
    }

    public void setCmiTransactionId(String cmiTransactionId) {
        this.cmiTransactionId = cmiTransactionId;
    }

    public String getCmiOrderId() {
        return cmiOrderId;
    }

    public void setCmiOrderId(String cmiOrderId) {
        this.cmiOrderId = cmiOrderId;
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

    public String getCmiMerchantId() {
        return cmiMerchantId;
    }

    public void setCmiMerchantId(String cmiMerchantId) {
        this.cmiMerchantId = cmiMerchantId;
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

    public LocalDateTime getCmiCreatedAt() {
        return cmiCreatedAt;
    }

    public void setCmiCreatedAt(LocalDateTime cmiCreatedAt) {
        this.cmiCreatedAt = cmiCreatedAt;
    }

    public LocalDateTime getCmiUpdatedAt() {
        return cmiUpdatedAt;
    }

    public void setCmiUpdatedAt(LocalDateTime cmiUpdatedAt) {
        this.cmiUpdatedAt = cmiUpdatedAt;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
