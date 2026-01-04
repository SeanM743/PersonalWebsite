package com.personal.backend.service;

import com.personal.backend.model.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.model.function.FunctionCallbackWrapper;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ToolRegistry {
    
    private final ApplicationContext applicationContext;
    private final Map<String, FunctionCallback> allFunctions = new HashMap<>();
    private final Map<String, Set<Role>> functionPermissions = new HashMap<>();
    
    public void initializeFunctions() {
        if (!allFunctions.isEmpty()) {
            return; // Already initialized
        }
        
        log.info("Initializing function registry...");
        
        // Get all Function beans from the application context
        Map<String, Function> functionBeans = applicationContext.getBeansOfType(Function.class);
        
        for (Map.Entry<String, Function> entry : functionBeans.entrySet()) {
            String functionName = entry.getKey();
            Function<?, ?> function = entry.getValue();
            
            try {
                // Get function description and permissions
                String description = getFunctionDescription(functionName, function);
                Set<Role> permissions = getFunctionPermissions(functionName, function);
                
                // Create function callback
                FunctionCallback callback = FunctionCallbackWrapper.builder(function)
                        .withName(functionName)
                        .withDescription(description)
                        .build();
                
                allFunctions.put(functionName, callback);
                functionPermissions.put(functionName, permissions);
                
                log.debug("Registered function: {} with permissions: {}", functionName, permissions);
                
            } catch (Exception e) {
                log.warn("Failed to register function {}: {}", functionName, e.getMessage());
            }
        }
        
        log.info("Function registry initialized with {} functions", allFunctions.size());
    }
    
    public List<FunctionCallback> getFunctionsForRole(Role userRole) {
        initializeFunctions();
        
        return allFunctions.entrySet().stream()
                .filter(entry -> {
                    Set<Role> permissions = functionPermissions.get(entry.getKey());
                    return permissions.isEmpty() || permissions.contains(userRole);
                })
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }
    
    public List<FunctionCallback> getFunctionsForUser(String username, Role userRole) {
        // For now, just use role-based filtering
        // In the future, this could include user-specific permissions
        return getFunctionsForRole(userRole);
    }
    
    public boolean isFunctionAvailable(String functionName, Role userRole) {
        initializeFunctions();
        
        Set<Role> permissions = functionPermissions.get(functionName);
        if (permissions == null) {
            return false;
        }
        
        return permissions.isEmpty() || permissions.contains(userRole);
    }
    
    public List<String> getAvailableFunctionNames(Role userRole) {
        initializeFunctions();
        
        return allFunctions.keySet().stream()
                .filter(name -> isFunctionAvailable(name, userRole))
                .sorted()
                .collect(Collectors.toList());
    }
    
    public int getTotalFunctionCount() {
        initializeFunctions();
        return allFunctions.size();
    }
    
    public int getFunctionCountForRole(Role userRole) {
        return getFunctionsForRole(userRole).size();
    }
    
    private String getFunctionDescription(String functionName, Function<?, ?> function) {
        try {
            // Try to get description from @Description annotation on the bean
            Object bean = applicationContext.getBean(functionName);
            Class<?> beanClass = bean.getClass();
            
            // Check for @Description annotation on the class
            Description classDescription = beanClass.getAnnotation(Description.class);
            if (classDescription != null) {
                return classDescription.value();
            }
            
            // Check for @Description annotation on methods
            for (Method method : beanClass.getDeclaredMethods()) {
                Description methodDescription = method.getAnnotation(Description.class);
                if (methodDescription != null) {
                    return methodDescription.value();
                }
            }
            
        } catch (Exception e) {
            log.debug("Could not extract description for function: {}", functionName);
        }
        
        // Default description based on function name
        return generateDefaultDescription(functionName);
    }
    
    private String generateDefaultDescription(String functionName) {
        // Convert camelCase to readable description
        String readable = functionName.replaceAll("([A-Z])", " $1").toLowerCase().trim();
        return "Function to " + readable;
    }
    
    private Set<Role> getFunctionPermissions(String functionName, Function<?, ?> function) {
        // Default permissions based on function name patterns
        Set<Role> permissions = new HashSet<>();
        
        String lowerName = functionName.toLowerCase();
        
        if (lowerName.contains("get") || lowerName.contains("read") || lowerName.contains("search") || 
            lowerName.contains("list") || lowerName.contains("view")) {
            // Read-only functions available to all roles
            permissions.add(Role.GUEST);
            permissions.add(Role.ADMIN);
        } else if (lowerName.contains("create") || lowerName.contains("update") || lowerName.contains("delete") || 
                   lowerName.contains("add") || lowerName.contains("modify") || lowerName.contains("remove")) {
            // Write functions only for admin
            permissions.add(Role.ADMIN);
        } else {
            // Default: available to all roles
            permissions.add(Role.GUEST);
            permissions.add(Role.ADMIN);
        }
        
        return permissions;
    }
    
    public Map<String, Object> getFunctionStatistics() {
        initializeFunctions();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalFunctions", allFunctions.size());
        stats.put("guestFunctions", getFunctionCountForRole(Role.GUEST));
        stats.put("adminFunctions", getFunctionCountForRole(Role.ADMIN));
        
        // Function categories
        Map<String, Integer> categories = new HashMap<>();
        for (String functionName : allFunctions.keySet()) {
            String category = categorizeFunction(functionName);
            categories.put(category, categories.getOrDefault(category, 0) + 1);
        }
        stats.put("categories", categories);
        
        return stats;
    }
    
    private String categorizeFunction(String functionName) {
        String lowerName = functionName.toLowerCase();
        
        if (lowerName.contains("post") || lowerName.contains("blog")) {
            return "content";
        } else if (lowerName.contains("activity") || lowerName.contains("media")) {
            return "activities";
        } else if (lowerName.contains("trip") || lowerName.contains("calendar")) {
            return "travel";
        } else if (lowerName.contains("fact") || lowerName.contains("quick")) {
            return "facts";
        } else {
            return "general";
        }
    }
}