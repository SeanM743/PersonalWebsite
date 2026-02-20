package com.personal.backend.service;

import com.personal.backend.dto.HeatMapDTO;
import com.personal.backend.dto.MarketData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HeatMapServiceTest {

    @Mock
    private YahooFinanceService yahooFinanceService;

    @InjectMocks
    private HeatMapService heatMapService;

    @Test
    void testGetHeatMapData() {
        MarketData aaplData = new MarketData();
        aaplData.setSymbol("AAPL");
        aaplData.setCurrentPrice(new BigDecimal("150.00"));
        aaplData.setDailyChangePercentage(new BigDecimal("2.5"));
        aaplData.setCompanyName("Apple Inc.");
        
        Map<String, MarketData> mockCache = new HashMap<>();
        mockCache.put("AAPL", aaplData);
        
        when(yahooFinanceService.getBatchMarketData(anyList())).thenReturn(mockCache);

        HeatMapDTO heatMap = heatMapService.getHeatMapData();

        assertNotNull(heatMap);
        assertFalse(heatMap.getSectors().isEmpty());
        // Verify technology sector exists and contains Apple
        boolean foundAapl = heatMap.getSectors().stream()
                .filter(s -> s.getName().equals("Technology"))
                .flatMap(s -> s.getStocks().stream())
                .anyMatch(st -> st.getSymbol().equals("AAPL") && st.getPrice().compareTo(new BigDecimal("150.00")) == 0);
                
        assertTrue(foundAapl);
    }

    @Test
    void testGetCustomHeatMapData() {
        MarketData msftData = new MarketData();
        msftData.setSymbol("MSFT");
        msftData.setCurrentPrice(new BigDecimal("300.00"));
        msftData.setDailyChangePercentage(new BigDecimal("-1.5"));
        msftData.setCompanyName("Microsoft Corp");
        
        Map<String, MarketData> mockCache = new HashMap<>();
        mockCache.put("MSFT", msftData);
        
        when(yahooFinanceService.getBatchMarketData(anyList())).thenReturn(mockCache);

        List<Map<String, Object>> customEntries = List.of(
                Map.of("symbol", "MSFT", "weight", 2000)
        );

        HeatMapDTO heatMap = heatMapService.getCustomHeatMapData(customEntries);

        assertNotNull(heatMap);
        assertEquals(1, heatMap.getSectors().size());
        assertEquals("Custom", heatMap.getSectors().get(0).getName());
        assertEquals(1, heatMap.getSectors().get(0).getStocks().size());
        assertEquals("MSFT", heatMap.getSectors().get(0).getStocks().get(0).getSymbol());
        assertEquals(2000L, heatMap.getSectors().get(0).getStocks().get(0).getWeight());
        assertEquals(new BigDecimal("-1.5"), heatMap.getSectors().get(0).getStocks().get(0).getChangePercent());
    }
}
