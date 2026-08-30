package ru.practicum.ewm.main.comment;

import java.util.List;

public interface CommentService {
    CommentDto create(long userId, long eventId, CommentTextDto dto);

    CommentDto update(long userId, long commentId, CommentTextDto dto);

    void deleteByAuthor(long userId, long commentId);

    void deleteByAdmin(long commentId);

    CommentDto getById(long commentId);

    List<CommentDto> getEventComments(long eventId, int from, int size);
}
