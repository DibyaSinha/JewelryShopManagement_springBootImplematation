package org.example.repository.impl;

import org.example.config.DatabaseConfig;
import org.example.exception.DatabaseException;
import org.example.model.Bill;
import org.example.model.BillItem;
import org.example.model.Jewelry;
import org.example.repository.interfaces.BillRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BillRepositoryImpl implements BillRepository {
    private static final Logger logger = LoggerFactory.getLogger(BillRepositoryImpl.class);

    @Override
    public long save(Bill bill, Connection conn) {
        String sql = "INSERT INTO bills (seller_id, customer_mobile, total_amount, discount_amount, gst_amount, grand_total, bill_date) VALUES (?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setLong(1, bill.getSellerId());
            if (bill.getCustomerMobile() != null && !bill.getCustomerMobile().isEmpty()) {
                stmt.setString(2, bill.getCustomerMobile());
            } else {
                stmt.setNull(2, java.sql.Types.VARCHAR);
            }
            stmt.setDouble(3, bill.getTotalAmount());
            stmt.setDouble(4, bill.getDiscountAmount());
            stmt.setDouble(5, bill.getGstAmount());
            stmt.setDouble(6, bill.getGrandTotal());
            stmt.setTimestamp(7, Timestamp.valueOf(bill.getBillDate()));
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    long id = rs.getLong(1);
                    bill.setBillId(id);
                    return id;
                }
            }
            throw new SQLException("Creating bill failed, no ID obtained.");
        } catch (SQLException e) {
            logger.error("Error saving bill", e);
            throw new DatabaseException("Failed to save bill", e);
        } finally {
            if (stmt != null) try { stmt.close(); } catch (SQLException ignored) {}
        }
    }

    @Override
    public java.util.Map<String, Double> getSalesBySellerForDate(java.time.LocalDate date) {
        String sql = "SELECT u.username, SUM(b.grand_total) as total " +
                     "FROM bills b JOIN users u ON b.seller_id = u.id " +
                     "WHERE DATE(b.bill_date) = ? GROUP BY u.username";
        java.util.Map<String, Double> map = new java.util.HashMap<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(date));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    map.put(rs.getString("username"), rs.getDouble("total"));
                }
            }
        } catch (SQLException e) {
            logger.error("Error fetching sales breakdown", e);
            throw new DatabaseException("Failed to fetch seller sales breakdown", e);
        }
        return map;
    }

    @Override
    public void saveItems(Bill bill, Connection conn) {
        String sql = "INSERT INTO bill_items (bill_id, jewelry_id, quantity, rate_at_time, base_amount, making_charge, total_amount) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement(sql);
            for (BillItem item : bill.getItems()) {
                stmt.setLong(1, bill.getBillId());
                stmt.setLong(2, item.getJewelry().getId());
                stmt.setInt(3, item.getQuantity());
                stmt.setDouble(4, item.getRateUsed());
                stmt.setDouble(5, item.baseAmount());
                stmt.setDouble(6, item.getMakingCharge());
                stmt.setDouble(7, item.totalBeforeGST());
                stmt.addBatch();
            }
            stmt.executeBatch();
        } catch (SQLException e) {
            logger.error("Error saving bill items for bill id: {}", bill.getBillId(), e);
            throw new DatabaseException("Failed to save bill items", e);
        } finally {
            if (stmt != null) try { stmt.close(); } catch (SQLException ignored) {}
        }
    }

    @Override
    public void updatePdfData(long billId, byte[] pdfData, Connection conn) {
        String sql = "UPDATE bills SET pdf_data = ? WHERE id = ?";
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, pdfData);
            stmt.setLong(2, billId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating PDF data for bill id: {}", billId, e);
            throw new DatabaseException("Failed to update PDF data", e);
        } finally {
            if (stmt != null) try { stmt.close(); } catch (SQLException ignored) {}
        }
    }

    @Override
    public List<Bill> findAll() {
        List<Bill> list = new ArrayList<>();
        String sql = "SELECT * FROM bills ORDER BY bill_date DESC";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRowToBill(rs, conn));
            }
        } catch (SQLException e) {
            logger.error("Error listing all bills", e);
            throw new DatabaseException("Failed to list bills", e);
        }
        return list;
    }

    @Override
    public Optional<Bill> findById(long id) {
        String sql = "SELECT * FROM bills WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToBill(rs, conn));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding bill by id: {}", id, e);
            throw new DatabaseException("Failed to find bill", e);
        }
        return Optional.empty();
    }

    private Bill mapRowToBill(ResultSet rs, Connection conn) throws SQLException {
        long billId = rs.getLong("id");
        List<BillItem> items = findItemsByBillId(billId, conn);
        Bill bill = new Bill(billId, items);
        bill.setSellerId(rs.getLong("seller_id"));
        bill.setCustomerMobile(rs.getString("customer_mobile"));
        bill.setDiscountAmount(rs.getDouble("discount_amount"));
        bill.setBillDate(rs.getTimestamp("bill_date").toLocalDateTime());
        bill.setPdfData(rs.getBytes("pdf_data"));
        return bill;
    }

    private List<BillItem> findItemsByBillId(long billId, Connection conn) throws SQLException {
        List<BillItem> items = new ArrayList<>();
        String sql = "SELECT bi.*, j.name, j.company_name, j.type, j.weight, j.making_percent " +
                     "FROM bill_items bi JOIN jewelry j ON bi.jewelry_id = j.id WHERE bi.bill_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, billId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Jewelry j = new Jewelry(
                            rs.getLong("jewelry_id"),
                            rs.getString("name"),
                            rs.getString("company_name"),
                            rs.getString("type"),
                            rs.getDouble("weight"),
                            0, // Stock not needed for historical bill items
                            rs.getDouble("making_percent")
                    );
                    BillItem item = new BillItem(j, rs.getInt("quantity"), rs.getDouble("rate_at_time"));
                    item.setId(rs.getLong("id"));
                    items.add(item);
                }
            }
        }
        return items;
    }
}
