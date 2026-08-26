package ru.practicum.ewm.main.request;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ParticipationRequestRepository extends JpaRepository<ParticipationRequest, Long> {
    @EntityGraph(attributePaths = {"event", "requester"})
    List<ParticipationRequest> findAllByRequesterIdOrderById(long requesterId);

    @EntityGraph(attributePaths = {"event", "requester"})
    List<ParticipationRequest> findAllByEventIdOrderById(long eventId);

    @EntityGraph(attributePaths = {"event", "requester"})
    List<ParticipationRequest> findAllByIdInAndEventId(Collection<Long> ids, long eventId);

    @EntityGraph(attributePaths = {"event", "requester"})
    List<ParticipationRequest> findAllByEventIdAndStatus(long eventId, RequestStatus status);

    @EntityGraph(attributePaths = {"event", "requester"})
    Optional<ParticipationRequest> findByIdAndRequesterId(long requestId, long requesterId);

    boolean existsByRequesterIdAndEventId(long requesterId, long eventId);
}
