package ru.practicum.ewm.main.category;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.main.error.NotFoundException;
import ru.practicum.ewm.main.util.OffsetPageRequest;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public CategoryDto create(NewCategoryDto dto) {
        return CategoryMapper.toCategoryDto(categoryRepository.save(CategoryMapper.toCategory(dto)));
    }

    @Override
    @Transactional
    public CategoryDto update(long categoryId, CategoryDto dto) {
        Category category = findCategory(categoryId);
        category.setName(dto.getName());
        return CategoryMapper.toCategoryDto(categoryRepository.saveAndFlush(category));
    }

    @Override
    @Transactional
    public void delete(long categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new NotFoundException("Category with id=" + categoryId + " was not found");
        }
        categoryRepository.deleteById(categoryId);
        categoryRepository.flush();
    }

    @Override
    public List<CategoryDto> getAll(int from, int size) {
        OffsetPageRequest pageable = new OffsetPageRequest(from, size, Sort.by("id").ascending());
        return categoryRepository.findAll(pageable).stream()
                .map(CategoryMapper::toCategoryDto)
                .toList();
    }

    @Override
    public CategoryDto getById(long categoryId) {
        return CategoryMapper.toCategoryDto(findCategory(categoryId));
    }

    private Category findCategory(long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException(
                        "Category with id=" + categoryId + " was not found"));
    }
}
