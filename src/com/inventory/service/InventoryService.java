package com.inventory.service;

import com.inventory.model.Product;
import com.inventory.model.StockRequest;
import com.inventory.model.Transaction;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * Core Service Layer managing inventory business logic.
 *
 * Core Data Structures Used:
 * 1. HashMap<String, Product>: Fast O(1) average lookup and storage by unique Product ID.
 * 2. Queue<StockRequest> (ArrayDeque): Enforces FIFO (First-In, First-Out) order for stock requests.
 * 3. List<Transaction> (ArrayList): Amortized O(1) appending for audit trail records.
 */
public class InventoryService {

    // 1. HashMap for O(1) fast product ID lookup and storage
    private Map<String, Product> productCatalog = new HashMap<>();

    // 2. Queue implemented as ArrayDeque to guarantee FIFO order of stock request processing
    private Queue<StockRequest> requestQueue = new ArrayDeque<>();

    // 3. ArrayList for sequential, append-only transaction history
    private List<Transaction> transactionHistory = new ArrayList<>();

    // Counters for generating readable, unique IDs
    private int transactionCounter = 1;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Adds a new product to the inventory.
     * Constraint: Product ID must be unique.
     *
     * @param product the product to add
     * @throws IllegalArgumentException if the product ID already exists or product is null
     */
    public void addProduct(Product product) {
        if (product == null) {
            throw new IllegalArgumentException("Cannot add a null product.");
        }
        if (productCatalog.containsKey(product.getId())) {
            throw new IllegalArgumentException("Product ID already exists: " + product.getId() + ". Duplicate IDs are not allowed.");
        }
        productCatalog.put(product.getId(), product);
    }

    /**
     * Retrieves a product by its unique ID.
     * DSA Complexity: O(1) average time via HashMap lookup.
     *
     * @param id the product ID
     * @return the Product object, or null if not found
     */
    public Product getProduct(String id) {
        if (id == null) {
            return null;
        }
        return productCatalog.get(id.trim());
    }

    /**
     * Checks if there are any pending stock requests in the queue for a given product ID.
     *
     * @param productId the product ID to check
     * @return true if a pending request exists for this product, false otherwise
     */
    public boolean hasPendingRequests(String productId) {
        if (productId == null) return false;
        String cleanId = productId.trim();
        for (StockRequest req : requestQueue) {
            if (req.getProductId().equalsIgnoreCase(cleanId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Deletes a product from the inventory catalog.
     * Safe behavior: Rejects deletion if product does not exist or if it has pending stock requests.
     *
     * @param productId the ID of the product to delete
     * @return true if successfully deleted
     * @throws IllegalArgumentException if product not found
     * @throws IllegalStateException if product has pending stock requests
     */
    public boolean deleteProduct(String productId) {
        if (productId == null || productId.trim().isEmpty()) {
            throw new IllegalArgumentException("Product ID cannot be empty.");
        }
        String cleanId = productId.trim();
        if (!productCatalog.containsKey(cleanId)) {
            throw new IllegalArgumentException("Product " + cleanId + " not found.");
        }
        if (hasPendingRequests(cleanId)) {
            throw new IllegalStateException("Cannot delete product " + cleanId + " because it has pending stock requests.");
        }

        productCatalog.remove(cleanId);
        return true;
    }

    /**
     * Updates an existing product's details in the inventory catalog.
     * Constraint: Product must exist; fields must meet standard domain validations.
     *
     * @param id           the ID of the product to update
     * @param name         updated name
     * @param category     updated category
     * @param price        updated price (> 0)
     * @param quantity     updated quantity (>= 0)
     * @param reorderLevel updated reorder level (>= 0)
     * @return true if successfully updated
     * @throws IllegalArgumentException if product not found or invalid inputs
     */
    public boolean updateProduct(String id, String name, String category, double price, int quantity, int reorderLevel) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Product ID cannot be empty.");
        }
        String cleanId = id.trim();
        Product product = productCatalog.get(cleanId);
        if (product == null) {
            throw new IllegalArgumentException("Product " + cleanId + " not found.");
        }

        // Apply validated setters on existing Product instance
        product.setName(name);
        product.setCategory(category != null && !category.trim().isEmpty() ? category.trim() : "General");
        product.setPrice(price);
        product.setQuantity(quantity);
        product.setReorderLevel(reorderLevel);
        return true;
    }

    /**
     * Returns a copy of all products in the catalog.
     * Does NOT expose internal HashMap directly to maintain encapsulation.
     *
     * @return List of all products
     */
    public List<Product> getAllProducts() {
        return new ArrayList<>(productCatalog.values());
    }

    /**
     * Adds stock directly to a product and records a successful transaction.
     *
     * @param id  product ID
     * @param qty quantity to add (> 0)
     * @return true if successful
     * @throws IllegalArgumentException if product not found or qty <= 0
     */
    public boolean addStock(String id, int qty) {
        Product product = getProduct(id);
        if (product == null) {
            throw new IllegalArgumentException("Product not found with ID: " + id);
        }
        if (qty <= 0) {
            throw new IllegalArgumentException("Quantity to add must be greater than 0. Received: " + qty);
        }

        int newQuantity = product.getQuantity() + qty;
        product.setQuantity(newQuantity);

        // Create and record transaction
        recordTransaction(product.getId(), product.getName(), "ADD_STOCK", qty, newQuantity);
        return true;
    }

    /**
     * Removes stock directly from a product.
     * STRICT CONSTRAINT: Stock quantity must never become negative.
     * If requested qty > available stock, the operation is rejected and stock remains unchanged.
     *
     * @param id  product ID
     * @param qty quantity to remove (> 0)
     * @return true if successfully removed, false if insufficient stock
     * @throws IllegalArgumentException if product not found or qty <= 0
     */
    public boolean removeStock(String id, int qty) {
        Product product = getProduct(id);
        if (product == null) {
            throw new IllegalArgumentException("Product not found with ID: " + id);
        }
        if (qty <= 0) {
            throw new IllegalArgumentException("Quantity to remove must be greater than 0. Received: " + qty);
        }

        // Check if there is enough stock
        if (qty > product.getQuantity()) {
            // Operation rejected! Stock must remain completely unchanged.
            return false;
        }

        int newQuantity = product.getQuantity() - qty;
        product.setQuantity(newQuantity);

        // Record transaction ONLY when stock successfully changes
        recordTransaction(product.getId(), product.getName(), "REMOVE_STOCK", qty, newQuantity);
        return true;
    }

    /**
     * Identifies and returns all low-stock products using their reorder level.
     * Condition: quantity <= reorderLevel
     *
     * @return List of products at or below their reorder level
     */
    public List<Product> getLowStockProducts() {
        List<Product> lowStockList = new ArrayList<>();
        for (Product product : productCatalog.values()) {
            if (product.isLowStock()) {
                lowStockList.add(product);
            }
        }
        return lowStockList;
    }

    /**
     * Enqueues a new stock request into the FIFO request queue.
     *
     * @param request the StockRequest to enqueue
     * @throws IllegalArgumentException if product does not exist or invalid request
     */
    public void enqueueRequest(StockRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Stock request cannot be null.");
        }
        if (!productCatalog.containsKey(request.getProductId())) {
            throw new IllegalArgumentException("Cannot create request: Product ID '" + request.getProductId() + "' does not exist.");
        }
        if (request.getQuantity() <= 0) {
            throw new IllegalArgumentException("Request quantity must be strictly greater than 0.");
        }

        request.setStatus("PENDING");
        // Enqueue to the tail of the ArrayDeque
        requestQueue.offer(request);
    }

    /**
     * Processes the next pending stock request in strict FIFO order.
     * Dequeues the oldest request using requestQueue.poll().
     *
     * @return a message describing the outcome of the processed request
     */
    public String processNextRequest() {
        if (requestQueue.isEmpty()) {
            return "No pending requests in queue.";
        }

        // poll() removes and returns the head of the queue (FIFO)
        StockRequest req = requestQueue.poll();
        Product product = productCatalog.get(req.getProductId());

        if (product == null) {
            req.setStatus("REJECTED");
            return "Request " + req.getRequestId() + " REJECTED: Product '" + req.getProductId() + "' no longer exists.";
        }

        if (req.getType().equalsIgnoreCase("ADD")) {
            // Processing restock ADD
            int newQuantity = product.getQuantity() + req.getQuantity();
            product.setQuantity(newQuantity);
            req.setStatus("COMPLETED");

            recordTransaction(product.getId(), product.getName(), "ADD_STOCK", req.getQuantity(), newQuantity);
            return "Request " + req.getRequestId() + " COMPLETED: Added " + req.getQuantity() +
                    " units to " + product.getName() + " (New Stock: " + newQuantity + ").";

        } else if (req.getType().equalsIgnoreCase("REMOVE")) {
            // Processing dispatch REMOVE
            if (product.getQuantity() >= req.getQuantity()) {
                // Sufficient stock exists
                int newQuantity = product.getQuantity() - req.getQuantity();
                product.setQuantity(newQuantity);
                req.setStatus("COMPLETED");

                recordTransaction(product.getId(), product.getName(), "REMOVE_STOCK", req.getQuantity(), newQuantity);
                return "Request " + req.getRequestId() + " COMPLETED: Removed " + req.getQuantity() +
                        " units from " + product.getName() + " (Remaining Stock: " + newQuantity + ").";
            } else {
                // Insufficient stock: reject request. Stock MUST remain unchanged!
                req.setStatus("REJECTED");
                return "Request " + req.getRequestId() + " REJECTED: Insufficient stock for " + product.getName() +
                        ". Requested: " + req.getQuantity() + ", Available: " + product.getQuantity() +
                        ". Stock remains unchanged at " + product.getQuantity() + ".";
            }
        }

        req.setStatus("REJECTED");
        return "Request " + req.getRequestId() + " REJECTED: Unknown request type: " + req.getType();
    }

    /**
     * Processes all pending requests until the queue becomes empty.
     *
     * @return List of status messages for all processed requests
     */
    public List<String> processAllRequests() {
        List<String> results = new ArrayList<>();
        if (requestQueue.isEmpty()) {
            results.add("No pending requests in queue.");
            return results;
        }

        while (!requestQueue.isEmpty()) {
            results.add(processNextRequest());
        }
        return results;
    }

    /**
     * Returns the current pending queue contents without destroying or modifying the queue.
     *
     * @return a List copy of pending StockRequests in arrival order
     */
    public List<StockRequest> getPendingRequests() {
        return new ArrayList<>(requestQueue);
    }

    /**
     * Returns the full transaction history.
     *
     * @return a List copy of all recorded Transactions
     */
    public List<Transaction> getTransactions() {
        return new ArrayList<>(transactionHistory);
    }

    /**
     * Helper method to generate an immutable Transaction record and append to history.
     */
    private void recordTransaction(String productId, String productName, String type, int qty, int resultingStock) {
        String txnId = "TXN-" + String.format("%03d", transactionCounter++);
        String timestamp = LocalDateTime.now().format(TIME_FORMATTER);
        Transaction txn = new Transaction(txnId, productId, productName, type, qty, timestamp, resultingStock);
        transactionHistory.add(txn);
    }
}
