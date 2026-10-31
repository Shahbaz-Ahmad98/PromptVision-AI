package com.ai.SpringAiDemo;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private final ChatModel chatModel;

    public ChatService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    // Normal AI Chat
    public String getResponse(String prompt) {
        return chatModel.call(prompt);
    }

    // Chat with model options
    public String getResponseOptions(String prompt) {

        ChatResponse response = chatModel.call(
                new Prompt(
                        prompt,
                        OpenAiChatOptions.builder()
                                .model("nvidia/nemotron-3.5-super-vl-120b-a12b")
                                .temperature(1.0)
                                .build()
                )
        );

        return response.getResult().getOutput().getText();
    }
}