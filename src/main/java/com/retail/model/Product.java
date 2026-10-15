package com.retail.model;

import java.util.List;

public class Product {
    private String id;
    private String name;
    private String brand;
    private String category; // Running, Basketball, Lifestyle, Luxury
    private double price;
    private int stock;
    private List<String> availableSizes; // e.g. ["US 8", "US 9", "US 10", "US 11", "US 12"]
    private String imageUrl;
    private String badge; // "LIMITED DROP", "BESTSELLER", "FEW LEFT"

    public Product() {}

    public Product(String id, String name, String brand, String category, double price, int stock, List<String> availableSizes, String imageUrl, String badge) {
        this.id = id;
        this.name = name;
        this.brand = brand;
        this.category = category;
        this.price = price;
        this.stock = stock;
        this.availableSizes = availableSizes;
        this.imageUrl = imageUrl;
        this.badge = badge;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    public List<String> getAvailableSizes() { return availableSizes; }
    public void setAvailableSizes(List<String> availableSizes) { this.availableSizes = availableSizes; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getBadge() { return badge; }
    public void setBadge(String badge) { this.badge = badge; }
}
