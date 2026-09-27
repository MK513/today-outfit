package com.todayoutfit.outfit;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutfitRepository extends JpaRepository<Outfit, Long> {

    Optional<Outfit> findByIdAndUserId(Long id, Long userId);

    Page<Outfit> findAllByUserId(Long userId, Pageable pageable);

    Page<Outfit> findAllByUserIdAndSource(Long userId, OutfitSource source, Pageable pageable);
}
