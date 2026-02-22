package com.personal.backend.service;

import com.personal.backend.dto.WeatherResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherService {

    private final RestTemplate restTemplate;

    @Value("${weather.api.key}")
    private String apiKey;

    @Value("${weather.api.base.url}")
    private String baseUrl;

    @Value("${weather.api.default.location}")
    private String defaultLocation;

    @Cacheable(value = "weather", key = "#location != null ? #location : 'default'", unless = "#result == null")
    public WeatherResponse getWeather(String location) {
        String queryLocation = (location != null && !location.trim().isEmpty()) ? location : defaultLocation;
        
        if (apiKey == null || apiKey.trim().isEmpty() || apiKey.equals("${OPENWEATHERMAP_API_KEY:}")) {
            log.warn("OpenWeatherMap API key is not configured. Weather widget will not function properly.");
            // Return a mock/empty response to prevent UI crashes if key is missing
            return createMockResponse(queryLocation);
        }

        try {
            String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                    .queryParam("q", queryLocation)
                    .queryParam("appid", apiKey)
                    .queryParam("units", "imperial") // Use imperial (Fahrenheit) by default
                    .toUriString();

            log.debug("Fetching weather data for location: {}", queryLocation);
            
            // Using a raw Map for parsing to avoid creating complex DTOs for the OpenWeather API response
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            if (response != null) {
                return parseWeatherResponse(response);
            }
        } catch (HttpClientErrorException.NotFound e) {
            log.error("Weather location not found: {}", queryLocation);
            throw new RuntimeException("Location not found: " + queryLocation);
        } catch (Exception e) {
            log.error("Error fetching weather data for {}: {}", queryLocation, e.getMessage());
        }
        
        return createMockResponse(queryLocation);
    }

    private WeatherResponse parseWeatherResponse(Map<String, Object> response) {
        @SuppressWarnings("unchecked")
        Map<String, Number> main = (Map<String, Number>) response.get("main");
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> weatherList = (List<Map<String, Object>>) response.get("weather");
        Map<String, Object> weather = weatherList != null && !weatherList.isEmpty() ? weatherList.get(0) : null;
        
        @SuppressWarnings("unchecked")
        Map<String, Object> sys = (Map<String, Object>) response.get("sys");

        String description = weather != null ? (String) weather.get("description") : "Unknown";
        String icon = weather != null ? (String) weather.get("icon") : "01d";
        
        // Capitalize description
        if (description != null && !description.isEmpty()) {
            description = description.substring(0, 1).toUpperCase() + description.substring(1);
        }

        return WeatherResponse.builder()
                .temperature(main.get("temp").doubleValue())
                .feelsLike(main.get("feels_like").doubleValue())
                .tempMin(main.get("temp_min").doubleValue())
                .tempMax(main.get("temp_max").doubleValue())
                .humidity(main.get("humidity").intValue())
                .description(description)
                .icon(icon)
                .locationName((String) response.get("name"))
                .country(sys != null ? (String) sys.get("country") : "")
                .timestamp(System.currentTimeMillis())
                .build();
    }
    
    private WeatherResponse createMockResponse(String location) {
        return WeatherResponse.builder()
                .temperature(0.0)
                .feelsLike(0.0)
                .tempMin(0.0)
                .tempMax(0.0)
                .humidity(0)
                .description("Weather Unavailable (Configure API Key)")
                .icon("01d")
                .locationName(location.split(",")[0])
                .country("")
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
