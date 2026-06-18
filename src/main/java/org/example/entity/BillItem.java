package org.example.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "bill_items")
public class BillItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JsonIgnore
    @JoinColumn(name = "bill_id", nullable = false)
    private Bill bill;

    @ManyToOne
    @JoinColumn(name = "jewelry_id", nullable = false)
    private Jewelry jewelry;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "rate_at_time", nullable = false)
    private Double rateAtTime;

    @Column(name = "base_amount", nullable = false)
    private Double baseAmount;

    @Column(name = "making_charge", nullable = false)
    private Double makingCharge;

    @Column(name = "total_amount", nullable = false)
    private Double totalAmount;

    public BillItem() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Bill getBill() { return bill; }
    public void setBill(Bill bill) { this.bill = bill; }
    public Jewelry getJewelry() { return jewelry; }
    public void setJewelry(Jewelry jewelry) { this.jewelry = jewelry; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Double getRateAtTime() { return rateAtTime; }
    public void setRateAtTime(Double rateAtTime) { this.rateAtTime = rateAtTime; }
    public Double getBaseAmount() { return baseAmount; }
    public void setBaseAmount(Double baseAmount) { this.baseAmount = baseAmount; }
    public Double getMakingCharge() { return makingCharge; }
    public void setMakingCharge(Double makingCharge) { this.makingCharge = makingCharge; }
    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }
}
