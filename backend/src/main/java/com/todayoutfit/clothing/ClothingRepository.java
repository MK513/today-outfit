package com.todayoutfit.clothing;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ClothingRepository extends JpaRepository<Clothing, Long>, JpaSpecificationExecutor<Clothing> {

    Optional<Clothing> findByIdAndUserId(Long id, Long userId);

    boolean existsByImageKey(String imageKey);

    List<Clothing> findAllByUserIdAndIdIn(Long userId, Collection<Long> ids);
}
