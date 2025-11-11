package com.hhplus.hhplus_ecommerce.user.repository;

import com.hhplus.hhplus_ecommerce.user.domain.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository {

    User save(User user);
    Optional<User> findById(Long id);
    List<User> findAll();
}