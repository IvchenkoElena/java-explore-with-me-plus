package ru.practicum.events.dto;

import lombok.Data;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.events.model.EventState;
import ru.practicum.user.dto.UserShortDto;

@Data
public class EventDto {
    private Long id;
    private String title;
    private String annotation;
    private String description;

    private long confirmedRequests;
    private long views;

    private boolean paid;
    private boolean requestModeration;
    private int participantLimit;

    private CategoryDto category;
    private LocationDto location;

    private EventState state;
    private UserShortDto initiator;

    private String eventDate;
    private String createdOn;
    private String publishedOn;
}
