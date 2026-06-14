package org.example.repository.impl;

import org.example.config.DatabaseConfig;
import org.example.exception.DatabaseException;
import org.example.model.DailyRate;
import org.example.repository.interfaces.DailyRateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DailyRateRepositoryImpl implements DailyRateRepository {
    private static final Logger logger = LoggerFactory.getLogger(DailyRateRepositoryImpl.class);

    @Override
    public void save(DailyRate rate) {
        String sql = "INSERT INTO daily_rates (metal_type, price_per_gram, rate_date) VALUES (?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE price_per_gram = VALUES(price_per_gram)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, rate.getMetalType());
            stmt.setDouble(2, rate.getPricePerGram());
            stmt.setDate(3, Date.valueOf(rate.getDate()));
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    rate.setId(rs.getLong(1));
                }
            }
            logger.info("Daily rate saved/updated: {} - {}", rate.getMetalType(), rate.getPricePerGram());
        } catch (SQLException e) {
            logger.error("Error saving daily rate", e);
            throw new DatabaseException("Failed to save daily rate", e);
        }
    }

    @Override
    public Optional<DailyRate> getTodayRate(String metalType) {
        String sql = "SELECT * FROM daily_rates WHERE metal_type = ? AND rate_date = CURDATE()";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, metalType);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToDailyRate(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error fetching today's rate for: {}", metalType, e);
            throw new DatabaseException("Failed to get today's rate", e);
        }
        return Optional.empty();
    }

    @Override
    public List<DailyRate> findAll() {
        List<DailyRate> rates = new ArrayList<>();
        String sql = "SELECT * FROM daily_rates ORDER BY rate_date DESC, metal_type ASC";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                rates.add(mapRowToDailyRate(rs));
            }
        } catch (SQLException e) {
            logger.error("Error listing all daily rates", e);
            throw new DatabaseException("Failed to list daily rates", e);
        }
        return rates;
    }

    private DailyRate mapRowToDailyRate(ResultSet rs) throws SQLException {
        DailyRate dr = new DailyRate(
                rs.getString("metal_type"),
                rs.getDouble("price_per_gram")
        );
        dr.setId(rs.getLong("id"));
        dr.setDate(rs.getDate("rate_date").toLocalDate());
        return dr;
    }
}
