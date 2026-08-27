package ru.practicum.ewm.main.user;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.main.error.NotFoundException;
import ru.practicum.ewm.main.util.OffsetPageRequest;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserDto create(NewUserRequest request) {
        return UserMapper.toUserDto(userRepository.save(UserMapper.toUser(request)));
    }

    @Override
    public List<UserDto> getAll(List<Long> ids, int from, int size) {
        Pageable pageable = new OffsetPageRequest(from, size, Sort.by("id").ascending());
        List<User> users = ids == null || ids.isEmpty()
                ? userRepository.findAll(pageable).getContent()
                : userRepository.findAllByIdIn(ids, pageable).getContent();
        return users.stream().map(UserMapper::toUserDto).toList();
    }

    @Override
    @Transactional
    public void delete(long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User with id=" + userId + " was not found");
        }
        userRepository.deleteById(userId);
        userRepository.flush();
    }
}
