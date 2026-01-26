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
            
            // Check if Google Cloud credentials are available
            if (!isGoogleCloudConfigured()) {
                log.warn("Google Cloud credentials not configured, providing fallback response");
                return getFallbackResponse(userMessage);
            }
            
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
            
            // If it's a credentials error, provide a helpful fallback
            if (e.getMessage().contains("credentials") || e.getMessage().contains("authentication")) {
                return getFallbackResponse(userMessage);
            }
            
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
            if (!isGoogleCloudConfigured()) {
                log.info("Google Cloud not configured, chat service running in fallback mode");
                return true; // Consider fallback mode as healthy
            }
            String testResponse = sendSimpleMessage("Hello");
            return testResponse != null && !testResponse.trim().isEmpty();
        } catch (Exception e) {
            log.warn("Gemini health check failed: {}", e.getMessage());
            return true; // Return true to allow fallback mode
        }
    }
    
    private boolean isGoogleCloudConfigured() {
        try {
            // Check if project ID is configured
            String projectId = System.getenv("GOOGLE_CLOUD_PROJECT_ID");
            if (projectId == null || projectId.trim().isEmpty()) {
                log.debug("GOOGLE_CLOUD_PROJECT_ID not set");
                return false;
            }
            
            // Try to make a simple call to check credentials
            chatModel.call(new Prompt("test"));
            return true;
        } catch (Exception e) {
            if (e.getMessage().contains("credentials") || 
                e.getMessage().contains("authentication") ||
                e.getMessage().contains("ADC")) {
                log.debug("Google Cloud credentials not configured: {}", e.getMessage());
                return false;
            }
            // Other errors might be temporary, so consider it configured
            return true;
        }
    }
    
    private String getFallbackResponse(String userMessage) {
        // Provide helpful responses based on the user's message
        String lowerMessage = userMessage.toLowerCase();
        
        if (lowerMessage.contains("hello") || lowerMessage.contains("hi")) {
            return "Hello! I'm your personal dashboard assistant. I'd love to help you with information about your content, activities, and schedule, but I need Google Cloud credentials to be configured first. " +
                   "To set up Gemini AI integration, please configure your Google Cloud project ID and credentials.";
        }
        
        if (lowerMessage.contains("help")) {
            return "I can help you with your personal dashboard! However, I'm currently running in fallback mode because Google Cloud credentials aren't configured. " +
                   "I can still provide basic assistance, but for full AI capabilities, please set up Google Cloud Vertex AI credentials.";
        }
        
        if (lowerMessage.contains("weather") || lowerMessage.contains("time") || lowerMessage.contains("date")) {
            return "I'd love to help you with that information, but I need Google Cloud credentials to access real-time data. " +
                   "In the meantime, you can check your dashboard for any scheduled events or activities.";
        }
        
        // Generic fallback response
        return "Thanks for your message! I'm currently running in fallback mode because Google Cloud Vertex AI credentials aren't configured. " +
               "To enable full AI capabilities, please:\n\n" +
               "1. Set up a Google Cloud project\n" +
               "2. Enable the Vertex AI API\n" +
               "3. Configure Application Default Credentials\n" +
               "4. Set the GOOGLE_CLOUD_PROJECT_ID environment variable\n\n" +
               "Until then, I can provide basic responses but won't have access to advanced AI features.";
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