
package com.ai.SpringAiDemo.chat;

import com.ai.SpringAiDemo.user.AppUser;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class ChatHistoryService {

    private final ChatOwnerRepository ownerRepository;
    private final ChatConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final ChatModel chatModel;

    public ChatHistoryService(
            ChatOwnerRepository ownerRepository,
            ChatConversationRepository conversationRepository,
            ChatMessageRepository messageRepository,
            ChatModel chatModel
    ) {
        this.ownerRepository = ownerRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.chatModel = chatModel;
    }

    private AppUser getCurrentUser(Authentication authentication) {

        if (authentication == null ||
                !authentication.isAuthenticated()) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Please login"
            );
        }

        return ownerRepository
                .findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "User not found"
                ));
    }

    private ChatConversation getOwnedConversation(
            Long id,
            Authentication authentication
    ) {
        AppUser user = getCurrentUser(authentication);

        return conversationRepository
                .findByIdAndOwner_Id(id, user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Conversation not found"
                ));
    }

    @Transactional
    public ConversationDto create(Authentication authentication) {

        AppUser user = getCurrentUser(authentication);

        ChatConversation conversation = new ChatConversation();
        conversation.setOwner(user);

        return toDto(conversationRepository.save(conversation));
    }

    @Transactional(readOnly = true)
    public List<ConversationDto> list(Authentication authentication) {

        AppUser user = getCurrentUser(authentication);

        return conversationRepository
                .findByOwner_IdOrderByUpdatedAtDesc(user.getId())
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ConversationDetailDto get(
            Long id,
            Authentication authentication
    ) {
        ChatConversation conversation =
                getOwnedConversation(id, authentication);

        List<MessageDto> messages = messageRepository
                .findByConversation_IdOrderByIdAsc(id)
                .stream()
                .map(this::toMessageDto)
                .toList();

        return new ConversationDetailDto(
                conversation.getId(),
                conversation.getTitle(),
                messages
        );
    }

    public MessageDto send(
            Long id,
            String text,
            Authentication authentication
    ) {
        ChatConversation conversation =
                getOwnedConversation(id, authentication);

        if (text == null || text.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Message cannot be empty"
            );
        }

        text = text.trim();

        if (text.length() > 8000) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Message is too long"
            );
        }

        List<ChatMessage> history = new ArrayList<>(
                messageRepository
                        .findTop20ByConversation_IdOrderByIdDesc(id)
        );

        Collections.reverse(history);

        List<Message> promptMessages = new ArrayList<>();

        for (ChatMessage oldMessage : history) {

            if ("USER".equals(oldMessage.getRole())) {
                promptMessages.add(
                        new UserMessage(oldMessage.getContent())
                );
            } else if ("ASSISTANT".equals(oldMessage.getRole())) {
                promptMessages.add(
                        new AssistantMessage(oldMessage.getContent())
                );
            }
        }

        promptMessages.add(new UserMessage(text));

        // Call Gemini before writing new messages.
        // If the provider fails, no incomplete turn is saved.
        String reply = chatModel.call(
                new Prompt(promptMessages)
        ).getResult().getOutput().getText();

        if (reply == null || reply.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "AI returned an empty response"
            );
        }

        return saveSuccessfulTurn(
                id,
                text,
                reply,
                authentication
        );
    }

    @Transactional
    public MessageDto saveSuccessfulTurn(
            Long id,
            String text,
            String reply,
            Authentication authentication
    ) {
        ChatConversation conversation =
                getOwnedConversation(id, authentication);

        if ("New Chat".equals(conversation.getTitle())) {
            conversation.setTitle(
                    text.length() > 60
                            ? text.substring(0, 60) + "..."
                            : text
            );
        }

        messageRepository.save(
                new ChatMessage(conversation, "USER", text)
        );

        ChatMessage assistantMessage = messageRepository.save(
                new ChatMessage(conversation, "ASSISTANT", reply)
        );

        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        return toMessageDto(assistantMessage);
    }

    @Transactional
    public void delete(
            Long id,
            Authentication authentication
    ) {
        ChatConversation conversation =
                getOwnedConversation(id, authentication);

        messageRepository.deleteConversationMessages(id);
        conversationRepository.delete(conversation);
    }

    private ConversationDto toDto(ChatConversation conversation) {
        return new ConversationDto(
                conversation.getId(),
                conversation.getTitle(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt()
        );
    }

    private MessageDto toMessageDto(ChatMessage message) {
        return new MessageDto(
                message.getId(),
                message.getRole(),
                message.getContent(),
                message.getCreatedAt()
        );
    }

    public record ConversationDto(
            Long id,
            String title,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record MessageDto(
            Long id,
            String role,
            String content,
            LocalDateTime createdAt
    ) {}

    public record ConversationDetailDto(
            Long id,
            String title,
            List<MessageDto> messages
    ) {}
}
