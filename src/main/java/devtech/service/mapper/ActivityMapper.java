package devtech.service.mapper;

import devtech.domain.Activity;
import devtech.service.dto.ActivityDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Activity} and its DTO {@link ActivityDTO}.
 */
@Mapper(componentModel = "spring")
public interface ActivityMapper extends EntityMapper<ActivityDTO, Activity> {
    @Mapping(target = "activityType", source = "activityType")
    ActivityDTO toDto(Activity s);

    @Mapping(target = "activityType", source = "activityType")
    Activity toEntity(ActivityDTO activityDTO);

    @Named("activityTypeToString")
    default String activityTypeToString(devtech.domain.ActivityType activityType) {
        return activityType != null ? activityType.getValue() : null;
    }

    @Named("stringToActivityType")
    default devtech.domain.ActivityType stringToActivityType(String activityType) {
        if (activityType == null) {
            return null;
        }
        return devtech.domain.ActivityType.valueOf(activityType.toUpperCase());
    }
}
