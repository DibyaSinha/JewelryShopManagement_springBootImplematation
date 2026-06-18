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

        if (request.getCustomerMobile() != null && !request.getCustomerMobile().isEmpty()) {
            Customer customer = customerRepository.findById(request.getCustomerMobile()).orElse(null);
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

        bill = billRepository.save(bill);

        byte[] pdfData = invoiceService.generatePdf(bill);
        bill.setPdfData(pdfData);
        billRepository.save(bill);

        logger.info("Bill generated successfully ID: {}", bill.getId());
        return bill;
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
