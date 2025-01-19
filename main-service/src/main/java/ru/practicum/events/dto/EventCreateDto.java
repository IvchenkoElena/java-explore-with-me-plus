package ru.practicum.events.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
public class EventCreateDto {
    public static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";
    @NotNull
    @Size(min = 3, max = 120)
    private String title;
    @NotNull
    @Size(min = 20, max = 2000)
    private String annotation;
    @NotNull
    @Size(min = 20, max = 7000)
    private String description;
    @NotNull
    private Long category;
    @NotNull
    @DateTimeFormat(pattern = DATE_PATTERN)
    private LocalDateTime eventDate;
    @NotNull
    private LocationDto location;

    private Boolean paid;
    private Integer participantLimit;
    private Boolean requestModeration;
}
