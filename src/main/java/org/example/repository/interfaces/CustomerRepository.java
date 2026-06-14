package org.example.repository.interfaces;

import org.example.model.Customer;
import java.util.Optional;

public interface CustomerRepository {
    void save(Customer customer);
    Optional<Customer> findByMobile(String mobileNumber);
}
