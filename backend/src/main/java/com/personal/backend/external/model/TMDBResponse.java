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
public class TMDBResponse<T> {
    
    private Integer page;
    
    @JsonProperty("total_results")
    private Integer totalResults;
    
    @JsonProperty("total_pages")
    private Integer totalPages;
    
    private List<T> results;
    
    public boolean hasResults() {
        return results != null && !results.isEmpty();
    }
    
    public T getFirstResult() {
        return hasResults() ? results.get(0) : null;
    }
    
    public int getResultCount() {
        return results != null ? results.size() : 0;
    }
}