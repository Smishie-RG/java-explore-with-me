package ru.practicum.ewm.main.comment;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    @EntityGraph(attributePaths = {"author", "event"})
    Optional<Comment> findDetailedById(long commentId);

    @EntityGraph(attributePaths = {"author", "event"})
    Optional<Comment> findByIdAndAuthorId(long commentId, long authorId);

    @EntityGraph(attributePaths = {"author", "event"})
    List<Comment> findAllByEventId(long eventId, Pageable pageable);
}
