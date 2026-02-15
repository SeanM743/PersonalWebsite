package com.personal.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HeatMapDTO {

    private List<HeatMapSector> sectors;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HeatMapSector {
        private String name;
        private List<HeatMapStock> stocks;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HeatMapStock {
        private String symbol;
        private String name;
        private BigDecimal price;
        private BigDecimal changePercent;
        private long weight; // approximate market cap in billions, used for treemap sizing
    }
}
