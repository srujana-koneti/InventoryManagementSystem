package com.inventory.model;

/**
 * Represents a product in the inventory.
 * Encapsulates product details and business validation rules.
 */
public class Product {
    private String id;
    private String name;
    private String category;
    private double price;
    private int quantity;
    private int reorderLevel;

    /**
     * Constructs a new Product with strict validation.
     */
    public Product(String id, String name, String category, double price, int quantity, int reorderLevel) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Product ID cannot be null or empty.");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Product Name cannot be null or empty.");
        }
        if (price <= 0) {
            throw new IllegalArgumentException("Price must be strictly greater than 0. Received: " + price);
        }
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative. Received: " + quantity);
        }
        if (reorderLevel < 0) {
            throw new IllegalArgumentException("Reorder level cannot be negative. Received: " + reorderLevel);
        }

        this.id = id.trim();
        this.name = name.trim();
        this.category = (category == null || category.trim().isEmpty()) ? "General" : category.trim();
        this.price = price;
        this.quantity = quantity;
        this.reorderLevel = reorderLevel;
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Product ID cannot be null or empty.");
        }
        this.id = id.trim();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Product Name cannot be null or empty.");
        }
        this.name = name.trim();
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = (category == null || category.trim().isEmpty()) ? "General" : category.trim();
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        if (price <= 0) {
            throw new IllegalArgumentException("Price must be greater than 0.");
        }
        this.price = price;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative. Stock rule violated: " + quantity);
        }
        this.quantity = quantity;
    }

    public int getReorderLevel() {
        return reorderLevel;
    }

    public void setReorderLevel(int reorderLevel) {
        if (reorderLevel < 0) {
            throw new IllegalArgumentException("Reorder level cannot be negative.");
        }
        this.reorderLevel = reorderLevel;
    }

    /**
     * Checks if this product has low stock based on its reorder level threshold.
     * @return true if quantity <= reorderLevel, false otherwise.
     */
    public boolean isLowStock() {
        return this.quantity <= this.reorderLevel;
    }

    /**
     * Returns a user-friendly status description.
     */
    public String getStockStatus() {
        if (this.quantity == 0) {
            return "OUT OF STOCK";
        } else if (isLowStock()) {
            return "LOW STOCK";
        } else {
            return "NORMAL";
        }
    }

    @Override
    public String toString() {
        return String.format("[ID: %-5s | Name: %-22s | Category: %-12s | Price: $%7.2f | Qty: %3d | Reorder: %2d | Status: %s]",
                id, name, category, price, quantity, reorderLevel, getStockStatus());
    }
}
