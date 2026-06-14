package org.example.repository.impl;

import org.example.config.DatabaseConfig;
import org.example.exception.DatabaseException;
import org.example.model.Customer;
import org.example.repository.interfaces.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.Optional;

public class CustomerRepositoryImpl implements CustomerRepository {
    private static final Logger logger = LoggerFactory.getLogger(CustomerRepositoryImpl.class);

    @Override
    public void save(Customer customer) {
        String sql = "INSERT INTO customers (mobile_number, name, discount_percent) VALUES (?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE name = VALUES(name), discount_percent = VALUES(discount_percent)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, customer.getMobileNumber());
            stmt.setString(2, customer.getName());
            stmt.setDouble(3, customer.getDiscountPercent());
            stmt.executeUpdate();
            logger.info("Customer saved/updated: {}", customer.getMobileNumber());
        } catch (SQLException e) {
            logger.error("Error saving customer", e);
            throw new DatabaseException("Failed to save customer", e);
        }
    }

    @Override
    public Optional<Customer> findByMobile(String mobileNumber) {
        String sql = "SELECT * FROM customers WHERE mobile_number = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, mobileNumber);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Customer customer = new Customer(
                            rs.getString("mobile_number"),
                            rs.getString("name"),
                            rs.getDouble("discount_percent")
                    );
                    customer.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    customer.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                    return Optional.of(customer);
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding customer by mobile", e);
            throw new DatabaseException("Failed to find customer", e);
        }
        return Optional.empty();
    }
}
