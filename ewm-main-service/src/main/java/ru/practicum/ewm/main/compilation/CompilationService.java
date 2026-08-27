package ru.practicum.ewm.main.compilation;

import java.util.List;

public interface CompilationService {
    CompilationDto create(NewCompilationDto dto);

    CompilationDto update(long compilationId, UpdateCompilationRequest request);

    void delete(long compilationId);

    List<CompilationDto> getAll(Boolean pinned, int from, int size);

    CompilationDto getById(long compilationId);
}
