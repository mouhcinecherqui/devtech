package devtechly.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;

/**
 * A Devis entity for quotes.
 */
@Entity
@Table(name = "devis")
public class Devis implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "montant")
    private Double amount;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "date_validation")
    private Instant dateValidation;

    @Column(name = "ticket_id")
    private Long ticketId;

    @Column(name = "status")
    private String status = "PENDING";

    // Constructors
    public Devis() {}

    public Devis(Long id, Double amount, String description, Instant dateValidation) {
        this.id = id;
        this.amount = amount;
        this.description = description;
        this.dateValidation = dateValidation;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getDateValidation() {
        return dateValidation;
    }

    public void setDateValidation(Instant dateValidation) {
        this.dateValidation = dateValidation;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Devis)) return false;
        Devis devis = (Devis) o;
        return id != null && id.equals(devis.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return (
            "Devis{" +
            "id=" +
            id +
            ", " +
            "amount=" +
            amount +
            ", " +
            "description='" +
            description +
            "'" +
            ", " +
            "dateValidation=" +
            dateValidation +
            ", " +
            "ticketId=" +
            ticketId +
            ", " +
            "status='" +
            status +
            "'" +
            '}'
        );
    }
}
