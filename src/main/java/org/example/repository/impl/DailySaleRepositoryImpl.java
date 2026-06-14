package org.example.repository.impl;

import org.example.config.DatabaseConfig;
import org.example.exception.DatabaseException;
import org.example.repository.interfaces.DailySaleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;

public class DailySaleRepositoryImpl implements DailySaleRepository {
    private static final Logger logger = LoggerFactory.getLogger(DailySaleRepositoryImpl.class);

    @Override
    public void addSaleAmount(LocalDate date, double amount, Connection conn) {
        String sql = "INSERT INTO daily_sales (sale_date, total_revenue) VALUES (?, ?) " +
                     "ON DUPLICATE KEY UPDATE total_revenue = total_revenue + VALUES(total_revenue)";
        
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement(sql);
            stmt.setDate(1, Date.valueOf(date));
            stmt.setDouble(2, amount);
            stmt.executeUpdate();
            logger.info("Added sale amount {} for date: {}", amount, date);
        } catch (SQLException e) {
            logger.error("Error adding sale amount for date: {}", date, e);
            throw new DatabaseException("Failed to update daily sales", e);
        } finally {
            if (stmt != null) try { stmt.close(); } catch (SQLException ignored) {}
        }
    }

    @Override
    public double getSaleForDate(LocalDate date) {
        String sql = "SELECT total_revenue FROM daily_sales WHERE sale_date = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(date));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("total_revenue");
                }
            }
        } catch (SQLException e) {
            logger.error("Error fetching sale for date: {}", date, e);
            throw new DatabaseException("Failed to fetch daily sale", e);
        }
        return 0.0;
    }

    @Override
    public double getMonthlySale() {
        String sql = "SELECT SUM(total_revenue) FROM daily_sales WHERE MONTH(sale_date) = MONTH(CURDATE()) AND YEAR(sale_date) = YEAR(CURDATE())";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getDouble(1);
            }
        } catch (SQLException e) {
            logger.error("Error fetching monthly sale", e);
            throw new DatabaseException("Failed to fetch monthly sale", e);
        }
        return 0.0;
    }

    @Override
    public double getTotalSale() {
        String sql = "SELECT SUM(total_revenue) FROM daily_sales";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getDouble(1);
            }
        } catch (SQLException e) {
            logger.error("Error fetching total sale", e);
            throw new DatabaseException("Failed to fetch total sale", e);
        }
        return 0.0;
    }
}
