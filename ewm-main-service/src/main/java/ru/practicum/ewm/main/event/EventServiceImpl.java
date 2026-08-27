package ru.practicum.ewm.main.event;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.main.category.Category;
import ru.practicum.ewm.main.category.CategoryRepository;
import ru.practicum.ewm.main.error.BadRequestException;
import ru.practicum.ewm.main.error.ConflictException;
import ru.practicum.ewm.main.error.NotFoundException;
import ru.practicum.ewm.main.user.User;
import ru.practicum.ewm.main.user.UserRepository;
import ru.practicum.ewm.main.util.OffsetPageRequest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {
    private static final LocalDateTime EMPTY_DATE = LocalDateTime.of(2000, 1, 1, 0, 0);

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final EventStatsService eventStatsService;

    @Override
    @Transactional
    public EventFullDto create(long userId, NewEventDto dto) {
        validateEventDate(dto.getEventDate(), 2);
        User user = findUser(userId);
        Category category = findCategory(dto.getCategory());
        Event event = eventRepository.save(EventMapper.toEvent(dto, category, user));
        return EventMapper.toEventFullDto(event, 0);
    }

    @Override
    public List<EventShortDto> getUserEvents(long userId, int from, int size) {
        findUser(userId);
        Pageable pageable = new OffsetPageRequest(from, size, Sort.by("id").ascending());
        return toShortDtos(eventRepository.findAllByInitiatorId(userId, pageable));
    }

    @Override
    public EventFullDto getUserEvent(long userId, long eventId) {
        findUser(userId);
        Event event = findUserEvent(userId, eventId);
        return toFullDtos(List.of(event)).getFirst();
    }

    @Override
    @Transactional
    public EventFullDto updateUserEvent(long userId, long eventId, UpdateEventUserRequest request) {
        findUser(userId);
        Event event = findUserEvent(userId, eventId);
        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Only pending or canceled events can be changed");
        }
        if (request.getEventDate() != null) {
            validateEventDate(request.getEventDate(), 2);
        }
        updateCommonFields(event, request.getAnnotation(), request.getCategory(),
                request.getDescription(), request.getEventDate(), request.getLocation(),
                request.getPaid(), request.getParticipantLimit(), request.getRequestModeration(),
                request.getTitle());
        if (request.getStateAction() == UserStateAction.CANCEL_REVIEW) {
            event.setState(EventState.CANCELED);
        } else if (request.getStateAction() == UserStateAction.SEND_TO_REVIEW) {
            event.setState(EventState.PENDING);
        }
        return EventMapper.toEventFullDto(eventRepository.save(event), getViews(event));
    }

    @Override
    public List<EventFullDto> searchAdmin(List<Long> users, List<EventState> states,
                                          List<Long> categories, LocalDateTime rangeStart,
                                          LocalDateTime rangeEnd, int from, int size) {
        validateRange(rangeStart, rangeEnd);
        Pageable pageable = new OffsetPageRequest(from, size, Sort.by("id").ascending());
        List<Event> events = eventRepository.searchAdmin(listOrDefault(users, -1L), hasValues(users),
                enumListOrDefault(states), hasValues(states), listOrDefault(categories, -1L),
                hasValues(categories), dateOrDefault(rangeStart), rangeStart != null,
                dateOrDefault(rangeEnd), rangeEnd != null, pageable);
        return toFullDtos(events);
    }

    @Override
    @Transactional
    public EventFullDto updateAdminEvent(long eventId, UpdateEventAdminRequest request) {
        Event event = findEvent(eventId);
        if (request.getEventDate() != null) {
            validateEventDate(request.getEventDate(), 1);
        }
        updateCommonFields(event, request.getAnnotation(), request.getCategory(),
                request.getDescription(), request.getEventDate(), request.getLocation(),
                request.getPaid(), request.getParticipantLimit(), request.getRequestModeration(),
                request.getTitle());
        applyAdminStateAction(event, request.getStateAction());
        return EventMapper.toEventFullDto(eventRepository.save(event), getViews(event));
    }

    @Override
    public List<EventShortDto> searchPublic(String text, List<Long> categories, Boolean paid,
                                            LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                            boolean onlyAvailable, EventSort sort, int from, int size,
                                            HttpServletRequest request) {
        LocalDateTime actualStart = rangeStart == null ? LocalDateTime.now() : rangeStart;
        validateRange(actualStart, rangeEnd);
        boolean sortByViews = sort == EventSort.VIEWS;
        Sort databaseSort = Sort.by(sort == EventSort.EVENT_DATE ? "eventDate" : "id").ascending();
        Pageable pageable = sortByViews
                ? Pageable.unpaged()
                : new OffsetPageRequest(from, size, databaseSort);
        List<Event> events = eventRepository.searchPublic(text == null ? "" : text,
                text != null && !text.isBlank(), listOrDefault(categories, -1L), hasValues(categories),
                Boolean.TRUE.equals(paid), paid != null, actualStart, dateOrDefault(rangeEnd),
                rangeEnd != null, onlyAvailable, pageable);
        eventStatsService.saveHit(request);
        List<EventShortDto> result = new ArrayList<>(toShortDtos(events));
        if (sortByViews) {
            result.sort(Comparator.comparing(EventShortDto::getViews).reversed());
            return paginate(result, from, size);
        }
        return result;
    }

    @Override
    public EventFullDto getPublicEvent(long eventId, HttpServletRequest request) {
        Event event = findEvent(eventId);
        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Published event with id=" + eventId + " was not found");
        }
        eventStatsService.saveHit(request);
        return toFullDtos(List.of(event)).getFirst();
    }

    private void updateCommonFields(Event event, String annotation, Long categoryId,
                                    String description, LocalDateTime eventDate, Location location,
                                    Boolean paid, Integer participantLimit, Boolean requestModeration,
                                    String title) {
        if (annotation != null) {
            event.setAnnotation(annotation);
        }
        if (categoryId != null) {
            event.setCategory(findCategory(categoryId));
        }
        if (description != null) {
            event.setDescription(description);
        }
        if (eventDate != null) {
            event.setEventDate(eventDate);
        }
        if (location != null) {
            event.setLocation(location);
        }
        if (paid != null) {
            event.setPaid(paid);
        }
        if (participantLimit != null) {
            event.setParticipantLimit(participantLimit);
        }
        if (requestModeration != null) {
            event.setRequestModeration(requestModeration);
        }
        if (title != null) {
            event.setTitle(title);
        }
    }

    private void applyAdminStateAction(Event event, AdminStateAction action) {
        if (action == null) {
            return;
        }
        if (event.getState() != EventState.PENDING) {
            throw new ConflictException("Only pending events can be published or rejected");
        }
        if (action == AdminStateAction.PUBLISH_EVENT) {
            validateEventDate(event.getEventDate(), 1);
            event.setState(EventState.PUBLISHED);
            event.setPublishedOn(LocalDateTime.now());
        } else {
            event.setState(EventState.CANCELED);
        }
    }

    private List<EventFullDto> toFullDtos(List<Event> events) {
        Map<Long, Long> views = eventStatsService.getViews(events);
        return events.stream()
                .map(event -> EventMapper.toEventFullDto(event, views.getOrDefault(event.getId(), 0L)))
                .toList();
    }

    private List<EventShortDto> toShortDtos(List<Event> events) {
        Map<Long, Long> views = eventStatsService.getViews(events);
        return events.stream()
                .map(event -> EventMapper.toEventShortDto(event, views.getOrDefault(event.getId(), 0L)))
                .toList();
    }

    private long getViews(Event event) {
        return eventStatsService.getViews(List.of(event)).getOrDefault(event.getId(), 0L);
    }

    private Event findEvent(long eventId) {
        return eventRepository.findDetailedById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));
    }

    private Event findUserEvent(long userId, long eventId) {
        return eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));
    }

    private User findUser(long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));
    }

    private Category findCategory(long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException(
                        "Category with id=" + categoryId + " was not found"));
    }

    private void validateEventDate(LocalDateTime eventDate, int hours) {
        if (eventDate.isBefore(LocalDateTime.now().plusHours(hours))) {
            throw new BadRequestException("Event date must be at least " + hours + " hour(s) from now");
        }
    }

    private void validateRange(LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null && start.isAfter(end)) {
            throw new BadRequestException("Range start must be before range end");
        }
    }

    private boolean hasValues(List<?> values) {
        return values != null && !values.isEmpty();
    }

    private List<Long> listOrDefault(List<Long> values, Long defaultValue) {
        return hasValues(values) ? values : List.of(defaultValue);
    }

    private List<EventState> enumListOrDefault(List<EventState> values) {
        return hasValues(values) ? values : List.of(EventState.PENDING);
    }

    private LocalDateTime dateOrDefault(LocalDateTime value) {
        return value == null ? EMPTY_DATE : value;
    }

    private List<EventShortDto> paginate(List<EventShortDto> events, int from, int size) {
        if (from >= events.size()) {
            return List.of();
        }
        int end = Math.min(from + size, events.size());
        return events.subList(from, end);
    }
}
