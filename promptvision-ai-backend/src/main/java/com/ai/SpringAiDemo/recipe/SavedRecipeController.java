
package com.ai.SpringAiDemo.recipe;

import com.ai.SpringAiDemo.user.AppUser;
import com.ai.SpringAiDemo.chat.ChatOwnerRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/recipes")
public class SavedRecipeController {

    private final SavedRecipeRepository recipeRepository;
    private final ChatOwnerRepository userRepository;

    public SavedRecipeController(
            SavedRecipeRepository recipeRepository,
            ChatOwnerRepository userRepository
    ) {
        this.recipeRepository = recipeRepository;
        this.userRepository = userRepository;
    }

    public record SaveRecipeRequest(
            @NotBlank @Size(max = 200) String title,
            @NotBlank String ingredients,
            @NotBlank String cuisine,
            String dietaryRestrictions,
            @NotBlank String content
    ) {}

    public record RecipeResponse(
            Long id,
            String title,
            String ingredients,
            String cuisine,
            String dietaryRestrictions,
            String content,
            LocalDateTime createdAt
    ) {}

    private AppUser currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Login required"
            );
        }

        return userRepository
                .findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "User not found"
                ));
    }

    private RecipeResponse toResponse(SavedRecipe r) {
        return new RecipeResponse(
                r.getId(),
                r.getTitle(),
                r.getIngredients(),
                r.getCuisine(),
                r.getDietaryRestrictions(),
                r.getContent(),
                r.getCreatedAt()
        );
    }

    @PostMapping
    @Transactional
    public ResponseEntity<RecipeResponse> save(
            @Valid @RequestBody SaveRecipeRequest request,
            Authentication authentication
    ) {
        AppUser user = currentUser(authentication);

        SavedRecipe recipe = new SavedRecipe();
        recipe.setOwner(user);
        recipe.setTitle(request.title().trim());
        recipe.setIngredients(request.ingredients().trim());
        recipe.setCuisine(request.cuisine().trim());
        recipe.setDietaryRestrictions(
                request.dietaryRestrictions() == null
                        ? "None"
                        : request.dietaryRestrictions().trim()
        );
        recipe.setContent(request.content());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(recipeRepository.save(recipe)));
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<RecipeResponse> list(Authentication authentication) {
        AppUser user = currentUser(authentication);

        return recipeRepository
                .findByOwner_IdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public RecipeResponse get(
            @PathVariable Long id,
            Authentication authentication
    ) {
        AppUser user = currentUser(authentication);

        SavedRecipe recipe = recipeRepository
                .findByIdAndOwner_Id(id, user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Recipe not found"
                ));

        return toResponse(recipe);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            Authentication authentication
    ) {
        AppUser user = currentUser(authentication);

        SavedRecipe recipe = recipeRepository
                .findByIdAndOwner_Id(id, user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Recipe not found"
                ));

        recipeRepository.delete(recipe);

        return ResponseEntity.noContent().build();
    }
}
