package org.example.repository.interfaces;

import org.example.model.User;
import java.util.List;
import java.util.Optional;

public interface UserRepository {
    void save(User user);
    Optional<User> findByUsername(String username);
    List<User> findAll();
}
