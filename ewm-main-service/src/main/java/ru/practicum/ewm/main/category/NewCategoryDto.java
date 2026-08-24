package ru.practicum.ewm.main.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class NewCategoryDto {
    @NotBlank
    @Size(max = 50)
    private String name;
}
