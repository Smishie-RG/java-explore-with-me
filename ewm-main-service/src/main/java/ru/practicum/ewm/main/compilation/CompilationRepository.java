package ru.practicum.ewm.main.compilation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CompilationRepository extends JpaRepository<Compilation, Long> {
    Page<Compilation> findAllByPinned(boolean pinned, Pageable pageable);

    @EntityGraph(attributePaths = {"events", "events.category", "events.initiator"})
    @Query("select distinct c from Compilation c where c.id = :compilationId")
    Optional<Compilation> findDetailedById(@Param("compilationId") long compilationId);

}
