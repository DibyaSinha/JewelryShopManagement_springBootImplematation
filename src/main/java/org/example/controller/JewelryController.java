package org.example.controller;

import org.example.dto.ApiResponse;
import org.example.entity.Jewelry;
import org.example.service.JewelryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jewelry")
public class JewelryController {
    private static final Logger logger = LoggerFactory.getLogger(JewelryController.class);

    @Autowired
    private JewelryService jewelryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Jewelry>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(jewelryService.getAllJewelry(), "Fetched all jewelry"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Jewelry>> getById(@PathVariable Long id) {
        Jewelry jewelry = jewelryService.getJewelryById(id);
        if (jewelry == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(ApiResponse.success(jewelry, "Fetched jewelry details"));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Jewelry>> add(@RequestBody Jewelry jewelry) {
        Jewelry saved = jewelryService.addJewelry(jewelry);
        logger.info("Jewelry design added: '{}' (Type: {}, Weight: {}g)", saved.getName(), saved.getType(), saved.getWeight());
        return ResponseEntity.ok(ApiResponse.success(saved, "Jewelry added"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Jewelry>> update(@PathVariable Long id, @RequestBody Jewelry jewelry) {
        Jewelry updated = jewelryService.updateJewelry(id, jewelry);
        logger.info("Jewelry design updated: ID {}", id);
        return ResponseEntity.ok(ApiResponse.success(updated, "Jewelry updated"));
    }

    @PostMapping("/{id}/stock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Jewelry>> addStock(@PathVariable Long id, @RequestParam Integer quantity) {
        Jewelry updated = jewelryService.addStock(id, quantity);
        logger.info("Stock updated for Jewelry ID: {} (Added: {}, New Stock: {})", id, quantity, updated != null ? updated.getStock() : "N/A");
        return ResponseEntity.ok(ApiResponse.success(updated, "Stock updated"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        jewelryService.deleteJewelry(id);
        logger.info("Jewelry design deleted: ID {}", id);
        return ResponseEntity.ok(ApiResponse.success(null, "Jewelry deleted"));
    }
}
