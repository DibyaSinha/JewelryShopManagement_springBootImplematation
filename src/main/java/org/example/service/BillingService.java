package org.example.service;

import org.example.config.DatabaseConfig;
import org.example.exception.DatabaseException;
import org.example.exception.InsufficientStockException;
import org.example.model.Bill;
import org.example.model.BillItem;
import org.example.model.Customer;
import org.example.model.DailyRate;
import org.example.model.Jewelry;
import org.example.repository.impl.BillRepositoryImpl;
import org.example.repository.impl.DailyRateRepositoryImpl;
import org.example.repository.impl.JewelryRepositoryImpl;
import org.example.repository.impl.DailySaleRepositoryImpl;
import org.example.repository.interfaces.BillRepository;
import org.example.repository.interfaces.DailyRateRepository;
import org.example.repository.interfaces.JewelryRepository;
import org.example.repository.interfaces.DailySaleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class BillingService {
    private static final Logger logger = LoggerFactory.getLogger(BillingService.class);

    private JewelryRepository jewelryRepo = new JewelryRepositoryImpl();
    private BillRepository billRepo = new BillRepositoryImpl();
    private DailyRateRepository rateRepo = new DailyRateRepositoryImpl();
    private DailySaleRepository saleRepo = new DailySaleRepositoryImpl();
    private CustomerService customerService = new CustomerService();

    public Bill generateBill(List<BillItem> items, long sellerId, String customerMobile) {
        Connection conn = null;
        try {
            conn = DatabaseConfig.getConnection();
            conn.setAutoCommit(false); // START TRANSACTION

            // 1. Validate Stock & Prepare Updates
            for (BillItem item : items) {
                Jewelry dbJewelry = jewelryRepo.findById(item.getJewelry().getId())
                        .orElseThrow(() -> new RuntimeException("Jewelry not found"));

                if (dbJewelry.getStock() < item.getQuantity()) {
                    throw new InsufficientStockException("Insufficient stock for: " + dbJewelry.getName());
                }

                // 2. Deduct Stock
                int updatedStock = dbJewelry.getStock() - item.getQuantity();
                jewelryRepo.updateStock(dbJewelry.getId(), updatedStock, conn);
            }

            // 3. Save Bill (Parent)
            Bill bill = new Bill(0, items);
            bill.setSellerId(sellerId);
            
            // Handle Customer and Discount
            if (customerMobile != null && !customerMobile.isEmpty()) {
                Customer customer = customerService.getCustomerByMobile(customerMobile);
                if (customer != null) {
                    bill.setCustomerMobile(customer.getMobileNumber());
                    bill.applyDiscount(customer.getDiscountPercent());
                }
            }

            long billId = billRepo.save(bill, conn);
            bill.setBillId(billId);

            // 4. Save Bill Items (Children)
            billRepo.saveItems(bill, conn);

            // 5. Generate PDF and save to Database
            byte[] pdfBytes = org.example.util.InvoiceUtil.generatePdf(bill);
            if (pdfBytes != null) {
                billRepo.updatePdfData(billId, pdfBytes, conn);
                bill.setPdfData(pdfBytes);
            }

            // 6. Update Daily Total Sale
            saleRepo.addSaleAmount(LocalDate.now(), bill.getGrandTotal(), conn);

            conn.commit(); // COMMIT TRANSACTION
            logger.info("Bill generated successfully. ID: {}", bill.getBillId());
            return bill;

        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback(); // ROLLBACK TRANSACTION
                    logger.warn("Transaction rolled back due to error: {}", e.getMessage());
                } catch (SQLException ex) {
                    logger.error("Failed to rollback transaction", ex);
                }
            }
            if (e instanceof InsufficientStockException) throw (InsufficientStockException) e;
            throw new DatabaseException("Failed to generate bill due to database error", e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ignored) {}
            }
        }
    }

    public double getTodayRate(String metalType) {
        return rateRepo.getTodayRate(metalType)
                .map(DailyRate::getPricePerGram)
                .orElseThrow(() -> new RuntimeException("Today's rate not updated for: " + metalType));
    }

    public java.util.Map<String, String> getTodayRates() {
        java.util.Map<String, String> rates = new java.util.HashMap<>();
        String[] metals = {"GOLD", "SILVER"};
        for (String m : metals) {
            String rateStr = rateRepo.getTodayRate(m)
                    .map(r -> "₹" + String.format("%.2f", r.getPricePerGram()))
                    .orElse("Not Updated");
            rates.put(m, rateStr);
        }
        return rates;
    }

    public double getTodayTotalSale() {
        return saleRepo.getSaleForDate(LocalDate.now());
    }

    public double getMonthlyTotalSale() {
        return saleRepo.getMonthlySale();
    }

    public double getGrandTotalSale() {
        return saleRepo.getTotalSale();
    }

    public Map<String, Double> getTodaySalesBySeller() {
        return billRepo.getSalesBySellerForDate(LocalDate.now());
    }
}
