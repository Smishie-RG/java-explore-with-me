package ru.practicum.ewm.main.user;

import java.util.List;

public interface UserService {
    UserDto create(NewUserRequest request);

    List<UserDto> getAll(List<Long> ids, int from, int size);

    void delete(long userId);
}
