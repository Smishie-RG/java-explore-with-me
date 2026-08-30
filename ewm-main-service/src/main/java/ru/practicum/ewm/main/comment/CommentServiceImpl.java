package ru.practicum.ewm.main.comment;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.main.error.ConflictException;
import ru.practicum.ewm.main.error.NotFoundException;
import ru.practicum.ewm.main.event.Event;
import ru.practicum.ewm.main.event.EventRepository;
import ru.practicum.ewm.main.event.EventState;
import ru.practicum.ewm.main.user.User;
import ru.practicum.ewm.main.user.UserRepository;
import ru.practicum.ewm.main.util.OffsetPageRequest;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    @Override
    @Transactional
    public CommentDto create(long userId, long eventId, CommentTextDto dto) {
        User author = findUser(userId);
        Event event = findEvent(eventId);
        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Comments can only be added to published events");
        }
        Comment comment = CommentMapper.toComment(dto, author, event);
        return CommentMapper.toDto(commentRepository.save(comment));
    }

    @Override
    @Transactional
    public CommentDto update(long userId, long commentId, CommentTextDto dto) {
        findUser(userId);
        Comment comment = findAuthorComment(userId, commentId);
        comment.setText(dto.getText());
        comment.setUpdated(LocalDateTime.now());
        return CommentMapper.toDto(commentRepository.save(comment));
    }

    @Override
    @Transactional
    public void deleteByAuthor(long userId, long commentId) {
        findUser(userId);
        commentRepository.delete(findAuthorComment(userId, commentId));
    }

    @Override
    @Transactional
    public void deleteByAdmin(long commentId) {
        commentRepository.delete(findComment(commentId));
    }

    @Override
    public CommentDto getById(long commentId) {
        return CommentMapper.toDto(findComment(commentId));
    }

    @Override
    public List<CommentDto> getEventComments(long eventId, int from, int size) {
        Event event = findEvent(eventId);
        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Published event with id=" + eventId + " was not found");
        }
        Sort sort = Sort.by("created").descending().and(Sort.by("id").descending());
        OffsetPageRequest pageable = new OffsetPageRequest(from, size, sort);
        return commentRepository.findAllByEventId(eventId, pageable).stream()
                .map(CommentMapper::toDto)
                .toList();
    }

    private Comment findComment(long commentId) {
        return commentRepository.findDetailedById(commentId)
                .orElseThrow(() -> new NotFoundException(
                        "Comment with id=" + commentId + " was not found"));
    }

    private Comment findAuthorComment(long userId, long commentId) {
        return commentRepository.findByIdAndAuthorId(commentId, userId)
                .orElseThrow(() -> new NotFoundException(
                        "Comment with id=" + commentId + " was not found"));
    }

    private User findUser(long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(
                        "User with id=" + userId + " was not found"));
    }

    private Event findEvent(long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(
                        "Event with id=" + eventId + " was not found"));
    }
}
