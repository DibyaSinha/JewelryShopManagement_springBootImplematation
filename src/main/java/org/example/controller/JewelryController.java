package org.example.controller;

import org.example.dto.ApiResponse;
import org.example.entity.Jewelry;
import org.example.service.JewelryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jewelry")
public class JewelryController {

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
        return ResponseEntity.ok(ApiResponse.success(jewelryService.addJewelry(jewelry), "Jewelry added"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Jewelry>> update(@PathVariable Long id, @RequestBody Jewelry jewelry) {
        return ResponseEntity.ok(ApiResponse.success(jewelryService.updateJewelry(id, jewelry), "Jewelry updated"));
    }

    @PostMapping("/{id}/stock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Jewelry>> addStock(@PathVariable Long id, @RequestParam Integer quantity) {
        return ResponseEntity.ok(ApiResponse.success(jewelryService.addStock(id, quantity), "Stock updated"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        jewelryService.deleteJewelry(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Jewelry deleted"));
    }
}
