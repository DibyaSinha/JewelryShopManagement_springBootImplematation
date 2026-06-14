package org.example.repository.interfaces;

import java.sql.Connection;
import java.time.LocalDate;
import java.util.Optional;

public interface DailySaleRepository {
    void addSaleAmount(LocalDate date, double amount, Connection conn);
    double getSaleForDate(LocalDate date);
    double getMonthlySale();
    double getTotalSale();
}
