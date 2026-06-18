package org.example.controller;

import org.example.dto.ApiResponse;
import org.example.entity.Customer;
import org.example.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Customer>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(customerService.getAllCustomers(), "Fetched all customers"));
    }

    @GetMapping("/{mobile}")
    public ResponseEntity<ApiResponse<Customer>> getByMobile(@PathVariable String mobile) {
        Customer customer = customerService.getCustomerByMobile(mobile);
        if (customer == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(ApiResponse.success(customer, "Fetched customer details"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Customer>> addOrUpdate(@RequestBody Customer customer) {
        return ResponseEntity.ok(ApiResponse.success(customerService.addOrUpdateCustomer(customer), "Customer saved"));
    }

    @DeleteMapping("/{mobile}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String mobile) {
        customerService.deleteCustomer(mobile);
        return ResponseEntity.ok(ApiResponse.success(null, "Customer deleted"));
    }
}
