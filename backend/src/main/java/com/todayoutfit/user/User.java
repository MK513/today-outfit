package com.todayoutfit.user;

import com.todayoutfit.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 소문자로 정규화해 저장한다. */
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    /** BCrypt 해시 */
    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 30)
    private String name;

    @Column(name = "is_demo", nullable = false)
    private boolean demo;

    public User(String email, String password, String name, boolean demo) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.demo = demo;
    }
}
