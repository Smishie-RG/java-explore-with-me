package ru.practicum.ewm.main.category;

import java.util.List;

public interface CategoryService {
    CategoryDto create(NewCategoryDto dto);

    CategoryDto update(long categoryId, CategoryDto dto);

    void delete(long categoryId);

    List<CategoryDto> getAll(int from, int size);

    CategoryDto getById(long categoryId);
}
