package org.example.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "jewelry")
public class Jewelry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "company_name")
    private String companyName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MetalType type;

    @Column(nullable = false)
    private Double weight;

    @Column(nullable = false)
    private Integer stock = 0;

    @Column(name = "making_percent", nullable = false)
    private Double makingPercent;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum MetalType { GOLD, SILVER }

    public Jewelry() {}

    public Jewelry(Long id, String name, String companyName, MetalType type, Double weight, Integer stock, Double makingPercent) {
        this.id = id;
        this.name = name;
        this.companyName = companyName;
        this.type = type;
        this.weight = weight;
        this.stock = stock;
        this.makingPercent = makingPercent;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public MetalType getType() { return type; }
    public void setType(MetalType type) { this.type = type; }
    public Double getWeight() { return weight; }
    public void setWeight(Double weight) { this.weight = weight; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public Double getMakingPercent() { return makingPercent; }
    public void setMakingPercent(Double makingPercent) { this.makingPercent = makingPercent; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
