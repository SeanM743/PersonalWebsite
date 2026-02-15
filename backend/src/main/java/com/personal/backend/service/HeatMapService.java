package com.personal.backend.service;

import com.personal.backend.dto.HeatMapDTO;
import com.personal.backend.dto.HeatMapDTO.HeatMapSector;
import com.personal.backend.dto.HeatMapDTO.HeatMapStock;
import com.personal.backend.dto.MarketData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class HeatMapService {

    private final YahooFinanceService yahooFinanceService;

    // Cached result (refreshed at most once per request, real caching via Spring @Cacheable if desired)
    private HeatMapDTO cachedHeatMap;
    private long cacheTimestamp = 0;
    private static final long CACHE_TTL_MS = 15 * 60 * 1000; // 15 minutes

    /**
     * Curated list of top S&P 500 stocks by sector.
     * Each entry: { symbol, displayName, approximateMarketCapBillions }
     */
    private static final Map<String, List<String[]>> SECTOR_STOCKS = new LinkedHashMap<>();

    static {
        SECTOR_STOCKS.put("Technology", Arrays.asList(
                new String[]{"AAPL", "Apple", "3400"},
                new String[]{"MSFT", "Microsoft", "3100"},
                new String[]{"NVDA", "NVIDIA", "2900"},
                new String[]{"AVGO", "Broadcom", "800"},
                new String[]{"ORCL", "Oracle", "460"},
                new String[]{"CRM", "Salesforce", "300"},
                new String[]{"AMD", "AMD", "250"},
                new String[]{"INTC", "Intel", "110"},
                new String[]{"CSCO", "Cisco", "230"},
                new String[]{"IBM", "IBM", "200"},
                new String[]{"QCOM", "Qualcomm", "190"},
                new String[]{"TXN", "Texas Instruments", "180"},
                new String[]{"INTU", "Intuit", "180"},
                new String[]{"NOW", "ServiceNow", "170"},
                new String[]{"ADI", "Analog Devices", "110"},
                new String[]{"SNPS", "Synopsys", "80"},
                new String[]{"MU", "Micron", "100"}
        ));

        SECTOR_STOCKS.put("Communication Services", Arrays.asList(
                new String[]{"GOOG", "Alphabet", "2200"},
                new String[]{"META", "Meta", "1600"},
                new String[]{"NFLX", "Netflix", "380"},
                new String[]{"DIS", "Disney", "200"},
                new String[]{"TMUS", "T-Mobile", "250"},
                new String[]{"VZ", "Verizon", "170"},
                new String[]{"T", "AT&T", "160"}
        ));

        SECTOR_STOCKS.put("Consumer Cyclical", Arrays.asList(
                new String[]{"AMZN", "Amazon", "2200"},
                new String[]{"TSLA", "Tesla", "800"},
                new String[]{"HD", "Home Depot", "380"},
                new String[]{"LOW", "Lowe's", "150"},
                new String[]{"MCD", "McDonald's", "210"},
                new String[]{"SBUX", "Starbucks", "110"},
                new String[]{"NKE", "Nike", "100"},
                new String[]{"TJX", "TJX", "130"}
        ));

        SECTOR_STOCKS.put("Consumer Defensive", Arrays.asList(
                new String[]{"WMT", "Walmart", "600"},
                new String[]{"PG", "Procter & Gamble", "380"},
                new String[]{"COST", "Costco", "380"},
                new String[]{"KO", "Coca-Cola", "270"},
                new String[]{"PEP", "PepsiCo", "220"},
                new String[]{"PM", "Philip Morris", "200"},
                new String[]{"CL", "Colgate-Palmolive", "80"}
        ));

        SECTOR_STOCKS.put("Healthcare", Arrays.asList(
                new String[]{"LLY", "Eli Lilly", "780"},
                new String[]{"UNH", "UnitedHealth", "510"},
                new String[]{"JNJ", "Johnson & Johnson", "380"},
                new String[]{"ABBV", "AbbVie", "330"},
                new String[]{"MRK", "Merck", "250"},
                new String[]{"TMO", "Thermo Fisher", "210"},
                new String[]{"ABT", "Abbott Labs", "200"},
                new String[]{"PFE", "Pfizer", "150"},
                new String[]{"AMGN", "Amgen", "160"}
        ));

        SECTOR_STOCKS.put("Financial", Arrays.asList(
                new String[]{"BRK-B", "Berkshire Hathaway", "900"},
                new String[]{"JPM", "JPMorgan", "650"},
                new String[]{"V", "Visa", "580"},
                new String[]{"MA", "Mastercard", "450"},
                new String[]{"BAC", "Bank of America", "330"},
                new String[]{"WFC", "Wells Fargo", "220"},
                new String[]{"GS", "Goldman Sachs", "170"},
                new String[]{"MS", "Morgan Stanley", "170"},
                new String[]{"AXP", "American Express", "180"},
                new String[]{"SPGI", "S&P Global", "150"},
                new String[]{"BLK", "BlackRock", "150"}
        ));

        SECTOR_STOCKS.put("Industrials", Arrays.asList(
                new String[]{"GE", "GE Aerospace", "220"},
                new String[]{"CAT", "Caterpillar", "190"},
                new String[]{"RTX", "RTX Corp", "160"},
                new String[]{"HON", "Honeywell", "140"},
                new String[]{"UNP", "Union Pacific", "150"},
                new String[]{"BA", "Boeing", "130"},
                new String[]{"DE", "Deere & Co", "120"},
                new String[]{"LMT", "Lockheed Martin", "120"}
        ));

        SECTOR_STOCKS.put("Energy", Arrays.asList(
                new String[]{"XOM", "ExxonMobil", "500"},
                new String[]{"CVX", "Chevron", "280"},
                new String[]{"COP", "ConocoPhillips", "140"},
                new String[]{"SLB", "Schlumberger", "65"},
                new String[]{"EOG", "EOG Resources", "70"}
        ));

        SECTOR_STOCKS.put("Real Estate", Arrays.asList(
                new String[]{"PLD", "Prologis", "110"},
                new String[]{"AMT", "American Tower", "95"},
                new String[]{"EQIX", "Equinix", "80"},
                new String[]{"SPG", "Simon Property", "55"}
        ));

        SECTOR_STOCKS.put("Materials", Arrays.asList(
                new String[]{"LIN", "Linde", "220"},
                new String[]{"APD", "Air Products", "65"},
                new String[]{"SHW", "Sherwin-Williams", "90"},
                new String[]{"FCX", "Freeport-McMoRan", "60"}
        ));

        SECTOR_STOCKS.put("Utilities", Arrays.asList(
                new String[]{"NEE", "NextEra Energy", "160"},
                new String[]{"SO", "Southern Co", "95"},
                new String[]{"DUK", "Duke Energy", "85"},
                new String[]{"D", "Dominion Energy", "45"}
        ));
    }

    public HeatMapDTO getHeatMapData() {
        // Return cached data if fresh enough
        if (cachedHeatMap != null && (System.currentTimeMillis() - cacheTimestamp) < CACHE_TTL_MS) {
            return cachedHeatMap;
        }

        log.info("Fetching heat map data for {} sectors", SECTOR_STOCKS.size());

        // Collect all symbols
        List<String> allSymbols = SECTOR_STOCKS.values().stream()
                .flatMap(List::stream)
                .map(arr -> arr[0])
                .collect(Collectors.toList());

        // Batch fetch market data
        Map<String, MarketData> marketDataMap = yahooFinanceService.getBatchMarketData(allSymbols);

        // Build sector data
        List<HeatMapSector> sectors = new ArrayList<>();
        for (Map.Entry<String, List<String[]>> entry : SECTOR_STOCKS.entrySet()) {
            String sectorName = entry.getKey();
            List<String[]> stockDefs = entry.getValue();

            List<HeatMapStock> stocks = new ArrayList<>();
            for (String[] def : stockDefs) {
                String symbol = def[0];
                String name = def[1];
                long weight = Long.parseLong(def[2]);

                MarketData md = marketDataMap.get(symbol);
                BigDecimal changePercent = BigDecimal.ZERO;
                BigDecimal price = BigDecimal.ZERO;

                if (md != null) {
                    if (md.getDailyChangePercentage() != null) {
                        changePercent = md.getDailyChangePercentage();
                    } else if (md.calculateDailyChangePercentage() != null) {
                        changePercent = md.calculateDailyChangePercentage();
                    }
                    if (md.getCurrentPrice() != null) {
                        price = md.getCurrentPrice();
                    }
                    // Use actual company name from Yahoo if available
                    if (md.getCompanyName() != null && !md.getCompanyName().isBlank()) {
                        name = md.getCompanyName();
                    }
                }

                stocks.add(HeatMapStock.builder()
                        .symbol(symbol)
                        .name(name)
                        .price(price)
                        .changePercent(changePercent)
                        .weight(weight)
                        .build());
            }

            sectors.add(HeatMapSector.builder()
                    .name(sectorName)
                    .stocks(stocks)
                    .build());
        }

        cachedHeatMap = HeatMapDTO.builder().sectors(sectors).build();
        cacheTimestamp = System.currentTimeMillis();

        log.info("Heat map data fetched successfully: {} sectors, {} total stocks",
                sectors.size(), allSymbols.size());

        return cachedHeatMap;
    }

    /**
     * Build heat map data for a custom list of symbols (e.g. user portfolio or watchlist).
     * Groups all stocks under a single sector with equal weighting.
     */
    public HeatMapDTO getCustomHeatMapData(List<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            return HeatMapDTO.builder().sectors(List.of()).build();
        }

        log.info("Fetching custom heat map data for {} symbols", symbols.size());

        Map<String, MarketData> marketDataMap = yahooFinanceService.getBatchMarketData(symbols);

        List<HeatMapStock> stocks = new ArrayList<>();
        for (String symbol : symbols) {
            MarketData md = marketDataMap.get(symbol.toUpperCase());
            String name = symbol;
            BigDecimal changePercent = BigDecimal.ZERO;
            BigDecimal price = BigDecimal.ZERO;

            if (md != null) {
                if (md.getCompanyName() != null && !md.getCompanyName().isBlank()) {
                    name = md.getCompanyName();
                }
                if (md.getDailyChangePercentage() != null) {
                    changePercent = md.getDailyChangePercentage();
                } else if (md.calculateDailyChangePercentage() != null) {
                    changePercent = md.calculateDailyChangePercentage();
                }
                if (md.getCurrentPrice() != null) {
                    price = md.getCurrentPrice();
                }
            }

            stocks.add(HeatMapStock.builder()
                    .symbol(symbol.toUpperCase())
                    .name(name)
                    .price(price)
                    .changePercent(changePercent)
                    .weight(100) // equal weighting
                    .build());
        }

        HeatMapSector sector = HeatMapSector.builder()
                .name("Custom")
                .stocks(stocks)
                .build();

        return HeatMapDTO.builder().sectors(List.of(sector)).build();
    }
}
