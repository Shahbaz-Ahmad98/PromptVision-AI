
package com.ai.SpringAiDemo.chat;

import com.ai.SpringAiDemo.user.AppUser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatHistoryServiceTest {

    @Mock
    private ChatOwnerRepository ownerRepository;

    @Mock
    private ChatConversationRepository conversationRepository;

    @Mock
    private ChatMessageRepository messageRepository;

    @Mock
    private ChatModel chatModel;

    @InjectMocks
    private ChatHistoryService chatHistoryService;

    private Authentication authentication;
    private AppUser user;
    private ChatConversation conversation;

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
        user.setEmail("user@example.com");
        user.setName("Test User");

        conversation = new ChatConversation();
        conversation.setOwner(user);
        conversation.setTitle("Java Questions");
    }

    // ==========================================
    // TEST 1: CREATE CONVERSATION
    // ==========================================

    @Test
    void shouldCreateConversationForCurrentUser() {

        when(ownerRepository.findByEmailIgnoreCase("user@example.com"))
                .thenReturn(Optional.of(user));

        when(conversationRepository.save(any(ChatConversation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ChatHistoryService.ConversationDto result =
                chatHistoryService.create(authentication);

        assertEquals("New Chat", result.title());

        verify(conversationRepository).save(
                argThat(chat -> chat.getOwner() == user)
        );
    }

    // ==========================================
    // TEST 2: LIST ONLY CURRENT USER'S CHATS
    // ==========================================

    @Test
    void shouldListConversationsForCurrentUser() {

        when(ownerRepository.findByEmailIgnoreCase("user@example.com"))
                .thenReturn(Optional.of(user));

        when(conversationRepository
                .findByOwner_IdOrderByUpdatedAtDesc(1L))
                .thenReturn(List.of(conversation));

        List<ChatHistoryService.ConversationDto> result =
                chatHistoryService.list(authentication);

        assertEquals(1, result.size());
        assertEquals("Java Questions", result.get(0).title());

        verify(conversationRepository)
                .findByOwner_IdOrderByUpdatedAtDesc(1L);
    }

    // ==========================================
    // TEST 3: REJECT ANOTHER USER'S CONVERSATION
    // ==========================================

    @Test
    void shouldRejectAccessToAnotherUsersConversation() {

        when(ownerRepository.findByEmailIgnoreCase("user@example.com"))
                .thenReturn(Optional.of(user));

        when(conversationRepository.findByIdAndOwner_Id(99L, 1L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> chatHistoryService.get(
                                99L,
                                authentication
                        )
                );

        assertEquals(
                HttpStatus.NOT_FOUND,
                exception.getStatusCode()
        );

        verify(messageRepository, never())
                .findByConversation_IdOrderByIdAsc(anyLong());
    }

    // ==========================================
    // TEST 4: REJECT UNAUTHENTICATED REQUEST
    // ==========================================

    @Test
    void shouldRejectUnauthenticatedUser() {

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> chatHistoryService.list(null)
                );

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                exception.getStatusCode()
        );

        verifyNoInteractions(
                ownerRepository,
                conversationRepository,
                messageRepository,
                chatModel
        );
    }

    // ==========================================
    // TEST 5: SEND MESSAGE AND SAVE GEMINI REPLY
    // ==========================================

    @Test
    void shouldSendMessageAndSaveGeminiResponse() {

        when(ownerRepository.findByEmailIgnoreCase("user@example.com"))
                .thenReturn(Optional.of(user));

        when(conversationRepository.findByIdAndOwner_Id(10L, 1L))
                .thenReturn(Optional.of(conversation));

        when(messageRepository
                .findTop20ByConversation_IdOrderByIdDesc(10L))
                .thenReturn(List.of());

        ChatResponse mockResponse = mock(ChatResponse.class);
        Generation mockGeneration = mock(Generation.class);

        when(chatModel.call(any(Prompt.class)))
                .thenReturn(mockResponse);

        when(mockResponse.getResult())
                .thenReturn(mockGeneration);

        when(mockGeneration.getOutput())
                .thenReturn(
                        new AssistantMessage(
                                "Java is a programming language."
                        )
                );

        when(messageRepository.save(any(ChatMessage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        chatHistoryService.send(
                10L,
                "What is Java?",
                authentication
        );

        verify(messageRepository).save(
                argThat(message ->
                        "USER".equals(message.getRole()) &&
                        "What is Java?".equals(message.getContent())
                )
        );

        verify(messageRepository).save(
                argThat(message ->
                        "ASSISTANT".equals(message.getRole()) &&
                        "Java is a programming language."
                                .equals(message.getContent())
                )
        );

        verify(chatModel).call(any(Prompt.class));
    }

    // ==========================================
    // TEST 6: REJECT EMPTY CHAT MESSAGE
    // ==========================================

    @Test
    void shouldRejectEmptyMessage() {

        when(ownerRepository.findByEmailIgnoreCase("user@example.com"))
                .thenReturn(Optional.of(user));

        when(conversationRepository.findByIdAndOwner_Id(10L, 1L))
                .thenReturn(Optional.of(conversation));

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> chatHistoryService.send(
                                10L,
                                "   ",
                                authentication
                        )
                );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );

        verifyNoInteractions(chatModel);
    }

    // ==========================================
    // TEST 7: DELETE OWN CONVERSATION
    // ==========================================

    @Test
    void shouldDeleteOwnedConversation() {

        when(ownerRepository.findByEmailIgnoreCase("user@example.com"))
                .thenReturn(Optional.of(user));

        when(conversationRepository.findByIdAndOwner_Id(10L, 1L))
                .thenReturn(Optional.of(conversation));

        chatHistoryService.delete(10L, authentication);

        verify(messageRepository)
                .deleteConversationMessages(10L);

        verify(conversationRepository)
                .delete(conversation);
    }
}
