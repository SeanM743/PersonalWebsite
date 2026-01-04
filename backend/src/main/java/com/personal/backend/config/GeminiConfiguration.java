package com.personal.backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.model.function.FunctionCallbackWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Configuration
@Slf4j
public class GeminiConfiguration {
    
    @Autowired
    private ApplicationContext applicationContext;
    
    @Value("${chat.session.timeout.minutes:30}")
    private int sessionTimeoutMinutes;
    
    @Value("${chat.context.max.messages:50}")
    private int maxContextMessages;
    
    @Value("${chat.context.max.tokens:8000}")
    private int maxContextTokens;
    
    @Bean
    public List<FunctionCallback> availableFunctions() {
        // Get all Function beans from the application context
        Map<String, Function> functionBeans = applicationContext.getBeansOfType(Function.class);
        
        log.info("Discovered {} function beans for Gemini integration", functionBeans.size());
        
        return functionBeans.entrySet().stream()
                .map(entry -> {
                    String functionName = entry.getKey();
                    Function<?, ?> function = entry.getValue();
                    
                    log.debug("Registering function: {}", functionName);
                    
                    return FunctionCallbackWrapper.builder(function)
                            .withName(functionName)
                            .withDescription(getFunctionDescription(functionName))
                            .build();
                })
                .collect(Collectors.toList());
    }
    
    private String getFunctionDescription(String functionName) {
        // Try to get description from bean definition or use default
        try {
            // Check if the bean has a @Description annotation or similar
            Object bean = applicationContext.getBean(functionName);
            if (bean != null) {
                // For now, return a generic description
                // In a real implementation, you'd extract this from annotations
                return "Function: " + functionName;
            }
        } catch (Exception e) {
            log.debug("Could not get description for function: {}", functionName);
        }
        
        return "Available function: " + functionName;
    }
    
    public int getSessionTimeoutMinutes() {
        return sessionTimeoutMinutes;
    }
    
    public int getMaxContextMessages() {
        return maxContextMessages;
    }
    
    public int getMaxContextTokens() {
        return maxContextTokens;
    }
}