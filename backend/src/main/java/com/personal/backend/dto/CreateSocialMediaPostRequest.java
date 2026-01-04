package com.personal.backend.dto;

import lombok.Data;
import lombok.Builder;

import java.util.List;

@Data
@Builder
public class CreateSocialMediaPostRequest {
    private String content;
    private List<String> imageUrls;
    private String caption;
}