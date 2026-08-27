package ru.practicum.ewm.main.request;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.main.error.ConflictException;
import ru.practicum.ewm.main.error.NotFoundException;
import ru.practicum.ewm.main.event.Event;
import ru.practicum.ewm.main.event.EventRepository;
import ru.practicum.ewm.main.event.EventState;
import ru.practicum.ewm.main.user.User;
import ru.practicum.ewm.main.user.UserRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParticipationRequestServiceImpl implements ParticipationRequestService {
    private final ParticipationRequestRepository requestRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ParticipationRequestDto create(long userId, long eventId) {
        User requester = findUser(userId);
        Event event = findEvent(eventId);
        validateNewRequest(userId, event);

        RequestStatus status = RequestStatus.PENDING;
        if (event.getParticipantLimit() == 0 || !event.getRequestModeration()) {
            status = RequestStatus.CONFIRMED;
            event.setConfirmedRequests(event.getConfirmedRequests() + 1);
        }
        ParticipationRequest request = new ParticipationRequest(null, LocalDateTime.now(),
                event, requester, status);
        return ParticipationRequestMapper.toDto(requestRepository.save(request));
    }

    @Override
    public List<ParticipationRequestDto> getUserRequests(long userId) {
        findUser(userId);
        return requestRepository.findAllByRequesterIdOrderById(userId).stream()
                .map(ParticipationRequestMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancel(long userId, long requestId) {
        findUser(userId);
        ParticipationRequest request = requestRepository.findByIdAndRequesterId(requestId, userId)
                .orElseThrow(() -> new NotFoundException(
                        "Request with id=" + requestId + " was not found"));
        if (request.getStatus() == RequestStatus.CONFIRMED) {
            Event event = request.getEvent();
            event.setConfirmedRequests(Math.max(0, event.getConfirmedRequests() - 1));
        }
        request.setStatus(RequestStatus.CANCELED);
        return ParticipationRequestMapper.toDto(requestRepository.save(request));
    }

    @Override
    public List<ParticipationRequestDto> getEventRequests(long userId, long eventId) {
        findUser(userId);
        findUserEvent(userId, eventId);
        return requestRepository.findAllByEventIdOrderById(eventId).stream()
                .map(ParticipationRequestMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateStatuses(
            long userId, long eventId, EventRequestStatusUpdateRequest updateRequest) {
        findUser(userId);
        Event event = findUserEvent(userId, eventId);
        List<ParticipationRequest> requests = requestRepository.findAllByIdInAndEventId(
                updateRequest.getRequestIds(), eventId);
        if (requests.size() != updateRequest.getRequestIds().size()) {
            throw new NotFoundException("One or more participation requests were not found");
        }
        if (requests.stream().anyMatch(request -> request.getStatus() != RequestStatus.PENDING)) {
            throw new ConflictException("Only pending requests can be changed");
        }

        List<ParticipationRequest> confirmed = new ArrayList<>();
        List<ParticipationRequest> rejected = new ArrayList<>();
        if (updateRequest.getStatus() == RequestUpdateStatus.REJECTED) {
            rejectRequests(requests, rejected);
        } else {
            confirmRequests(event, requests, confirmed, rejected);
        }
        requestRepository.saveAll(confirmed);
        requestRepository.saveAll(rejected);
        eventRepository.save(event);
        return new EventRequestStatusUpdateResult(toDtos(confirmed), toDtos(rejected));
    }

    private void confirmRequests(Event event, List<ParticipationRequest> requests,
                                 List<ParticipationRequest> confirmed,
                                 List<ParticipationRequest> rejected) {
        if (event.getParticipantLimit() != 0
                && event.getConfirmedRequests() >= event.getParticipantLimit()) {
            throw new ConflictException("The participant limit has been reached");
        }
        long available = event.getParticipantLimit() == 0
                ? requests.size()
                : event.getParticipantLimit() - event.getConfirmedRequests();
        for (ParticipationRequest request : requests) {
            if (confirmed.size() < available) {
                request.setStatus(RequestStatus.CONFIRMED);
                confirmed.add(request);
            } else {
                request.setStatus(RequestStatus.REJECTED);
                rejected.add(request);
            }
        }
        event.setConfirmedRequests(event.getConfirmedRequests() + confirmed.size());
        if (event.getParticipantLimit() != 0
                && event.getConfirmedRequests() >= event.getParticipantLimit()) {
            List<ParticipationRequest> pending = requestRepository.findAllByEventIdAndStatus(
                    event.getId(), RequestStatus.PENDING);
            pending.removeAll(confirmed);
            pending.removeAll(rejected);
            rejectRequests(pending, rejected);
        }
    }

    private void rejectRequests(List<ParticipationRequest> requests,
                                List<ParticipationRequest> rejected) {
        for (ParticipationRequest request : requests) {
            request.setStatus(RequestStatus.REJECTED);
            rejected.add(request);
        }
    }

    private void validateNewRequest(long userId, Event event) {
        if (event.getInitiator().getId() == userId) {
            throw new ConflictException("The event initiator cannot submit a request");
        }
        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("A request can only be submitted for a published event");
        }
        if (requestRepository.existsByRequesterIdAndEventId(userId, event.getId())) {
            throw new ConflictException("A participation request already exists");
        }
        if (event.getParticipantLimit() != 0
                && event.getConfirmedRequests() >= event.getParticipantLimit()) {
            throw new ConflictException("The participant limit has been reached");
        }
    }

    private List<ParticipationRequestDto> toDtos(List<ParticipationRequest> requests) {
        return requests.stream().map(ParticipationRequestMapper::toDto).toList();
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
}
