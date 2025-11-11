package devtech.service.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class PaiementDTO {

    public Long id;
    public String user;
    public Object amount; // Peut être Double, String ou Number
    public String currency = "MAD";
    public Object date; // Peut être LocalDate ou String
    public String status;

    // Champs CMI
    public String cmiTransactionId;
    public String cmiOrderId;
    public String cmiResponseCode;
    public String cmiResponseMessage;
    public String cmiApprovalCode;
    public String cmiMerchantId;
    public String cmiPaymentMethod;
    public String cmiCardType;
    public String cmiCardNumber;
    public String cmiCardHolder;
    public String cmiInstallment;
    public String cmi3DSecure;
    public String cmiIpAddress;
    public String cmiUserAgent;
    public LocalDateTime cmiCreatedAt;
    public LocalDateTime cmiUpdatedAt;

    // Champs additionnels
    public String description;
    public String clientIp;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;

    @Override
    public String toString() {
        return (
            "PaiementDTO{" +
            "id=" +
            id +
            ", user='" +
            user +
            '\'' +
            ", amount=" +
            amount +
            " (type: " +
            (amount != null ? amount.getClass().getSimpleName() : "null") +
            ")" +
            ", currency='" +
            currency +
            '\'' +
            ", date=" +
            date +
            " (type: " +
            (date != null ? date.getClass().getSimpleName() : "null") +
            ")" +
            ", status='" +
            status +
            '\'' +
            ", description='" +
            description +
            '\'' +
            '}'
        );
    }
}
