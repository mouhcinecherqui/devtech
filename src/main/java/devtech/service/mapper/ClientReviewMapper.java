package devtech.service.mapper;

import devtech.domain.ClientReview;
import devtech.service.dto.ClientReviewDTO;
import java.util.List;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link ClientReview} and its DTO {@link ClientReviewDTO}.
 */
@Mapper(componentModel = "spring")
public interface ClientReviewMapper extends EntityMapper<ClientReviewDTO, ClientReview> {
    @Mapping(target = "ticketId", source = "ticket.id")
    @Mapping(target = "ticketTitle", source = "ticket.type")
    ClientReviewDTO toDto(ClientReview s);

    @Mapping(target = "ticket.id", source = "ticketId")
    @Mapping(target = "ticket.type", source = "ticketTitle")
    ClientReview toEntity(ClientReviewDTO s);

    @Override
    default List<ClientReviewDTO> toDto(List<ClientReview> entityList) {
        return entityList.stream().map(this::toDto).toList();
    }

    @Override
    default List<ClientReview> toEntity(List<ClientReviewDTO> dtoList) {
        return dtoList.stream().map(this::toEntity).toList();
    }

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "ticket.id", source = "ticketId")
    @Mapping(target = "ticket.type", source = "ticketTitle")
    void partialUpdate(@MappingTarget ClientReview entity, ClientReviewDTO dto);

    default ClientReview fromId(Long id) {
        if (id == null) {
            return null;
        }
        ClientReview clientReview = new ClientReview();
        clientReview.setId(id);
        return clientReview;
    }
}
