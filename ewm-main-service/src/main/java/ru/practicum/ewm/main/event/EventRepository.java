package ru.practicum.ewm.main.event;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {
    @EntityGraph(attributePaths = {"category", "initiator"})
    Optional<Event> findByIdAndInitiatorId(long eventId, long initiatorId);

    @EntityGraph(attributePaths = {"category", "initiator"})
    List<Event> findAllByInitiatorId(long initiatorId, Pageable pageable);

    @EntityGraph(attributePaths = {"category", "initiator"})
    @Query("select e from Event e where e.id = :eventId")
    Optional<Event> findDetailedById(@Param("eventId") long eventId);

    @EntityGraph(attributePaths = {"category", "initiator"})
    @Query("""
            select e from Event e
            where (:filterUsers = false or e.initiator.id in :users)
              and (:filterStates = false or e.state in :states)
              and (:filterCategories = false or e.category.id in :categories)
              and (:filterStart = false or e.eventDate >= :rangeStart)
              and (:filterEnd = false or e.eventDate <= :rangeEnd)
            """)
    List<Event> searchAdmin(@Param("users") Collection<Long> users,
                            @Param("filterUsers") boolean filterUsers,
                            @Param("states") Collection<EventState> states,
                            @Param("filterStates") boolean filterStates,
                            @Param("categories") Collection<Long> categories,
                            @Param("filterCategories") boolean filterCategories,
                            @Param("rangeStart") LocalDateTime rangeStart,
                            @Param("filterStart") boolean filterStart,
                            @Param("rangeEnd") LocalDateTime rangeEnd,
                            @Param("filterEnd") boolean filterEnd,
                            Pageable pageable);

    @EntityGraph(attributePaths = {"category", "initiator"})
    @Query("""
            select e from Event e
            where e.state = ru.practicum.ewm.main.event.EventState.PUBLISHED
              and (:filterText = false
                   or lower(e.annotation) like lower(concat('%', :text, '%'))
                   or lower(e.description) like lower(concat('%', :text, '%')))
              and (:filterCategories = false or e.category.id in :categories)
              and (:filterPaid = false or e.paid = :paid)
              and e.eventDate >= :rangeStart
              and (:filterEnd = false or e.eventDate <= :rangeEnd)
              and (:onlyAvailable = false or e.participantLimit = 0
                   or e.confirmedRequests < e.participantLimit)
            """)
    List<Event> searchPublic(@Param("text") String text,
                             @Param("filterText") boolean filterText,
                             @Param("categories") Collection<Long> categories,
                             @Param("filterCategories") boolean filterCategories,
                             @Param("paid") boolean paid,
                             @Param("filterPaid") boolean filterPaid,
                             @Param("rangeStart") LocalDateTime rangeStart,
                             @Param("rangeEnd") LocalDateTime rangeEnd,
                             @Param("filterEnd") boolean filterEnd,
                             @Param("onlyAvailable") boolean onlyAvailable,
                             Pageable pageable);
}
