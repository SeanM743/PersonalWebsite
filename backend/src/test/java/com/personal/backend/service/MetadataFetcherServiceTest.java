package com.personal.backend.service;

import com.personal.backend.external.client.OpenLibraryClient;
import com.personal.backend.external.client.TMDBClient;
import com.personal.backend.external.model.BookMetadata;
import com.personal.backend.external.model.MovieMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MetadataFetcherServiceTest {

    @Mock
    private OpenLibraryClient openLibraryClient;

    @Mock
    private TMDBClient tmdbClient;

    @Mock
    private MetadataCacheManager cacheManager;

    @InjectMocks
    private MetadataFetcherService metadataFetcherService;

    @BeforeEach
    void setUp() {
        // Common setup if needed
    }

    @Test
    void testFetchMovieMetadata_Success() {
        // Arrange
        String movieTitle = "Inception";
        MovieMetadata mockMovie = MovieMetadata.builder()
                .title("Inception")
                .id(12345L)
                .overview("Dreams within dreams")
                .build();

        // Cache miss
        when(cacheManager.generateMovieCacheKey(movieTitle)).thenReturn("MOVIE_Inception");
        when(cacheManager.getCachedMovieMetadata("MOVIE_Inception")).thenReturn(Optional.empty());

        // TMDB hit
        when(tmdbClient.searchMovie(movieTitle)).thenReturn(Optional.of(mockMovie));

        // Act
        Optional<MovieMetadata> result = metadataFetcherService.fetchMovieMetadata(movieTitle);

        // Assert
        assertTrue(result.isPresent());
        assertEquals("Inception", result.get().getTitle());
        
        // Verify interactions
        verify(tmdbClient, times(1)).searchMovie(movieTitle);
        verify(cacheManager, times(1)).cacheMovieMetadata("MOVIE_Inception", mockMovie);
    }

    @Test
    void testFetchMovieMetadata_Cached() {
        // Arrange
        String movieTitle = "The Matrix";
        MovieMetadata cachedMovie = MovieMetadata.builder()
                .title("The Matrix")
                .build();

        // Cache hit
        when(cacheManager.generateMovieCacheKey(movieTitle)).thenReturn("MOVIE_The_Matrix");
        when(cacheManager.getCachedMovieMetadata("MOVIE_The_Matrix")).thenReturn(Optional.of(cachedMovie));

        // Act
        Optional<MovieMetadata> result = metadataFetcherService.fetchMovieMetadata(movieTitle);

        // Assert
        assertTrue(result.isPresent());
        assertEquals("The Matrix", result.get().getTitle());
        
        // TMDB should NOT be called
        verify(tmdbClient, never()).searchMovie(anyString());
    }

    @Test
    void testFetchMovieMetadata_NotFound() {
        // Arrange
        String movieTitle = "NonExistentMovie123";

        // Cache miss
        when(cacheManager.generateMovieCacheKey(movieTitle)).thenReturn("MOVIE_NonExistent");
        when(cacheManager.getCachedMovieMetadata("MOVIE_NonExistent")).thenReturn(Optional.empty());

        // TMDB miss
        when(tmdbClient.searchMovie(movieTitle)).thenReturn(Optional.empty());

        // Act
        Optional<MovieMetadata> result = metadataFetcherService.fetchMovieMetadata(movieTitle);

        // Assert
        assertFalse(result.isPresent());
        
        // Should verify nothing was cached
        verify(cacheManager, never()).cacheMovieMetadata(anyString(), any());
    }
}
