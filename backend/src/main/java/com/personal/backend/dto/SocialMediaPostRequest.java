package com.personal.backend.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SocialMediaPostRequest {
    
    @Size(max = 2000, message = "Post content must be less than 2000 characters")
    private String content;
    
    @Size(max = 10, message = "Maximum 10 images allowed per post")
    private List<MultipartFile> images;
    
    @Size(max = 1000, message = "Comments must be less than 1000 characters")
    private String comments;
    
    @Size(max = 500, message = "Caption must be less than 500 characters")
    private String caption;
}