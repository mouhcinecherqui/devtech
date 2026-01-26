package devtechly.service.dto;

import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A DTO for the {@link devtechly.domain.ClientReview} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ClientReviewDTO implements Serializable {

    private Long id;

    @NotNull
    private String clientName;

    @NotNull
    @Min(value = 1)
    @Max(value = 5)
    private Integer rating;

    private String comment;

    @NotNull
    private Instant createdDate;

    @NotNull
    private Instant updatedDate;

    @NotNull
    private Boolean isApproved;

    private Long ticketId;

    private String ticketTitle;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public ClientReviewDTO clientName(String clientName) {
        this.clientName = clientName;
        return this;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public ClientReviewDTO rating(Integer rating) {
        this.rating = rating;
        return this;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public ClientReviewDTO comment(String comment) {
        this.comment = comment;
        return this;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    public ClientReviewDTO createdDate(Instant createdDate) {
        this.createdDate = createdDate;
        return this;
    }

    public Instant getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(Instant updatedDate) {
        this.updatedDate = updatedDate;
    }

    public ClientReviewDTO updatedDate(Instant updatedDate) {
        this.updatedDate = updatedDate;
        return this;
    }

    public Boolean getIsApproved() {
        return isApproved;
    }

    public void setIsApproved(Boolean isApproved) {
        this.isApproved = isApproved;
    }

    public ClientReviewDTO isApproved(Boolean isApproved) {
        this.isApproved = isApproved;
        return this;
    }

    public Long getTicketId() {
        return ticketId;
    }

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
    }

    public ClientReviewDTO ticketId(Long ticketId) {
        this.ticketId = ticketId;
        return this;
    }

    public String getTicketTitle() {
        return ticketTitle;
    }

    public void setTicketTitle(String ticketTitle) {
        this.ticketTitle = ticketTitle;
    }

    public ClientReviewDTO ticketTitle(String ticketTitle) {
        this.ticketTitle = ticketTitle;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ClientReviewDTO)) {
            return false;
        }

        ClientReviewDTO clientReviewDTO = (ClientReviewDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, clientReviewDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ClientReviewDTO{" +
            "id=" + getId() +
            ", clientName='" + getClientName() + "'" +
            ", rating=" + getRating() +
            ", comment='" + getComment() + "'" +
            ", createdDate='" + getCreatedDate() + "'" +
            ", updatedDate='" + getUpdatedDate() + "'" +
            ", isApproved='" + getIsApproved() + "'" +
            ", ticketId=" + getTicketId() +
            ", ticketTitle='" + getTicketTitle() + "'" +
            "}";
    }
}
