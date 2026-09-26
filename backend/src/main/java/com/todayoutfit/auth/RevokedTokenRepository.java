package com.todayoutfit.auth;

import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface RevokedTokenRepository extends JpaRepository<RevokedToken, String> {

    @Transactional
    @Modifying
    @Query("delete from RevokedToken t where t.expiresAt < :now")
    int deleteExpired(LocalDateTime now);
}
