package com.personal.backend.external.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class BookMetadata {
    
    private String title;
    
    @JsonProperty("author_name")
    private List<String> authors;
    
    private String isbn;
    
    @JsonProperty("cover_i")
    private Long coverId;
    
    private String coverUrl;
    
    private String publisher;
    
    @JsonProperty("first_publish_year")
    private Integer publishYear;
    
    private String description;
    
    @JsonProperty("subject")
    private List<String> subjects;
    
    @JsonProperty("language")
    private List<String> languages;
    
    @JsonProperty("number_of_pages_median")
    private Integer pageCount;
    
    // Helper methods
    public String getFirstAuthor() {
        return authors != null && !authors.isEmpty() ? authors.get(0) : null;
    }
    
    public String getAuthorsAsString() {
        return authors != null ? String.join(", ", authors) : null;
    }
    
    public String getCoverUrl() {
        if (coverUrl != null) {
            return coverUrl;
        }
        if (coverId != null) {
            return String.format("https://covers.openlibrary.org/b/id/%d-L.jpg", coverId);
        }
        return null;
    }
    
    public String getSubjectsAsString() {
        return subjects != null ? String.join(", ", subjects) : null;
    }
}