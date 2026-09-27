package com.todayoutfit.outfit;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutfitRepository extends JpaRepository<Outfit, Long> {

    Optional<Outfit> findByIdAndUserId(Long id, Long userId);
}
