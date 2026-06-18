package org.example.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_rates", uniqueConstraints = {
    @UniqueConstraint(name = "unique_rate_per_day", columnNames = {"metal_type", "rate_date"})
})
public class DailyRate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "metal_type", nullable = false)
    private Jewelry.MetalType metalType;

    @Column(name = "price_per_gram", nullable = false)
    private Double pricePerGram;

    @Column(name = "rate_date", nullable = false)
    private LocalDate rateDate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public DailyRate() {}

    public DailyRate(Long id, Jewelry.MetalType metalType, Double pricePerGram, LocalDate rateDate) {
        this.id = id;
        this.metalType = metalType;
        this.pricePerGram = pricePerGram;
        this.rateDate = rateDate;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Jewelry.MetalType getMetalType() { return metalType; }
    public void setMetalType(Jewelry.MetalType metalType) { this.metalType = metalType; }
    public Double getPricePerGram() { return pricePerGram; }
    public void setPricePerGram(Double pricePerGram) { this.pricePerGram = pricePerGram; }
    public LocalDate getRateDate() { return rateDate; }
    public void setRateDate(LocalDate rateDate) { this.rateDate = rateDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
