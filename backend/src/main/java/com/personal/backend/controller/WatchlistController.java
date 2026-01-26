package com.personal.backend.controller;

import com.personal.backend.dto.AccountResponse;
import com.personal.backend.model.WatchlistItem;
import com.personal.backend.repository.WatchlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/watchlist")
@RequiredArgsConstructor
@Slf4j
public class WatchlistController {

    private final WatchlistRepository watchlistRepository;

    @GetMapping
    public ResponseEntity<AccountResponse<List<WatchlistItem>>> getWatchlist() {
        Long userId = 1L; // Admin
        List<WatchlistItem> items = watchlistRepository.findByUserIdOrderBySymbolAsc(userId);
        return ResponseEntity.ok(AccountResponse.success(items, "Watchlist retrieved"));
    }

    @PostMapping
    @Transactional
    public ResponseEntity<AccountResponse<WatchlistItem>> addToWatchlist(@RequestBody @Valid WatchlistItem item) {
        Long userId = 1L;
        // Check duplicate
        if (watchlistRepository.findByUserIdAndSymbol(userId, item.getSymbol()).isPresent()) {
            return ResponseEntity.badRequest().body(AccountResponse.error("Symbol already in watchlist"));
        }
        
        item.setUserId(userId);
        WatchlistItem saved = watchlistRepository.save(item);
        return ResponseEntity.ok(AccountResponse.success(saved, "Added to watchlist"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<AccountResponse<Void>> removeFromWatchlist(@PathVariable Long id) {
        watchlistRepository.deleteById(id);
        return ResponseEntity.ok(AccountResponse.success(null, "Removed from watchlist"));
    }
}
