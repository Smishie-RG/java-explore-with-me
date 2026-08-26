package ru.practicum.ewm.main.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UpdateEventUserRequest {
    @Pattern(regexp = "(?s).*\\S.*")
    @Size(min = 20, max = 2000)
    private String annotation;

    private Long category;

    @Pattern(regexp = "(?s).*\\S.*")
    @Size(min = 20, max = 7000)
    private String description;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime eventDate;

    @Valid
    private Location location;

    private Boolean paid;

    @PositiveOrZero
    private Integer participantLimit;

    private Boolean requestModeration;
    private UserStateAction stateAction;

    @Pattern(regexp = "(?s).*\\S.*")
    @Size(min = 3, max = 120)
    private String title;
}
