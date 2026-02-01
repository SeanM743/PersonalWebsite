package com.personal.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class FinnhubCandleResponse {
    @JsonProperty("c")
    private List<Double> closePrices;
    
    @JsonProperty("t")
    private List<Long> timestamps;
    
    @JsonProperty("s")
    private String status;
}
