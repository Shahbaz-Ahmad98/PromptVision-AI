
package com.ai.SpringAiDemo.chat;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

@RestController
@RequestMapping("/chats")
public class ChatHistoryController {

    private final ChatHistoryService chatHistoryService;

    public ChatHistoryController(
            ChatHistoryService chatHistoryService
    ) {
        this.chatHistoryService = chatHistoryService;
    }

    public record SendMessageRequest(
            @NotBlank String message
    ) {}

    @PostMapping
    public ResponseEntity<ChatHistoryService.ConversationDto> create(
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(chatHistoryService.create(authentication));
    }

    @GetMapping
    public List<ChatHistoryService.ConversationDto> list(
            Authentication authentication
    ) {
        return chatHistoryService.list(authentication);
    }

    @GetMapping("/{id}")
    public ChatHistoryService.ConversationDetailDto get(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return chatHistoryService.get(id, authentication);
    }

    @PostMapping("/{id}/messages")
    public ChatHistoryService.MessageDto send(
            @PathVariable Long id,
            @Valid @RequestBody SendMessageRequest request,
            Authentication authentication
    ) {
        return chatHistoryService.send(
                id,
                request.message(),
                authentication
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            Authentication authentication
    ) {
        chatHistoryService.delete(id, authentication);
        return ResponseEntity.noContent().build();
    }
}
