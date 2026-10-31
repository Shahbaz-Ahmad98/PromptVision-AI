
package com.ai.SpringAiDemo;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface GeneratedImageRepository
        extends JpaRepository<GeneratedImage, Long> {

    List<GeneratedImage> findByUserIdOrderByCreatedAtDesc(
            Long userId
    );

    Optional<GeneratedImage> findByIdAndUserId(
            Long id,
            Long userId
    );
}
