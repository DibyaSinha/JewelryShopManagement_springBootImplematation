package org.example.model;

import java.time.LocalDate;

public class DailyRate {
    private long id;
    private String metalType;
    private double pricePerGram;
    private LocalDate date;

    public DailyRate(String metalType, double pricePerGram) {
        this.metalType = metalType;
        this.pricePerGram = pricePerGram;
        this.date = LocalDate.now();
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getMetalType() { return metalType; }
    public double getPricePerGram() { return pricePerGram; }

    public LocalDate getDate() {
        if (date == null) date = LocalDate.now();
        return date;
    }

    public void setDate(LocalDate date) { this.date = date; }
}
