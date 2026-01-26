package devtechly.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO for contact requests from clients.
 */
public class ContactRequestDTO {

    @NotBlank(message = "Le sujet est obligatoire")
    @Size(min = 5, max = 200, message = "Le sujet doit contenir entre 5 et 200 caractères")
    private String subject;

    @NotBlank(message = "Le message est obligatoire")
    @Size(min = 20, max = 2000, message = "Le message doit contenir entre 20 et 2000 caractères")
    private String message;

    @NotNull(message = "La priorité est obligatoire")
    private String priority;

    private String timestamp;

    // Constructors
    public ContactRequestDTO() {}

    public ContactRequestDTO(String subject, String message, String priority, String timestamp) {
        this.subject = subject;
        this.message = message;
        this.priority = priority;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return (
            "ContactRequestDTO{" +
            "subject='" +
            subject +
            '\'' +
            ", message='" +
            message +
            '\'' +
            ", priority='" +
            priority +
            '\'' +
            ", timestamp='" +
            timestamp +
            '\'' +
            '}'
        );
    }
}
