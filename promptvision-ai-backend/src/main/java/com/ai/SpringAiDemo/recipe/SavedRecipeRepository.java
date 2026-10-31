
package com.ai.SpringAiDemo.recipe;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SavedRecipeRepository
        extends JpaRepository<SavedRecipe, Long> {

    List<SavedRecipe> findByOwner_IdOrderByCreatedAtDesc(Long userId);

    Optional<SavedRecipe> findByIdAndOwner_Id(Long id, Long userId);
}
