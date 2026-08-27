package ru.practicum.ewm.main.compilation;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

@Data
public class UpdateCompilationRequest {
    private Set<Long> events;
    private Boolean pinned;

    @Pattern(regexp = "(?s).*\\S.*")
    @Size(max = 50)
    private String title;
}
