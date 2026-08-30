package ru.practicum.ewm.main.comment;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.main.event.Event;
import ru.practicum.ewm.main.user.User;
import ru.practicum.ewm.main.user.UserMapper;

import java.time.LocalDateTime;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CommentMapper {
    public static Comment toComment(CommentTextDto dto, User author, Event event) {
        LocalDateTime now = LocalDateTime.now();
        return new Comment(null, dto.getText(), author, event, now, now);
    }

    public static CommentDto toDto(Comment comment) {
        return new CommentDto(comment.getId(), comment.getText(),
                UserMapper.toUserShortDto(comment.getAuthor()), comment.getEvent().getId(),
                comment.getCreated(), comment.getUpdated());
    }
}
