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
public class MovieMetadata {
    
    private Long id;
    
    private String title;
    
    @JsonProperty("original_title")
    private String originalTitle;
    
    private String overview;
    
    @JsonProperty("release_date")
    private String releaseDate;
    
    @JsonProperty("poster_path")
    private String posterPath;
    
    @JsonProperty("backdrop_path")
    private String backdropPath;
    
    @JsonProperty("genre_ids")
    private List<Integer> genreIds;
    
    @JsonProperty("vote_average")
    private Double voteAverage;
    
    @JsonProperty("vote_count")
    private Integer voteCount;
    
    private Boolean adult;
    
    @JsonProperty("original_language")
    private String originalLanguage;
    
    private Double popularity;
    
    @JsonProperty("video")
    private Boolean hasVideo;
    
    // Additional fields from detailed API calls
    private String director;
    private String genre;
    private Integer runtime;
    private String imdbId;
    private Integer budget;
    private Long revenue;
    
    // Helper methods
    public String getPosterUrl() {
        return posterPath != null ? "https://image.tmdb.org/t/p/w500" + posterPath : null;
    }
    
    public String getBackdropUrl() {
        return backdropPath != null ? "https://image.tmdb.org/t/p/w1280" + backdropPath : null;
    }
    
    public Integer getReleaseYear() {
        if (releaseDate != null && releaseDate.length() >= 4) {
            try {
                return Integer.parseInt(releaseDate.substring(0, 4));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
    
    public String getFormattedRating() {
        return voteAverage != null ? String.format("%.1f/10", voteAverage) : null;
    }
}