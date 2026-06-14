package org.example.model;

public class BillItem {
    private long id;
    private Jewelry jewelry;
    private int quantity;
    private double rateUsed;

    public BillItem(Jewelry jewelry, int quantity, double rateUsed) {
        this.jewelry = jewelry;
        this.quantity = quantity;
        this.rateUsed = rateUsed;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public Jewelry getJewelry() { return jewelry; }
    public int getQuantity() { return quantity; }

    public double baseAmount() {
        return jewelry.getWeight() * rateUsed * quantity;
    }

    public double makingCharge() {
        return baseAmount() * jewelry.getMakingPercent() / 100;
    }

    public double totalBeforeGST() {
        return baseAmount() + makingCharge();
    }

    public double getRateUsed() {
        return rateUsed;
    }

    public double getMakingCharge() {
        return makingCharge();
    }
}
