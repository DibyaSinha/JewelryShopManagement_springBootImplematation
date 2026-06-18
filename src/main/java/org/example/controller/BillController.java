package org.example.controller;

import jakarta.validation.Valid;
import org.example.dto.ApiResponse;
import org.example.dto.BillRequest;
import org.example.entity.Bill;
import org.example.entity.User;
import org.example.repository.UserRepository;
import org.example.service.BillingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bills")
public class BillController {

    @Autowired
    private BillingService billingService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping
    public ResponseEntity<ApiResponse<Bill>> generateBill(@Valid @RequestBody BillRequest request, Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName()).orElseThrow();
        Bill bill = billingService.generateBill(request, user.getId());
        return ResponseEntity.ok(ApiResponse.success(bill, "Bill generated successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Bill>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(billingService.getAllBills(), "Fetched bill history"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Bill>> getById(@PathVariable Long id) {
        Bill bill = billingService.getBillById(id);
        if (bill == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(ApiResponse.success(bill, "Fetched bill details"));
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> getPdf(@PathVariable Long id) {
        Bill bill = billingService.getBillById(id);
        if (bill == null || bill.getPdfData() == null) return ResponseEntity.notFound().build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=bill_" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bill.getPdfData());
    }
}
