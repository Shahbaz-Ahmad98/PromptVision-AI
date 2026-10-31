
package com.ai.SpringAiDemo.recipe;

import com.ai.SpringAiDemo.chat.ChatOwnerRepository;
import com.ai.SpringAiDemo.user.AppUser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SavedRecipeControllerTest {

    @Mock
    private SavedRecipeRepository recipeRepository;

    @Mock
    private ChatOwnerRepository userRepository;

    @InjectMocks
    private SavedRecipeController controller;

    private Authentication authentication;
    private AppUser user;
    private SavedRecipe recipe;

    @BeforeEach
    void setUp() {

        authentication =
                new UsernamePasswordAuthenticationToken(
                        "user@example.com",
                        null,
                        List.of()
                );

        user = new AppUser();
        user.setId(1L);
        user.setName("Test User");
        user.setEmail("user@example.com");

        recipe = new SavedRecipe();
        recipe.setOwner(user);
        recipe.setTitle("Chicken Biryani");
        recipe.setIngredients("Rice, Chicken, Onion");
        recipe.setCuisine("Indian");
        recipe.setDietaryRestrictions("None");
        recipe.setContent("Cook rice and chicken together.");
    }

    private void mockCurrentUser() {
        when(userRepository.findByEmailIgnoreCase(
                "user@example.com"
        )).thenReturn(Optional.of(user));
    }

    // ==========================================
    // TEST 1: SAVE A RECIPE
    // ==========================================

    @Test
    void shouldSaveRecipeSuccessfully() {

        mockCurrentUser();

        when(recipeRepository.save(any(SavedRecipe.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        var request =
                new SavedRecipeController.SaveRecipeRequest(
                        "Chicken Biryani",
                        "Rice, Chicken, Onion",
                        "Indian",
                        "None",
                        "Cook rice and chicken together."
                );

        ResponseEntity<SavedRecipeController.RecipeResponse>
                response = controller.save(
                        request,
                        authentication
                );

        assertEquals(
                HttpStatus.CREATED,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());

        assertEquals(
                "Chicken Biryani",
                response.getBody().title()
        );

        verify(recipeRepository).save(
                argThat(saved ->
                        saved.getOwner() == user &&
                        "Chicken Biryani".equals(saved.getTitle())
                )
        );
    }

    // ==========================================
    // TEST 2: LIST CURRENT USER'S RECIPES
    // ==========================================

    @Test
    void shouldListOnlyCurrentUsersRecipes() {

        mockCurrentUser();

        when(recipeRepository
                .findByOwner_IdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(recipe));

        var result = controller.list(authentication);

        assertEquals(1, result.size());

        assertEquals(
                "Chicken Biryani",
                result.get(0).title()
        );

        verify(recipeRepository)
                .findByOwner_IdOrderByCreatedAtDesc(1L);
    }

    // ==========================================
    // TEST 3: VIEW OWN RECIPE
    // ==========================================

    @Test
    void shouldViewOwnRecipe() {

        mockCurrentUser();

        when(recipeRepository.findByIdAndOwner_Id(10L, 1L))
                .thenReturn(Optional.of(recipe));

        var result = controller.get(
                10L,
                authentication
        );

        assertEquals("Chicken Biryani", result.title());
        assertEquals("Indian", result.cuisine());
    }

    // ==========================================
    // TEST 4: REJECT ANOTHER USER'S RECIPE
    // ==========================================

    @Test
    void shouldRejectAnotherUsersRecipe() {

        mockCurrentUser();

        when(recipeRepository.findByIdAndOwner_Id(99L, 1L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> controller.get(
                                99L,
                                authentication
                        )
                );

        assertEquals(
                HttpStatus.NOT_FOUND,
                exception.getStatusCode()
        );

        verify(recipeRepository, never())
                .findById(99L);
    }

    // ==========================================
    // TEST 5: DELETE OWN RECIPE
    // ==========================================

    @Test
    void shouldDeleteOwnRecipe() {

        mockCurrentUser();

        when(recipeRepository.findByIdAndOwner_Id(10L, 1L))
                .thenReturn(Optional.of(recipe));

        ResponseEntity<Void> response = controller.delete(
                10L,
                authentication
        );

        assertEquals(
                HttpStatus.NO_CONTENT,
                response.getStatusCode()
        );

        verify(recipeRepository).delete(recipe);
    }

    // ==========================================
    // TEST 6: BLOCK DELETION OF OTHER USER'S RECIPE
    // ==========================================

    @Test
    void shouldRejectDeletingAnotherUsersRecipe() {

        mockCurrentUser();

        when(recipeRepository.findByIdAndOwner_Id(99L, 1L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> controller.delete(
                                99L,
                                authentication
                        )
                );

        assertEquals(
                HttpStatus.NOT_FOUND,
                exception.getStatusCode()
        );

        verify(recipeRepository, never())
                .delete(any(SavedRecipe.class));
    }

    // ==========================================
    // TEST 7: REJECT UNAUTHENTICATED REQUEST
    // ==========================================

    @Test
    void shouldRejectUnauthenticatedUser() {

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> controller.list(null)
                );

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                exception.getStatusCode()
        );

        verifyNoInteractions(
                userRepository,
                recipeRepository
        );
    }
}
