package com.personal.backend.service;

import com.personal.backend.model.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiChatClient {
    
    private final ChatModel chatModel;
    
    @Value("${chat.retry.max.attempts:3}")
    private int maxRetryAttempts;
    
    @Value("${chat.retry.backoff.delay:1000}")
    private long backoffDelay;
    
    @Retryable(
        retryFor = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public String sendMessage(String userMessage, List<ChatMessage> conversationHistory, List<FunctionCallback> enabledFunctions) {
        try {
            log.debug("Sending message to Gemini with {} history messages and {} functions", 
                     conversationHistory.size(), enabledFunctions.size());
            
            List<Message> messages = buildMessageHistory(conversationHistory, userMessage);
            
            Prompt prompt = new Prompt(messages, 
                VertexAiGeminiChatOptions.builder()
                    .withFunctionCallbacks(enabledFunctions)
                    .build());
            
            long startTime = System.currentTimeMillis();
            ChatResponse response = chatModel.call(prompt);
            long duration = System.currentTimeMillis() - startTime;
            
            log.debug("Gemini API call completed in {}ms", duration);
            
            if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
                throw new RuntimeException("Empty response from Gemini API");
            }
            
            String responseContent = response.getResult().getOutput().getContent();
            log.debug("Received response from Gemini: {} characters", responseContent.length());
            
            return responseContent;
            
        } catch (Exception e) {
            log.error("Error calling Gemini API: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get response from AI service: " + e.getMessage(), e);
        }
    }
    
    public String sendSimpleMessage(String userMessage) {
        return sendMessage(userMessage, new ArrayList<>(), new ArrayList<>());
    }
    
    public String sendMessageWithFunctions(String userMessage, List<FunctionCallback> enabledFunctions) {
        return sendMessage(userMessage, new ArrayList<>(), enabledFunctions);
    }
    
    private List<Message> buildMessageHistory(List<ChatMessage> conversationHistory, String currentMessage) {
        List<Message> messages = new ArrayList<>();
        
        // Add system message for context
        messages.add(new SystemMessage(
            "You are a helpful AI assistant for a personal dashboard application. " +
            "You can help users with information about their content, activities, and schedule. " +
            "Use the available functions when appropriate to provide accurate and up-to-date information."
        ));
        
        // Add conversation history
        for (ChatMessage chatMessage : conversationHistory) {
            switch (chatMessage.getRole()) {
                case "user":
                    messages.add(new UserMessage(chatMessage.getContent()));
                    break;
                case "assistant":
                    messages.add(new org.springframework.ai.chat.messages.AssistantMessage(chatMessage.getContent()));
                    break;
                case "system":
                    messages.add(new SystemMessage(chatMessage.getContent()));
                    break;
                default:
                    log.warn("Unknown message role: {}", chatMessage.getRole());
            }
        }
        
        // Add current user message
        messages.add(new UserMessage(currentMessage));
        
        return messages;
    }
    
    public boolean isHealthy() {
        try {
            String testResponse = sendSimpleMessage("Hello");
            return testResponse != null && !testResponse.trim().isEmpty();
        } catch (Exception e) {
            log.warn("Gemini health check failed: {}", e.getMessage());
            return false;
        }
    }
    
    public int estimateTokenCount(String text) {
        // Simple token estimation (roughly 4 characters per token for English)
        return Math.max(1, text.length() / 4);
    }
    
    public int estimateTokenCount(List<ChatMessage> messages) {
        return messages.stream()
                .mapToInt(msg -> estimateTokenCount(msg.getContent()))
                .sum();
    }
}