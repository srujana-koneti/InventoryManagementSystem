package com.inventory.model;

/**
 * Represents a stock request (Restock ADD or Dispatch REMOVE).
 * Managed in FIFO order inside the request queue.
 */
public class StockRequest {
    private String requestId;
    private String productId;
    private String type;     // "ADD" or "REMOVE"
    private int quantity;
    private String status;   // "PENDING", "COMPLETED", "REJECTED"

    public StockRequest(String requestId, String productId, String type, int quantity) {
        if (requestId == null || requestId.trim().isEmpty()) {
            throw new IllegalArgumentException("Request ID cannot be empty.");
        }
        if (productId == null || productId.trim().isEmpty()) {
            throw new IllegalArgumentException("Product ID cannot be empty.");
        }
        if (type == null || (!type.equalsIgnoreCase("ADD") && !type.equalsIgnoreCase("REMOVE"))) {
            throw new IllegalArgumentException("Request type must be either 'ADD' or 'REMOVE'.");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Request quantity must be strictly greater than 0. Received: " + quantity);
        }

        this.requestId = requestId.trim();
        this.productId = productId.trim();
        this.type = type.trim().toUpperCase();
        this.quantity = quantity;
        this.status = "PENDING";
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        if (type == null || (!type.equalsIgnoreCase("ADD") && !type.equalsIgnoreCase("REMOVE"))) {
            throw new IllegalArgumentException("Request type must be either 'ADD' or 'REMOVE'.");
        }
        this.type = type.trim().toUpperCase();
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0.");
        }
        this.quantity = quantity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return String.format("[Request: %-6s | Product: %-5s | Action: %-6s | Qty: %3d | Status: %s]",
                requestId, productId, type, quantity, status);
    }
}
