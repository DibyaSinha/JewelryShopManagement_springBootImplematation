package org.example.service;

import org.example.exception.DatabaseException;
import org.example.model.Customer;
import org.example.repository.impl.CustomerRepositoryImpl;
import org.example.repository.interfaces.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomerService {
    private static final Logger logger = LoggerFactory.getLogger(CustomerService.class);
    private CustomerRepository repo = new CustomerRepositoryImpl();

    public void addOrUpdateCustomer(String mobileNumber, String name, double discountPercent) {
        if (mobileNumber == null || mobileNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Mobile number cannot be empty.");
        }
        Customer customer = new Customer(mobileNumber.trim(), name, discountPercent);
        repo.save(customer);
        logger.info("Customer {} processed.", mobileNumber);
    }

    public Customer getCustomerByMobile(String mobileNumber) {
        if (mobileNumber == null || mobileNumber.trim().isEmpty()) {
            return null;
        }
        return repo.findByMobile(mobileNumber.trim()).orElse(null);
    }
}
