package com.hhplus.hhplus_ecommerce.user.infrastructure;

import com.hhplus.hhplus_ecommerce.user.domain.User;
import com.hhplus.hhplus_ecommerce.user.repository.UserRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryUserRepository implements UserRepository {

    private final Map<Long, User> store = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public User save(User user) {
        if (user.getId() == null) {
            User newUser = User.builder()
                    .id(idGenerator.getAndIncrement())
                    .name(user.getName())
                    .build();
            store.put(newUser.getId(), newUser);
            return newUser;
        } else {
            store.put(user.getId(), user);
            return user;
        }
    }

    @Override
    public Optional<User> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(store.values());
    }

    public void clear() {
        store.clear();
        idGenerator.set(1);
    }
}