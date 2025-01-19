package ru.practicum.events.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
public class EventAdminUpdateDto {
    public static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";
    @Size(min = 3, max = 120)
    private String title;

    @Size(min = 20, max = 2000)
    private String annotation;

    @Size(min = 20, max = 7000)
    private String description;

    private Long category;

    private LocationDto location;
    @DateTimeFormat(pattern = DATE_PATTERN)
    private LocalDateTime eventDate;

    private Boolean paid;
    private Integer participantLimit;
    private Boolean requestModeration;

    private AdminUpdateStateAction stateAction;


}
