package com.personal.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountResponse<T> {
    private boolean success;
    private T data;
    private String message;
    private String error;

    public static <T> AccountResponse<T> success(T data, String message) {
        return AccountResponse.<T>builder()
                .success(true)
                .data(data)
                .message(message)
                .build();
    }

    public static <T> AccountResponse<T> error(String error) {
        return AccountResponse.<T>builder()
                .success(false)
                .error(error)
                .build();
    }
}
