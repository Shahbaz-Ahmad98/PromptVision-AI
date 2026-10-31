
package com.ai.SpringAiDemo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ChatServiceTest {

    private ChatModel chatModel;
    private ChatService chatService;

    @BeforeEach
    void setUp() {
        chatModel = mock(ChatModel.class);
        chatService = new ChatService(chatModel);
    }

    @Test
    void shouldReturnGeminiResponse() {
        // Arrange
        String prompt = "What is Spring Boot?";
        String expected = "Spring Boot is a Java framework.";

        when(chatModel.call(prompt)).thenReturn(expected);

        // Act
        String actual = chatService.getResponse(prompt);

        // Assert
        assertEquals(expected, actual);
        verify(chatModel, times(1)).call(prompt);
    }

    @Test
    void shouldReturnRecipeStyleResponseFromChatService() {
        // Arrange
        String prompt = "Suggest a recipe using rice.";
        String expected = "Prepare vegetable fried rice.";

        when(chatModel.call(prompt)).thenReturn(expected);

        // Act
        String actual = chatService.getResponse(prompt);

        // Assert
        assertEquals(expected, actual);
        verify(chatModel).call(prompt);
    }
}
