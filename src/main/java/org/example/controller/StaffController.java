package org.example.controller;

import org.example.dto.ApiResponse;
import org.example.entity.Staff;
import org.example.service.StaffService;
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
        return ResponseEntity.ok(ApiResponse.success(staffService.createStaff(staff), "Staff created successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Staff>> updateStaff(@PathVariable Long id, @RequestBody Staff staff) {
        return ResponseEntity.ok(ApiResponse.success(staffService.updateStaff(id, staff), "Staff updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteStaff(@PathVariable Long id) {
        staffService.deleteStaff(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Staff deleted successfully"));
    }

    @PutMapping("/{id}/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String newPassword = payload.get("newPassword");
        staffService.resetPassword(id, newPassword);
        return ResponseEntity.ok(ApiResponse.success(null, "Password reset successfully"));
    }
}
