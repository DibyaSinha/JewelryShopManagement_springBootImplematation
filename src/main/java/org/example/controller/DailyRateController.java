package org.example.controller;

import org.example.dto.ApiResponse;
import org.example.entity.DailyRate;
import org.example.entity.Jewelry;
import org.example.service.DailyRateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rates")
public class DailyRateController {

    @Autowired
    private DailyRateService dailyRateService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<DailyRate>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(dailyRateService.getAllRates(), "Fetched all rates"));
    }

    @GetMapping("/today/{type}")
    public ResponseEntity<ApiResponse<DailyRate>> getToday(@PathVariable Jewelry.MetalType type) {
        return dailyRateService.getTodayRate(type)
                .map(rate -> ResponseEntity.ok(ApiResponse.success(rate, "Fetched today's rate")))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DailyRate>> update(@RequestBody DailyRate rate) {
        return ResponseEntity.ok(ApiResponse.success(dailyRateService.saveRate(rate), "Daily rate updated"));
    }
}
