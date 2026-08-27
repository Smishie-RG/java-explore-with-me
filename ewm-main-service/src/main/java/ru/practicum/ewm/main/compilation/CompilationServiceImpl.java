package ru.practicum.ewm.main.compilation;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.main.error.NotFoundException;
import ru.practicum.ewm.main.event.Event;
import ru.practicum.ewm.main.event.EventMapper;
import ru.practicum.ewm.main.event.EventRepository;
import ru.practicum.ewm.main.event.EventStatsService;
import ru.practicum.ewm.main.util.OffsetPageRequest;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {
    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final EventStatsService eventStatsService;

    @Override
    @Transactional
    public CompilationDto create(NewCompilationDto dto) {
        Set<Event> events = findEvents(dto.getEvents());
        Boolean pinned = dto.getPinned() == null ? false : dto.getPinned();
        Compilation compilation = new Compilation(null, pinned, dto.getTitle(), events);
        return toDto(compilationRepository.save(compilation));
    }

    @Override
    @Transactional
    public CompilationDto update(long compilationId, UpdateCompilationRequest request) {
        Compilation compilation = findCompilation(compilationId);
        if (request.getEvents() != null) {
            compilation.setEvents(findEvents(request.getEvents()));
        }
        if (request.getPinned() != null) {
            compilation.setPinned(request.getPinned());
        }
        if (request.getTitle() != null) {
            compilation.setTitle(request.getTitle());
        }
        return toDto(compilationRepository.save(compilation));
    }

    @Override
    @Transactional
    public void delete(long compilationId) {
        if (!compilationRepository.existsById(compilationId)) {
            throw new NotFoundException("Compilation with id=" + compilationId + " was not found");
        }
        compilationRepository.deleteById(compilationId);
    }

    @Override
    public List<CompilationDto> getAll(Boolean pinned, int from, int size) {
        OffsetPageRequest pageable = new OffsetPageRequest(from, size, Sort.by("id").ascending());
        Page<Compilation> page = pinned == null
                ? compilationRepository.findAll(pageable)
                : compilationRepository.findAllByPinned(pinned, pageable);
        return page.stream().map(this::toDto).toList();
    }

    @Override
    public CompilationDto getById(long compilationId) {
        return toDto(findCompilation(compilationId));
    }

    private Compilation findCompilation(long compilationId) {
        return compilationRepository.findDetailedById(compilationId)
                .orElseThrow(() -> new NotFoundException(
                        "Compilation with id=" + compilationId + " was not found"));
    }

    private Set<Event> findEvents(Set<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return new LinkedHashSet<>();
        }
        List<Event> events = eventRepository.findAllById(eventIds);
        if (events.size() != eventIds.size()) {
            throw new NotFoundException("One or more events were not found");
        }
        return new LinkedHashSet<>(events);
    }

    private CompilationDto toDto(Compilation compilation) {
        List<Event> events = new ArrayList<>(compilation.getEvents());
        Map<Long, Long> views = eventStatsService.getViews(events);
        return new CompilationDto(events.stream()
                .map(event -> EventMapper.toEventShortDto(event,
                        views.getOrDefault(event.getId(), 0L)))
                .toList(), compilation.getId(), compilation.getPinned(), compilation.getTitle());
    }
}
