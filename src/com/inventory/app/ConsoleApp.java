package com.inventory.app;

import com.inventory.dsa.SearchUtil;
import com.inventory.dsa.SortUtil;
import com.inventory.model.Product;
import com.inventory.model.StockRequest;
import com.inventory.model.Transaction;
import com.inventory.service.InventoryService;

import java.util.List;
import java.util.Scanner;

/**
 * Interactive Console Application for the Inventory Management & Resource Optimization System.
 * Demonstrates Core Java and Data Structures & Algorithms (DSA).
 */
public class ConsoleApp {

    private final InventoryService inventoryService;
    private final Scanner scanner;
    private int requestIdCounter = 1;

    public ConsoleApp() {
        this.inventoryService = new InventoryService();
        this.scanner = new Scanner(System.in);
        seedSampleData();
    }

    public ConsoleApp(InventoryService service, Scanner scanner) {
        this.inventoryService = service;
        this.scanner = scanner;
    }

    /**
     * Seeds initial sample products as per project requirements.
     */
    public void seedSampleData() {
        try {
            inventoryService.addProduct(new Product("P101", "Wireless Mouse", "Electronics", 25.50, 15, 10));
            inventoryService.addProduct(new Product("P102", "Mechanical Keyboard", "Electronics", 75.00, 4, 5));
            inventoryService.addProduct(new Product("P103", "USB-C Hub", "Accessories", 34.99, 2, 8));
            inventoryService.addProduct(new Product("P104", "Desk Chair", "Furniture", 189.00, 20, 5));
            inventoryService.addProduct(new Product("P105", "Desk Mat", "Furniture", 45.00, 0, 10));
        } catch (Exception e) {
            System.err.println("Failed to seed sample data: " + e.getMessage());
        }
    }

    /**
     * Main application loop.
     */
    public void run() {
        boolean running = true;
        while (running) {
            printMainMenu();
            int choice = readIntInput("Enter your choice (1-14): ");

            switch (choice) {
                case 1:
                    handleAddProduct();
                    break;
                case 2:
                    handleViewAllProducts();
                    break;
                case 3:
                    handleSearchMenu();
                    break;
                case 4:
                    handleSortMenu();
                    break;
                case 5:
                    handleAddStockDirectly();
                    break;
                case 6:
                    handleRemoveStockDirectly();
                    break;
                case 7:
                    handleViewLowStockAlerts();
                    break;
                case 8:
                    handleCreateStockRequest();
                    break;
                case 9:
                    handleProcessNextRequest();
                    break;
                case 10:
                    handleProcessAllRequests();
                    break;
                case 11:
                    handleViewPendingQueue();
                    break;
                case 12:
                    handleViewTransactionHistory();
                    break;
                case 13:
                    handleDeleteProduct();
                    break;
                case 14:
                    System.out.println("\nThank you for using Inventory Management System. Goodbye!");
                    running = false;
                    break;
                default:
                    System.out.println("\n[ERROR] Invalid option. Please enter a number between 1 and 14.");
            }
        }
    }

    private void printMainMenu() {
        System.out.println("\n====================================================");
        System.out.println(" INVENTORY MANAGEMENT & RESOURCE OPTIMIZATION SYSTEM");
        System.out.println("====================================================");
        System.out.println("1. Add New Product");
        System.out.println("2. View All Products");
        System.out.println("3. Search Products");
        System.out.println("4. Sort Products");
        System.out.println("5. Add Stock Directly");
        System.out.println("6. Remove Stock Directly");
        System.out.println("7. View Low-Stock Alerts");
        System.out.println("8. Create Stock Request");
        System.out.println("9. Process Next Stock Request");
        System.out.println("10. Process All Stock Requests");
        System.out.println("11. View Pending Request Queue");
        System.out.println("12. View Stock Transaction History");
        System.out.println("13. Delete Product");
        System.out.println("14. Exit");
        System.out.println("====================================================");
    }

    private void handleAddProduct() {
        System.out.println("\n--- ADD NEW PRODUCT ---");
        String id = readNonEmptyString("Enter Product ID (e.g. P106): ");
        if (inventoryService.getProduct(id) != null) {
            System.out.println("[ERROR] Product ID '" + id + "' already exists! IDs must be unique.");
            return;
        }

        String name = readNonEmptyString("Enter Product Name: ");
        String category = readNonEmptyString("Enter Category: ");
        double price = readPositiveDouble("Enter Price ($): ");
        int quantity = readNonNegativeInt("Enter Initial Quantity: ");
        int reorderLevel = readNonNegativeInt("Enter Reorder Threshold Level: ");

        try {
            Product newProduct = new Product(id, name, category, price, quantity, reorderLevel);
            inventoryService.addProduct(newProduct);
            System.out.println("[SUCCESS] Product added successfully: " + newProduct.getName() + " (" + newProduct.getId() + ")");
        } catch (Exception e) {
            System.out.println("[ERROR] Could not add product: " + e.getMessage());
        }
    }

    private void handleViewAllProducts() {
        System.out.println("\n--- ALL PRODUCTS IN INVENTORY ---");
        List<Product> products = inventoryService.getAllProducts();
        displayProductTable(products);
    }

    private void handleSearchMenu() {
        System.out.println("\n--- SEARCH PRODUCTS ---");
        System.out.println("1. Search by Product ID using HashMap [O(1) average lookup]");
        System.out.println("2. Linear Search by Name or Category [O(n) partial match]");
        System.out.println("3. Binary Search by Exact Name [O(log n) requires name-sorted list]");
        System.out.println("4. Back to Main Menu");

        int subChoice = readIntInput("Select search method (1-4): ");
        switch (subChoice) {
            case 1: {
                System.out.println("\n[DSA: HashMap Lookup - O(1)]");
                String id = readNonEmptyString("Enter Product ID to find: ");
                Product p = inventoryService.getProduct(id);
                if (p != null) {
                    System.out.println("\n[FOUND in HashMap in O(1) time]:");
                    printProductRowHeader();
                    printProductRow(p);
                    printProductRowFooter();
                } else {
                    System.out.println("[RESULT] Product not found with ID: " + id);
                }
                break;
            }
            case 2: {
                System.out.println("\n[DSA: Linear Search - O(n)]");
                System.out.println("a. Search by Name contains");
                System.out.println("b. Search by Category");
                String opt = readNonEmptyString("Choose (a/b): ").toLowerCase();
                if (opt.equals("a")) {
                    String query = readNonEmptyString("Enter partial product name: ");
                    List<Product> results = SearchUtil.linearSearchByName(inventoryService.getAllProducts(), query);
                    System.out.println("\nLinear Search Results for '" + query + "' (" + results.size() + " matches):");
                    displayProductTable(results);
                } else if (opt.equals("b")) {
                    String cat = readNonEmptyString("Enter category name: ");
                    List<Product> results = SearchUtil.linearSearchByCategory(inventoryService.getAllProducts(), cat);
                    System.out.println("\nLinear Search Results for Category '" + cat + "' (" + results.size() + " matches):");
                    displayProductTable(results);
                } else {
                    System.out.println("[ERROR] Invalid search option.");
                }
                break;
            }
            case 3: {
                System.out.println("\n[DSA: Binary Search - O(log n)]");
                System.out.println("Step 1: Fetching all products and sorting by Name using Merge Sort...");
                List<Product> sortedList = inventoryService.getAllProducts();
                SortUtil.sortByNameAscending(sortedList);
                System.out.println("Pre-condition satisfied: Product list is now strictly sorted by Name.");

                String targetName = readNonEmptyString("Enter exact product name to search: ");
                System.out.println("Step 2: Executing manual Binary Search algorithm...");
                Product match = SearchUtil.binarySearchByName(sortedList, targetName);

                if (match != null) {
                    System.out.println("\n[FOUND via Binary Search in O(log n) time]:");
                    printProductRowHeader();
                    printProductRow(match);
                    printProductRowFooter();
                } else {
                    System.out.println("[RESULT] Product with exact name '" + targetName + "' was not found.");
                }
                break;
            }
            case 4:
                return;
            default:
                System.out.println("[ERROR] Invalid choice.");
        }
    }

    private void handleSortMenu() {
        System.out.println("\n--- SORT PRODUCTS (USING CUSTOM MERGE SORT) ---");
        System.out.println("DSA Concept: Merge Sort from scratch [Time O(n log n), Space O(n)]");
        System.out.println("1. Sort by Product Name Ascending (A - Z)");
        System.out.println("2. Sort by Price Ascending (Low to High)");
        System.out.println("3. Sort by Price Descending (High to Low)");
        System.out.println("4. Sort by Quantity Ascending (Low to High)");
        System.out.println("5. Sort by Quantity Descending (High to Low)");
        System.out.println("6. Back to Main Menu");

        int sortChoice = readIntInput("Select sorting option (1-6): ");
        List<Product> list = inventoryService.getAllProducts();

        switch (sortChoice) {
            case 1:
                SortUtil.sortByNameAscending(list);
                System.out.println("\n[Sorted by Name Ascending (A - Z) via Merge Sort]:");
                displayProductTable(list);
                break;
            case 2:
                SortUtil.sortByPriceAscending(list);
                System.out.println("\n[Sorted by Price Ascending (Low to High) via Merge Sort]:");
                displayProductTable(list);
                break;
            case 3:
                SortUtil.sortByPriceDescending(list);
                System.out.println("\n[Sorted by Price Descending (High to Low) via Merge Sort]:");
                displayProductTable(list);
                break;
            case 4:
                SortUtil.sortByQuantityAscending(list);
                System.out.println("\n[Sorted by Quantity Ascending (Low to High) via Merge Sort]:");
                displayProductTable(list);
                break;
            case 5:
                SortUtil.sortByQuantityDescending(list);
                System.out.println("\n[Sorted by Quantity Descending (High to Low) via Merge Sort]:");
                displayProductTable(list);
                break;
            case 6:
                return;
            default:
                System.out.println("[ERROR] Invalid sorting option.");
        }
    }

    private void handleAddStockDirectly() {
        System.out.println("\n--- ADD STOCK DIRECTLY ---");
        String id = readNonEmptyString("Enter Product ID: ");
        Product p = inventoryService.getProduct(id);
        if (p == null) {
            System.out.println("[ERROR] Product not found with ID: " + id);
            return;
        }

        System.out.println("Current product: " + p.getName() + " (Current Stock: " + p.getQuantity() + ")");
        int qty = readPositiveInt("Enter quantity to ADD (> 0): ");

        try {
            inventoryService.addStock(id, qty);
            System.out.println("[SUCCESS] Stock updated. New Quantity for " + p.getName() + ": " + p.getQuantity());
        } catch (Exception e) {
            System.out.println("[ERROR] " + e.getMessage());
        }
    }

    private void handleRemoveStockDirectly() {
        System.out.println("\n--- REMOVE STOCK DIRECTLY ---");
        String id = readNonEmptyString("Enter Product ID: ");
        Product p = inventoryService.getProduct(id);
        if (p == null) {
            System.out.println("[ERROR] Product not found with ID: " + id);
            return;
        }

        System.out.println("Current product: " + p.getName() + " (Current Stock: " + p.getQuantity() + ")");
        int qty = readPositiveInt("Enter quantity to REMOVE (> 0): ");

        boolean success = inventoryService.removeStock(id, qty);
        if (success) {
            System.out.println("[SUCCESS] Stock removed. Remaining Quantity for " + p.getName() + ": " + p.getQuantity());
        } else {
            System.out.println("[ERROR] Operation rejected! Requested: " + qty + ", but available stock is only " +
                    p.getQuantity() + ".");
            System.out.println("Stock quantity must NEVER become negative. Stock remains unchanged at: " + p.getQuantity());
        }
    }

    private void handleViewLowStockAlerts() {
        System.out.println("\n--- LOW-STOCK ALERTS (THRESHOLD: quantity <= reorderLevel) ---");
        List<Product> lowStock = inventoryService.getLowStockProducts();
        if (lowStock.isEmpty()) {
            System.out.println("All product stock levels are healthy! No low-stock alerts at this time.");
        } else {
            System.out.println("Found " + lowStock.size() + " product(s) requiring reorder attention:");
            displayProductTable(lowStock);
        }
    }

    private void handleCreateStockRequest() {
        System.out.println("\n--- CREATE STOCK REQUEST (FIFO QUEUE) ---");
        String productId = readNonEmptyString("Enter Product ID: ");
        Product p = inventoryService.getProduct(productId);
        if (p == null) {
            System.out.println("[ERROR] Product not found with ID: " + productId + ". Requests can only be created for valid products.");
            return;
        }

        System.out.println("Selected Product: " + p.getName() + " (Current Stock: " + p.getQuantity() + ")");
        System.out.println("Request Types:");
        System.out.println("  1. ADD (Restock replenishment)");
        System.out.println("  2. REMOVE (Order dispatch)");
        int typeChoice = readIntInput("Choose type (1 or 2): ");
        String type = (typeChoice == 1) ? "ADD" : (typeChoice == 2) ? "REMOVE" : null;
        if (type == null) {
            System.out.println("[ERROR] Invalid request type.");
            return;
        }

        int qty = readPositiveInt("Enter request quantity (> 0): ");
        String reqId = "REQ-" + (requestIdCounter++);

        try {
            StockRequest req = new StockRequest(reqId, productId, type, qty);
            inventoryService.enqueueRequest(req);
            System.out.println("[SUCCESS] Request enqueued at tail of FIFO queue: " + req);
            System.out.println("Queue count: " + inventoryService.getPendingRequests().size() + " pending request(s).");
        } catch (Exception e) {
            System.out.println("[ERROR] Could not enqueue request: " + e.getMessage());
        }
    }

    private void handleProcessNextRequest() {
        System.out.println("\n--- PROCESS NEXT STOCK REQUEST (FIFO DEQUEUE) ---");
        String result = inventoryService.processNextRequest();
        System.out.println("[PROCESS RESULT]: " + result);
    }

    private void handleProcessAllRequests() {
        System.out.println("\n--- PROCESS ALL STOCK REQUESTS (FIFO BATCH) ---");
        List<String> results = inventoryService.processAllRequests();
        for (String res : results) {
            System.out.println("-> " + res);
        }
    }

    private void handleViewPendingQueue() {
        System.out.println("\n--- PENDING STOCK REQUEST QUEUE (FIFO ORDER) ---");
        List<StockRequest> queueList = inventoryService.getPendingRequests();
        if (queueList.isEmpty()) {
            System.out.println("Request queue is currently empty.");
        } else {
            System.out.println(String.format("%-10s | %-12s | %-10s | %-8s | %-10s",
                    "Req ID", "Product ID", "Action", "Quantity", "Status"));
            System.out.println("------------------------------------------------------------");
            for (StockRequest r : queueList) {
                System.out.println(String.format("%-10s | %-12s | %-10s | %-8d | %-10s",
                        r.getRequestId(), r.getProductId(), r.getType(), r.getQuantity(), r.getStatus()));
            }
            System.out.println("------------------------------------------------------------");
            System.out.println("Total pending requests in queue: " + queueList.size() + " (Oldest is processed first)");
        }
    }

    private void handleViewTransactionHistory() {
        System.out.println("\n--- STOCK TRANSACTION AUDIT HISTORY ---");
        List<Transaction> txns = inventoryService.getTransactions();
        if (txns.isEmpty()) {
            System.out.println("No stock transactions recorded yet.");
        } else {
            System.out.println(String.format("%-10s | %-19s | %-8s | %-20s | %-12s | %-6s | %-15s",
                    "Txn ID", "Timestamp", "Prod ID", "Product Name", "Action", "Qty", "Resulting Stock"));
            System.out.println("--------------------------------------------------------------------------------------------------");
            for (Transaction t : txns) {
                System.out.println(String.format("%-10s | %-19s | %-8s | %-20s | %-12s | %-6d | %-15d",
                        t.getTransactionId(), t.getTimestamp(), t.getProductId(), t.getProductName(),
                        t.getType(), t.getQuantity(), t.getResultingStock()));
            }
            System.out.println("--------------------------------------------------------------------------------------------------");
            System.out.println("Total completed transactions: " + txns.size());
        }
    }

    private void handleDeleteProduct() {
        System.out.println("\n--- DELETE PRODUCT ---");
        String id = readNonEmptyString("Enter Product ID to delete (e.g. P106): ");
        Product p = inventoryService.getProduct(id);
        if (p == null) {
            System.out.println("[ERROR] Product not found with ID: " + id);
            return;
        }

        System.out.println("Product found: " + p.getName() + " (" + p.getId() + ")");
        System.out.print("Are you sure you want to delete this product? (yes/no): ");
        String confirm = scanner.nextLine().trim();
        if (!confirm.equalsIgnoreCase("yes") && !confirm.equalsIgnoreCase("y")) {
            System.out.println("[INFO] Deletion canceled by user.");
            return;
        }

        try {
            inventoryService.deleteProduct(id);
            System.out.println("[SUCCESS] Product " + id + " deleted successfully.");
        } catch (IllegalStateException e) {
            System.out.println("[ERROR] " + e.getMessage());
        } catch (Exception e) {
            System.out.println("[ERROR] Could not delete product: " + e.getMessage());
        }
    }

    // Helper table formatting methods

    private void displayProductTable(List<Product> products) {
        if (products == null || products.isEmpty()) {
            System.out.println("No products to display.");
            return;
        }

        printProductRowHeader();
        for (Product p : products) {
            printProductRow(p);
        }
        printProductRowFooter();
        System.out.println("Total count: " + products.size() + " product(s).");
    }

    private void printProductRowHeader() {
        System.out.println("+-------+------------------------+---------------+----------+-------+---------+---------------+");
        System.out.println("| ID    | Name                   | Category      | Price($) | Stock | Reorder | Status        |");
        System.out.println("+-------+------------------------+---------------+----------+-------+---------+---------------+");
    }

    private void printProductRow(Product p) {
        System.out.println(String.format("| %-5s | %-22s | %-13s | %8.2f | %5d | %7d | %-13s |",
                p.getId(),
                truncate(p.getName(), 22),
                truncate(p.getCategory(), 13),
                p.getPrice(),
                p.getQuantity(),
                p.getReorderLevel(),
                p.getStockStatus()));
    }

    private void printProductRowFooter() {
        System.out.println("+-------+------------------------+---------------+----------+-------+---------+---------------+");
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        return (s.length() <= maxLen) ? s : s.substring(0, maxLen - 3) + "...";
    }

    // Safe Scanner Input Helpers

    private int readIntInput(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("[ERROR] Invalid number format. Please enter an integer.");
            }
        }
    }

    private int readPositiveInt(String prompt) {
        while (true) {
            int val = readIntInput(prompt);
            if (val > 0) return val;
            System.out.println("[ERROR] Value must be strictly greater than 0.");
        }
    }

    private int readNonNegativeInt(String prompt) {
        while (true) {
            int val = readIntInput(prompt);
            if (val >= 0) return val;
            System.out.println("[ERROR] Value cannot be negative.");
        }
    }

    private double readPositiveDouble(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                double val = Double.parseDouble(input);
                if (val > 0) return val;
                System.out.println("[ERROR] Price must be strictly greater than 0.");
            } catch (NumberFormatException e) {
                System.out.println("[ERROR] Invalid decimal format. Please enter a valid number (e.g. 25.50).");
            }
        }
    }

    private String readNonEmptyString(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (!input.isEmpty()) return input;
            System.out.println("[ERROR] Input cannot be blank.");
        }
    }

    public InventoryService getInventoryService() {
        return inventoryService;
    }

    public static void main(String[] args) {
        ConsoleApp app = new ConsoleApp();
        app.run();
    }
}
