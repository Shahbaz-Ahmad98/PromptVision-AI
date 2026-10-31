
package com.ai.SpringAiDemo.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface ChatMessageRepository
        extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByConversation_IdOrderByIdAsc(
            Long conversationId
    );

    List<ChatMessage> findTop20ByConversation_IdOrderByIdDesc(
            Long conversationId
    );

    @Modifying
    @Query("""
        DELETE FROM ChatMessage m
        WHERE m.conversation.id = :conversationId
        """)
    void deleteConversationMessages(Long conversationId);
}
