package devtech.service.mapper;

import java.util.List;

/**
 * Contract for a generic dto to entity mapper.
 *
 * @param <D> - DTO type parameter.
 * @param <E> - Entity type parameter.
 */
public interface EntityMapper<D, E> {
    /**
     * Converts an entity to a DTO.
     *
     * @param entity the entity to convert.
     * @return the DTO.
     */
    D toDto(E entity);

    /**
     * Converts a DTO to an entity.
     *
     * @param dto the DTO to convert.
     * @return the entity.
     */
    E toEntity(D dto);

    /**
     * Converts a list of entities to a list of DTOs.
     *
     * @param entityList the list of entities to convert.
     * @return the list of DTOs.
     */
    List<D> toDto(List<E> entityList);

    /**
     * Converts a list of DTOs to a list of entities.
     *
     * @param dtoList the list of DTOs to convert.
     * @return the list of entities.
     */
    List<E> toEntity(List<D> dtoList);
}
