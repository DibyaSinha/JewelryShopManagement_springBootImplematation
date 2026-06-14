package org.example.model;

public class Jewelry {
    private long id;
    private String name;
    private String companyName;
    private String type;          // GOLD / SILVER / DIAMOND
    private double weight;        // grams
    private int stock;
    private double makingPercent;

    public Jewelry(long id, String name, String companyName, String type,
                   double weight, int stock, double makingPercent) {
        this.id = id;
        this.name = name;
        this.companyName = companyName;
        this.type = type;
        this.weight = weight;
        this.stock = stock;
        this.makingPercent = makingPercent;
    }

    public long getId() { return id; }
    public String getName() { return name; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getType() { return type; }
    public double getWeight() { return weight; }
    public int getStock() { return stock; }
    public double getMakingPercent() { return makingPercent; }

    public void setId(long id) { this.id = id; }
    public void reduceStock(int qty) { stock -= qty; }
    public void addStock(int qty) { stock += qty; }
}
