package org.example.model;

import java.time.LocalDateTime;
import java.util.List;

public class Bill {
    private long billId;
    private long sellerId;
    private String customerMobile;
    private List<BillItem> items;
    private LocalDateTime billDate;
    private double totalAmount; // Subtotal
    private double discountAmount;
    private double gstAmount;
    private double grandTotal;
    private byte[] pdfData;

    public Bill(long billId, List<BillItem> items) {
        this.billId = billId;
        this.items = items;
        this.billDate = LocalDateTime.now();
        this.discountAmount = 0.0;
        calculateTotals();
    }

    public void applyDiscount(double discountPercent) {
        double subtotal = 0;
        for (BillItem item : items) {
            subtotal += item.totalBeforeGST();
        }
        this.discountAmount = subtotal * (discountPercent / 100.0);
        calculateTotals();
    }

    private void calculateTotals() {
        double subtotal = 0;
        for (BillItem item : items) {
            subtotal += item.totalBeforeGST();
        }
        this.totalAmount = subtotal;
        double discountedSubtotal = subtotal - this.discountAmount;
        this.gstAmount = discountedSubtotal * 0.03;
        this.grandTotal = discountedSubtotal + gstAmount;
    }

    public long getBillId() { return billId; }
    public void setBillId(long billId) { this.billId = billId; }
    public long getSellerId() { return sellerId; }
    public void setSellerId(long sellerId) { this.sellerId = sellerId; }
    public String getCustomerMobile() { return customerMobile; }
    public void setCustomerMobile(String customerMobile) { this.customerMobile = customerMobile; }
    public List<BillItem> getItems() { return items; }
    public LocalDateTime getBillDate() { return billDate; }
    public void setBillDate(LocalDateTime billDate) { this.billDate = billDate; }

    public double getTotalAmount() { return totalAmount; }
    public double getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(double discountAmount) { 
        this.discountAmount = discountAmount;
        calculateTotals(); // Recalculate if set directly
    }
    public double getGstAmount() { return gstAmount; }
    public double getGrandTotal() { return grandTotal; }

    public byte[] getPdfData() { return pdfData; }
    public void setPdfData(byte[] pdfData) { this.pdfData = pdfData; }

    // Legacy method for compatibility if needed
    public double totalAmount() {
        return grandTotal;
    }
}
