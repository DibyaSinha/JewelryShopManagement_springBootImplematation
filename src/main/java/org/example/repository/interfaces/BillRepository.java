package org.example.repository.interfaces;

import org.example.model.Bill;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public interface BillRepository {
    long save(Bill bill, Connection conn);
    void saveItems(Bill bill, Connection conn);
    void updatePdfData(long billId, byte[] pdfData, Connection conn);
    List<Bill> findAll();
    java.util.Map<String, Double> getSalesBySellerForDate(java.time.LocalDate date);
    Optional<Bill> findById(long id);
}
