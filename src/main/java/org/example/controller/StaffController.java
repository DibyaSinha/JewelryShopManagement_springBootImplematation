package org.example.controller;

import org.example.dto.ApiResponse;
import org.example.entity.Staff;
import org.example.service.StaffService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/staff")
@PreAuthorize("hasRole('ADMIN')")
public class StaffController {
    private static final Logger logger = LoggerFactory.getLogger(StaffController.class);

    @Autowired
    private StaffService staffService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Staff>>> getAllStaff() {
        return ResponseEntity.ok(ApiResponse.success(staffService.getAllStaff(), "Fetched all staff"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Staff>> getStaffById(@PathVariable Long id) {
        Staff staff = staffService.getStaffById(id);
        if (staff == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(ApiResponse.success(staff, "Fetched staff details"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Staff>> createStaff(@RequestBody Staff staff) {
        Staff created = staffService.createStaff(staff);
        logger.info("Staff member created successfully: Name='{}' (ID: {})", created.getName(), created.getId());
        return ResponseEntity.ok(ApiResponse.success(created, "Staff created successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Staff>> updateStaff(@PathVariable Long id, @RequestBody Staff staff) {
        Staff updated = staffService.updateStaff(id, staff);
        logger.info("Staff member updated successfully: ID {}", id);
        return ResponseEntity.ok(ApiResponse.success(updated, "Staff updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteStaff(@PathVariable Long id) {
        staffService.deleteStaff(id);
        logger.info("Staff member deleted successfully: ID {}", id);
        return ResponseEntity.ok(ApiResponse.success(null, "Staff deleted successfully"));
    }

    @PutMapping("/{id}/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String newPassword = payload.get("newPassword");
        staffService.resetPassword(id, newPassword);
        logger.info("Password reset successfully for staff ID: {}", id);
        return ResponseEntity.ok(ApiResponse.success(null, "Password reset successfully"));
    }
}
