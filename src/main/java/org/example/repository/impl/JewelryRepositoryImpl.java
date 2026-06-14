package org.example.repository.impl;

import org.example.config.DatabaseConfig;
import org.example.exception.DatabaseException;
import org.example.model.Jewelry;
import org.example.repository.interfaces.JewelryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JewelryRepositoryImpl implements JewelryRepository {
    private static final Logger logger = LoggerFactory.getLogger(JewelryRepositoryImpl.class);

    @Override
    public void save(Jewelry jewelry) {
        String sql = "INSERT INTO jewelry (name, company_name, type, weight, stock, making_percent) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, jewelry.getName());
            stmt.setString(2, jewelry.getCompanyName());
            stmt.setString(3, jewelry.getType());
            stmt.setDouble(4, jewelry.getWeight());
            stmt.setInt(5, jewelry.getStock());
            stmt.setDouble(6, jewelry.getMakingPercent());
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    jewelry.setId(rs.getLong(1));
                }
            }
            logger.info("Jewelry saved: {}", jewelry.getName());
        } catch (SQLException e) {
            logger.error("Error saving jewelry", e);
            throw new DatabaseException("Failed to save jewelry", e);
        }
    }

    @Override
    public void updateStock(long id, int newStock, Connection conn) {
        String sql = "UPDATE jewelry SET stock = ? WHERE id = ?";
        boolean isInternalConn = (conn == null);
        Connection currentConn = null;
        PreparedStatement stmt = null;
        try {
            currentConn = isInternalConn ? DatabaseConfig.getConnection() : conn;
            stmt = currentConn.prepareStatement(sql);
            stmt.setInt(1, newStock);
            stmt.setLong(2, id);
            int affected = stmt.executeUpdate();
            if (affected == 0) throw new SQLException("Update stock failed, no rows affected.");
        } catch (SQLException e) {
            logger.error("Error updating stock for jewelry id: {}", id, e);
            throw new DatabaseException("Failed to update stock", e);
        } finally {
            if (isInternalConn && currentConn != null) {
                try { stmt.close(); currentConn.close(); } catch (SQLException ignored) {}
            } else if (stmt != null) {
                try { stmt.close(); } catch (SQLException ignored) {}
            }
        }
    }

    @Override
    public Optional<Jewelry> findById(long id) {
        String sql = "SELECT * FROM jewelry WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToJewelry(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding jewelry by id: {}", id, e);
            throw new DatabaseException("Failed to find jewelry", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Jewelry> findAll() {
        List<Jewelry> list = new ArrayList<>();
        String sql = "SELECT * FROM jewelry";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRowToJewelry(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding all jewelry", e);
            throw new DatabaseException("Failed to list jewelry", e);
        }
        return list;
    }

    private Jewelry mapRowToJewelry(ResultSet rs) throws SQLException {
        return new Jewelry(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("company_name"),
                rs.getString("type"),
                rs.getDouble("weight"),
                rs.getInt("stock"),
                rs.getDouble("making_percent")
        );
    }
}
