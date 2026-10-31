
package com.ai.SpringAiDemo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.ArgumentCaptor;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RecipeServiceTest {

    private ChatModel chatModel;
    private RecipeService recipeService;

    @BeforeEach
    void setUp() {
        chatModel = mock(ChatModel.class);
        recipeService = new RecipeService(chatModel);
    }

    // ==========================================
    // TEST 1: RECIPE GENERATION
    // ==========================================

    @Test
    void shouldGenerateRecipeSuccessfully() {

        // Arrange
        String expectedRecipe =
                "Chicken Biryani Recipe: Cook rice and chicken.";

        ChatResponse mockResponse = mock(ChatResponse.class);
        Generation mockGeneration = mock(Generation.class);

        when(chatModel.call(any(Prompt.class)))
                .thenReturn(mockResponse);

        when(mockResponse.getResult())
                .thenReturn(mockGeneration);

        when(mockGeneration.getOutput())
                .thenReturn(new AssistantMessage(expectedRecipe));

        // Act
        String actualRecipe = recipeService.createRecipe(
                "Chicken, rice, onion",
                "Indian",
                "None"
        );

        // Assert
        assertEquals(expectedRecipe, actualRecipe);

        verify(chatModel, times(1))
                .call(any(Prompt.class));
    }

    // ==========================================
    // TEST 2: VERIFY PROMPT CONTENT
    // ==========================================

    @Test
    void shouldIncludeIngredientsCuisineAndDietInPrompt() {

        // Arrange
        ChatResponse mockResponse = mock(ChatResponse.class);
        Generation mockGeneration = mock(Generation.class);

        when(chatModel.call(any(Prompt.class)))
                .thenReturn(mockResponse);

        when(mockResponse.getResult())
                .thenReturn(mockGeneration);

        when(mockGeneration.getOutput())
                .thenReturn(new AssistantMessage("Vegetable Pasta"));

        // Act
        recipeService.createRecipe(
                "Tomato, pasta, cheese",
                "Italian",
                "Vegetarian"
        );

        // Capture the prompt sent to Gemini
        ArgumentCaptor<Prompt> promptCaptor =
                ArgumentCaptor.forClass(Prompt.class);

        verify(chatModel).call(promptCaptor.capture());

        String promptText =
                promptCaptor.getValue().getContents();

        // Assert
        assertTrue(promptText.contains("Tomato, pasta, cheese"));
        assertTrue(promptText.contains("Italian"));
        assertTrue(promptText.contains("Vegetarian"));
        assertTrue(promptText.contains("Step-by-step cooking instructions"));
    }

    // ==========================================
    // TEST 3: DIFFERENT CUISINES
    // ==========================================

    @Test
    void shouldSupportDifferentCuisineTypes() {

        // Arrange
        ChatResponse mockResponse = mock(ChatResponse.class);
        Generation mockGeneration = mock(Generation.class);

        when(chatModel.call(any(Prompt.class)))
                .thenReturn(mockResponse);

        when(mockResponse.getResult())
                .thenReturn(mockGeneration);

        when(mockGeneration.getOutput())
                .thenReturn(new AssistantMessage("Chinese Fried Rice"));

        // Act
        String result = recipeService.createRecipe(
                "Rice, soy sauce, vegetables",
                "Chinese",
                "Vegan"
        );

        // Assert
        assertEquals("Chinese Fried Rice", result);

        verify(chatModel).call(any(Prompt.class));
    }
}
