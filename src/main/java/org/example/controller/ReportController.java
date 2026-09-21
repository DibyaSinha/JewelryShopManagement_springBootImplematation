package org.example.controller;

import org.example.dto.ApiResponse;
import org.example.service.BillingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
public class ReportController {

    @Autowired
    private BillingService billingService;

    @GetMapping("/today-total")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Double>> getTodayTotal() {
        return ResponseEntity.ok(ApiResponse.success(billingService.getTodayTotalSale(), "Fetched today's total sale"));
    }

    @GetMapping("/monthly-total")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Double>> getMonthlyTotal() {
        return ResponseEntity.ok(ApiResponse.success(billingService.getMonthlyTotalSale(), "Fetched monthly total sale"));
    }

    @GetMapping("/all-time-total")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Double>> getAllTimeTotal() {
        return ResponseEntity.ok(ApiResponse.success(billingService.getTotalSale(), "Fetched all-time total sale"));
    }

    @GetMapping("/today-by-seller")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Double>>> getTodayBySeller() {
        return ResponseEntity.ok(ApiResponse.success(billingService.getTodaySalesBySeller(), "Fetched today's sales breakdown by seller"));
    }

    @GetMapping("/monthly-breakdown")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Double>>> getMonthlyBreakdown() {
        return ResponseEntity.ok(ApiResponse.success(billingService.getMonthlySalesBreakdown(), "Fetched monthly sales breakdown"));
    }

    @GetMapping("/my-today-total")
    public ResponseEntity<ApiResponse<Double>> getMyTodayTotal(Authentication auth) {
        Map<String, Double> sales = billingService.getTodaySalesBySeller();
        Double myTotal = sales.getOrDefault(auth.getName(), 0.0);
        return ResponseEntity.ok(ApiResponse.success(myTotal, "Fetched your today's total sale"));
    }
}
