package org.example.service;

import org.example.entity.Customer;
import org.example.repository.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CustomerService {
    private static final Logger logger = LoggerFactory.getLogger(CustomerService.class);

    @Autowired
    private CustomerRepository customerRepository;

    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    public Customer getCustomerByMobile(String mobile) {
        return customerRepository.findById(mobile).orElse(null);
    }

    @Transactional
    public Customer addOrUpdateCustomer(Customer customer) {
        if (customer.getDiscountPercent() == null) {
            customer.setDiscountPercent(0.0);
        }
        boolean isNew = !customerRepository.existsById(customer.getMobileNumber());
        Customer saved = customerRepository.save(customer);
        logger.info("Customer record {} in database: Name='{}', Discount={}%", isNew ? "created" : "updated", saved.getName(), saved.getDiscountPercent());
        return saved;
    }

    @Transactional
    public void deleteCustomer(String mobile) {
        customerRepository.deleteById(mobile);
        logger.info("Customer record deleted from database");
    }
}
