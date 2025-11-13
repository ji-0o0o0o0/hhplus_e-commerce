package com.hhplus.hhplus_ecommerce.user.repository;

import com.hhplus.hhplus_ecommerce.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    
}