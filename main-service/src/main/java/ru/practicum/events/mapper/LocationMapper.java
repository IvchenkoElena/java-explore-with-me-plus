package ru.practicum.events.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.events.dto.LocationDto;
import ru.practicum.events.model.Location;

@Mapper(componentModel = "spring")
public interface LocationMapper {
    Location toLocation(final LocationDto locationDto);

    @Mapping(target = "id", ignore = true)
    LocationDto toLocationDto(final Location location);
}
