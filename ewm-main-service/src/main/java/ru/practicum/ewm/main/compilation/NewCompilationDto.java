package ru.practicum.ewm.main.compilation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

@Data
public class NewCompilationDto {
    private Set<Long> events;
    private Boolean pinned = false;

    @NotBlank
    @Size(max = 50)
    private String title;
}
