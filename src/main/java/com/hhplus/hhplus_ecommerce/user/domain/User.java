package com.hhplus.hhplus_ecommerce.user.domain;

import com.hhplus.hhplus_ecommerce.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;


    // 사용자 생성
    public static User create(String name) {
        return User.builder()
                .name(name)
                .build();
    }

    public void updateInfo(String name) {
        this.name = name;
    }
}