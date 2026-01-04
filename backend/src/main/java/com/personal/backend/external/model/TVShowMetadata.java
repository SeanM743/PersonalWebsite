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
public class TVShowMetadata {
    
    private Long id;
    
    private String name;
    
    @JsonProperty("original_name")
    private String originalName;
    
    private String overview;
    
    @JsonProperty("first_air_date")
    private String firstAirDate;
    
    @JsonProperty("last_air_date")
    private String lastAirDate;
    
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
    
    @JsonProperty("original_language")
    private String originalLanguage;
    
    private Double popularity;
    
    @JsonProperty("origin_country")
    private List<String> originCountry;
    
    // Additional fields from detailed API calls
    @JsonProperty("number_of_episodes")
    private Integer numberOfEpisodes;
    
    @JsonProperty("number_of_seasons")
    private Integer numberOfSeasons;
    
    @JsonProperty("episode_run_time")
    private List<Integer> episodeRunTime;
    
    private String status;
    private String type;
    private String creator;
    private String genre;
    private List<String> networks;
    
    // Helper methods
    public String getPosterUrl() {
        return posterPath != null ? "https://image.tmdb.org/t/p/w500" + posterPath : null;
    }
    
    public String getBackdropUrl() {
        return backdropPath != null ? "https://image.tmdb.org/t/p/w1280" + backdropPath : null;
    }
    
    public Integer getFirstAirYear() {
        if (firstAirDate != null && firstAirDate.length() >= 4) {
            try {
                return Integer.parseInt(firstAirDate.substring(0, 4));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
    
    public Integer getLastAirYear() {
        if (lastAirDate != null && lastAirDate.length() >= 4) {
            try {
                return Integer.parseInt(lastAirDate.substring(0, 4));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
    
    public String getFormattedRating() {
        return voteAverage != null ? String.format("%.1f/10", voteAverage) : null;
    }
    
    public String getYearRange() {
        Integer firstYear = getFirstAirYear();
        Integer lastYear = getLastAirYear();
        
        if (firstYear != null && lastYear != null && !firstYear.equals(lastYear)) {
            return firstYear + "-" + lastYear;
        } else if (firstYear != null) {
            return firstYear.toString();
        }
        return null;
    }
    
    public Integer getAverageEpisodeRuntime() {
        if (episodeRunTime != null && !episodeRunTime.isEmpty()) {
            return episodeRunTime.stream().mapToInt(Integer::intValue).sum() / episodeRunTime.size();
        }
        return null;
    }
}