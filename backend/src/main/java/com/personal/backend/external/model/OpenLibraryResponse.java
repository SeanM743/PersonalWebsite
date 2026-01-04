package com.personal.backend.external.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenLibraryResponse {
    
    @JsonProperty("numFound")
    private int numFound;
    
    @JsonProperty("start")
    private int start;
    
    @JsonProperty("numFoundExact")
    private boolean numFoundExact;
    
    @JsonProperty("docs")
    private List<BookMetadata> docs;
    
    @JsonProperty("num_found")
    private Integer totalFound;
    
    public boolean hasResults() {
        return docs != null && !docs.isEmpty();
    }
    
    public BookMetadata getFirstResult() {
        return hasResults() ? docs.get(0) : null;
    }
}