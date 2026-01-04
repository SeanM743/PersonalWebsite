package com.personal.backend.config;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class SpringAIConfiguration {
    
    @Bean
    @Primary
    public ChatModel geminiChatModel() {
        // Mock implementation for now - will be replaced with proper Gemini integration
        return new ChatModel() {
            @Override
            public ChatResponse call(Prompt prompt) {
                // Return a simple mock response
                return ChatResponse.builder()
                        .withGenerations(java.util.List.of())
                        .build();
            }
            
            @Override
            public org.springframework.ai.chat.prompt.ChatOptions getDefaultOptions() {
                return null;
            }
        };
    }
}