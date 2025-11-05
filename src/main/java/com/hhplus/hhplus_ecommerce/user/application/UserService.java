package com.hhplus.hhplus_ecommerce.user.application;

import com.hhplus.hhplus_ecommerce.common.exception.BusinessException;
import com.hhplus.hhplus_ecommerce.common.exception.ErrorCode;
import com.hhplus.hhplus_ecommerce.user.domain.User;
import com.hhplus.hhplus_ecommerce.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
    // 새로운: 없으면 생성 (테스트용)
    public User getOrCreateUser(Long userId) {
        return userRepository.findById(userId)
                .orElseGet(() -> {
                    String dummyName = "User-" + userId;
                    User newUser = User.create(dummyName);
                    return userRepository.save(newUser);
                });
    }

}