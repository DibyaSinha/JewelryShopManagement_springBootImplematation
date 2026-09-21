package org.example.controller;

import org.example.dto.ApiResponse;
import org.example.entity.Customer;
import org.example.service.CustomerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {
    private static final Logger logger = LoggerFactory.getLogger(CustomerController.class);

    @Autowired
    private CustomerService customerService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Customer>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(customerService.getAllCustomers(), "Fetched all customers"));
    }

    @GetMapping("/{mobile}")
    public ResponseEntity<ApiResponse<Customer>> getByMobile(@PathVariable String mobile) {
        logger.info("Customer searched by mobile number");
        Customer customer = customerService.getCustomerByMobile(mobile);
        if (customer == null) {
            logger.info("Customer not found for searched mobile number");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Customer not found"));
        }
        logger.info("Customer found: Name='{}', Discount={}%", customer.getName(), customer.getDiscountPercent());
        return ResponseEntity.ok(ApiResponse.success(customer, "Fetched customer details"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Customer>> addOrUpdate(@RequestBody Customer customer) {
        Customer saved = customerService.addOrUpdateCustomer(customer);
        logger.info("Customer created/updated successfully: Name='{}'", saved.getName());
        return ResponseEntity.ok(ApiResponse.success(saved, "Customer saved"));
    }

    @DeleteMapping("/{mobile}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String mobile) {
        customerService.deleteCustomer(mobile);
        logger.info("Customer deleted successfully");
        return ResponseEntity.ok(ApiResponse.success(null, "Customer deleted"));
    }
}
