package com.inventory.model;

/**
 * Immutable record representing a completed stock transaction.
 * Appended to the transaction history for auditing.
 */
public class Transaction {
    private final String transactionId;
    private final String productId;
    private final String productName;
    private final String type;          // "ADD_STOCK" or "REMOVE_STOCK"
    private final int quantity;
    private final String timestamp;
    private final int resultingStock;

    public Transaction(String transactionId, String productId, String productName,
                       String type, int quantity, String timestamp, int resultingStock) {
        if (transactionId == null || transactionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Transaction ID cannot be empty.");
        }
        if (productId == null || productId.trim().isEmpty()) {
            throw new IllegalArgumentException("Product ID cannot be empty.");
        }
        this.transactionId = transactionId.trim();
        this.productId = productId.trim();
        this.productName = (productName == null) ? "Unknown" : productName.trim();
        this.type = type;
        this.quantity = quantity;
        this.timestamp = (timestamp == null) ? "" : timestamp;
        this.resultingStock = resultingStock;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public String getType() {
        return type;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public int getResultingStock() {
        return resultingStock;
    }

    @Override
    public String toString() {
        return String.format("[Txn: %-8s | Time: %-19s | Product: %-5s (%-18s) | Type: %-12s | Qty: %3d | Resulting Stock: %3d]",
                transactionId, timestamp, productId, productName, type, quantity, resultingStock);
    }
}
