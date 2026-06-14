package org.example.model;

import java.time.LocalDateTime;

public class Customer {
    private String mobileNumber;
    private String name;
    private double discountPercent;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Customer(String mobileNumber, String name, double discountPercent) {
        this.mobileNumber = mobileNumber;
        this.name = name;
        this.discountPercent = discountPercent;
    }

    public String getMobileNumber() { return mobileNumber; }
    public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(double discountPercent) { this.discountPercent = discountPercent; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
