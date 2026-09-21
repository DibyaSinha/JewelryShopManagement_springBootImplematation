package org.example.service;

import org.example.dto.BillRequest;
import org.example.entity.*;
import org.example.exception.InsufficientStockException;
import org.example.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BillingService {
    private static final Logger logger = LoggerFactory.getLogger(BillingService.class);

    @Autowired
    private BillRepository billRepository;
    @Autowired
    private JewelryRepository jewelryRepository;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private DailyRateRepository dailyRateRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private InvoiceService invoiceService;

    @Transactional
    public Bill generateBill(BillRequest request, Long sellerId) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new RuntimeException("Seller not found"));

        Bill bill = new Bill();
        bill.setSeller(seller);
        bill.setBillDate(LocalDateTime.now());

        if (request.getCustomerMobile() != null && !request.getCustomerMobile().trim().isEmpty()) {
            String mobile = request.getCustomerMobile().trim();
            Customer customer = customerRepository.findById(mobile).orElse(null);
            if (customer == null) {
                if (request.getCustomerName() != null && !request.getCustomerName().trim().isEmpty()) {
                    customer = new Customer(mobile, request.getCustomerName().trim(), 0.0);
                    customer = customerRepository.save(customer);
                    logger.info("New customer created during billing: Name='{}', Mobile='{}'", customer.getName(), customer.getMobileNumber());
                } else {
                    throw new RuntimeException("Customer name is required for new customer");
                }
            }
            bill.setCustomer(customer);
        }

        List<BillItem> billItems = new ArrayList<>();
        double subtotal = 0;

        for (BillRequest.BillItemRequest itemReq : request.getItems()) {
            Jewelry jewelry = jewelryRepository.findById(itemReq.getJewelryId())
                    .orElseThrow(() -> new RuntimeException("Jewelry not found: " + itemReq.getJewelryId()));

            if (jewelry.getStock() < itemReq.getQuantity()) {
                throw new InsufficientStockException("Insufficient stock for: " + jewelry.getName());
            }

            if (jewelry.getVersion() == null) {
                jewelry.setVersion(0L);
            }

            jewelry.setStock(jewelry.getStock() - itemReq.getQuantity());
            jewelryRepository.save(jewelry);

            DailyRate rate = dailyRateRepository.findByMetalTypeAndRateDate(jewelry.getType(), LocalDate.now())
                    .orElseThrow(() -> new RuntimeException("Rate not updated for " + jewelry.getType()));

            double baseAmount = jewelry.getWeight() * rate.getPricePerGram() * itemReq.getQuantity();
            double makingCharge = baseAmount * (jewelry.getMakingPercent() / 100.0);
            double totalItemAmount = baseAmount + makingCharge;

            BillItem billItem = new BillItem();
            billItem.setBill(bill);
            billItem.setJewelry(jewelry);
            billItem.setQuantity(itemReq.getQuantity());
            billItem.setRateAtTime(rate.getPricePerGram());
            billItem.setBaseAmount(baseAmount);
            billItem.setMakingCharge(makingCharge);
            billItem.setTotalAmount(totalItemAmount);

            billItems.add(billItem);
            subtotal += totalItemAmount;

            logger.info("Jewelry item added to bill: {}, Qty: {}", jewelry.getName(), itemReq.getQuantity());
            logger.info("Stock updated: Item '{}' (ID: {}), stock reduced by {} (Remaining: {})", jewelry.getName(), jewelry.getId(), itemReq.getQuantity(), jewelry.getStock());
        }

        bill.setItems(billItems);
        bill.setTotalAmount(subtotal);

        double discountAmount = 0;
        if (bill.getCustomer() != null) {
            discountAmount = subtotal * (bill.getCustomer().getDiscountPercent() / 100.0);
        }
        bill.setDiscountAmount(discountAmount);

        double taxableAmount = subtotal - discountAmount;
        double gstAmount = taxableAmount * 0.03;
        bill.setGstAmount(gstAmount);
        bill.setGrandTotal(taxableAmount + gstAmount);

        logger.info("Bill calculation completed: Total: Rs.{}", String.format("%.2f", bill.getGrandTotal()));

        bill = billRepository.save(bill);

        logger.info("Payment completed: Grand Total: Rs.{}", String.format("%.2f", bill.getGrandTotal()));
        logger.info("Bill created successfully: Bill ID: {}", bill.getId());
        return bill;
    }

    @Transactional(readOnly = true)
    public byte[] getBillPdf(Long id) {
        Bill bill = billRepository.findById(id).orElse(null);
        if (bill == null) {
            logger.warn("Bill not found for PDF export: ID {}", id);
            return null;
        }
        byte[] pdf = invoiceService.generatePdf(bill);
        logger.info("Bill PDF retrieved successfully for Bill ID: {}", id);
        return pdf;
    }

    public Double getTodayRate(Jewelry.MetalType type) {
        return dailyRateRepository.findByMetalTypeAndRateDate(type, LocalDate.now())
                .map(DailyRate::getPricePerGram)
                .orElseThrow(() -> new RuntimeException("Rate not found for today: " + type));
    }

    public List<Bill> getAllBills() {
        return billRepository.findAllByOrderByBillDateDesc();
    }

    public Bill getBillById(Long id) {
        return billRepository.findById(id).orElse(null);
    }

    public Double getTodayTotalSale() {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = LocalDateTime.now();
        Double total = billRepository.getTotalRevenueBetween(start, end);
        return total != null ? total : 0.0;
    }

    public Double getMonthlyTotalSale() {
        LocalDateTime start = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime end = LocalDateTime.now();
        Double total = billRepository.getTotalRevenueBetween(start, end);
        return total != null ? total : 0.0;
    }

    public Double getTotalSale() {
        Double total = billRepository.getTotalRevenue();
        return total != null ? total : 0.0;
    }

    public Map<String, Double> getMonthlySalesBreakdown() {
        return billRepository.getMonthlySalesBreakdown().stream()
                .collect(Collectors.toMap(
                        obj -> (String) obj[0],
                        obj -> (Double) obj[1]
                ));
    }

    public Map<String, Double> getTodaySalesBySeller() {
        return billRepository.findSalesBySellerForDate(LocalDate.now()).stream()
                .collect(Collectors.toMap(
                        obj -> (String) obj[0],
                        obj -> (Double) obj[1]
                ));
    }
}
