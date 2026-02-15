package com.personal.backend.controller;

import com.personal.backend.dto.HeatMapDTO;
import com.personal.backend.service.HeatMapService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/market")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class HeatMapController {

    private final HeatMapService heatMapService;

    @GetMapping("/heatmap")
    public ResponseEntity<HeatMapDTO> getHeatMapData() {
        return ResponseEntity.ok(heatMapService.getHeatMapData());
    }

    @PostMapping("/heatmap/custom")
    public ResponseEntity<HeatMapDTO> getCustomHeatMapData(@RequestBody List<java.util.Map<String, Object>> entries) {
        return ResponseEntity.ok(heatMapService.getCustomHeatMapData(entries));
    }
}
