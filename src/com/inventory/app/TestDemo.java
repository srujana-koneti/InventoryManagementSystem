package com.inventory.app;

import com.inventory.dsa.SearchUtil;
import com.inventory.dsa.SortUtil;
import com.inventory.model.Product;
import com.inventory.model.StockRequest;
import com.inventory.service.InventoryService;

import java.util.List;

/**
 * Automated Verification Suite that validates the 7 core project test scenarios.
 */
public class TestDemo {

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("  INVENTORY MANAGEMENT SYSTEM - AUTOMATED VERIFICATION SUITE   ");
        System.out.println("===============================================================\n");

        InventoryService service = new InventoryService();

        // Seed initial products
        service.addProduct(new Product("P101", "Wireless Mouse", "Electronics", 25.50, 15, 10));
        service.addProduct(new Product("P102", "Mechanical Keyboard", "Electronics", 75.00, 4, 5));
        service.addProduct(new Product("P103", "USB-C Hub", "Accessories", 34.99, 2, 8));
        service.addProduct(new Product("P104", "Desk Chair", "Furniture", 189.00, 20, 5));
        service.addProduct(new Product("P105", "Desk Mat", "Furniture", 45.00, 0, 10));

        boolean allPassed = true;

        // -------------------------------------------------------------
        // Test 1: Search P102 using HashMap lookup O(1)
        // -------------------------------------------------------------
        System.out.println("--- TEST 1: HashMap Lookup for P102 ---");
        Product p102 = service.getProduct("P102");
        if (p102 != null && p102.getName().equals("Mechanical Keyboard")) {
            System.out.println("[PASS] Found product P102: " + p102.getName() + " in O(1) time.");
        } else {
            System.out.println("[FAIL] Product P102 not found or name mismatch.");
            allPassed = false;
        }

        // -------------------------------------------------------------
        // Test 2: Search P999
        // -------------------------------------------------------------
        System.out.println("\n--- TEST 2: Search Non-Existent Product P999 ---");
        Product p999 = service.getProduct("P999");
        if (p999 == null) {
            System.out.println("[PASS] Product P999 correctly returned null (Not Found).");
        } else {
            System.out.println("[FAIL] Unexpected product returned for P999.");
            allPassed = false;
        }

        // -------------------------------------------------------------
        // Test 3: View low-stock products (quantity <= reorderLevel)
        // -------------------------------------------------------------
        System.out.println("\n--- TEST 3: Low-Stock Products Detection ---");
        List<Product> lowStock = service.getLowStockProducts();
        System.out.println("Low stock items found: " + lowStock.size());
        boolean hasP102 = false, hasP103 = false, hasP105 = false;
        for (Product p : lowStock) {
            System.out.println(" -> " + p.getId() + ": " + p.getName() + " (Qty: " + p.getQuantity() + ", Reorder: " + p.getReorderLevel() + ")");
            if (p.getId().equals("P102")) hasP102 = true;
            if (p.getId().equals("P103")) hasP103 = true;
            if (p.getId().equals("P105")) hasP105 = true;
        }
        if (lowStock.size() == 3 && hasP102 && hasP103 && hasP105) {
            System.out.println("[PASS] Identified all expected low-stock items (P102, P103, P105).");
        } else {
            System.out.println("[FAIL] Low-stock detection mismatch.");
            allPassed = false;
        }

        // -------------------------------------------------------------
        // Test 4: Try removing 10 units from P102 when stock is 4
        // -------------------------------------------------------------
        System.out.println("\n--- TEST 4: Negative Stock Prevention ---");
        System.out.println("Initial P102 stock: " + p102.getQuantity());
        boolean removed = service.removeStock("P102", 10);
        System.out.println("Attempted to remove 10 units. Operation success: " + removed);
        System.out.println("Stock after attempted removal: " + p102.getQuantity());
        if (!removed && p102.getQuantity() == 4) {
            System.out.println("[PASS] Operation successfully rejected. Stock remains at 4. No negative stock allowed.");
        } else {
            System.out.println("[FAIL] Negative stock rule violated!");
            allPassed = false;
        }

        // -------------------------------------------------------------
        // Test 5: FIFO Stock Request Queue Processing
        // -------------------------------------------------------------
        System.out.println("\n--- TEST 5: FIFO Request Queue Processing ---");
        // Create requests in exact order:
        // REQ-1: ADD 10 P102
        // REQ-2: REMOVE 5 P101
        // REQ-3: REMOVE 10 P103
        StockRequest req1 = new StockRequest("REQ-1", "P102", "ADD", 10);
        StockRequest req2 = new StockRequest("REQ-2", "P101", "REMOVE", 5);
        StockRequest req3 = new StockRequest("REQ-3", "P103", "REMOVE", 10);

        service.enqueueRequest(req1);
        service.enqueueRequest(req2);
        service.enqueueRequest(req3);

        System.out.println("Enqueued 3 requests in order: REQ-1, REQ-2, REQ-3.");

        // Process #1: REQ-1 must be processed first
        String res1 = service.processNextRequest();
        System.out.println("1st Process: " + res1);
        Product p102After = service.getProduct("P102");
        boolean passReq1 = req1.getStatus().equals("COMPLETED") && p102After.getQuantity() == 14;

        // Process #2: REQ-2 must be processed second
        String res2 = service.processNextRequest();
        System.out.println("2nd Process: " + res2);
        Product p101After = service.getProduct("P101");
        boolean passReq2 = req2.getStatus().equals("COMPLETED") && p101After.getQuantity() == 10;

        // Process #3: REQ-3 must be processed third and REJECTED because P103 has 2 units
        String res3 = service.processNextRequest();
        System.out.println("3rd Process: " + res3);
        Product p103After = service.getProduct("P103");
        boolean passReq3 = req3.getStatus().equals("REJECTED") && p103After.getQuantity() == 2;

        if (passReq1 && passReq2 && passReq3) {
            System.out.println("[PASS] Strict FIFO queue verified! REQ-1 (P102=14), REQ-2 (P101=10), REQ-3 (REJECTED, P103=2).");
        } else {
            System.out.println("[FAIL] FIFO request processing failed.");
            allPassed = false;
        }

        // -------------------------------------------------------------
        // Test 6: Sort products by price ascending using custom Merge Sort
        // -------------------------------------------------------------
        System.out.println("\n--- TEST 6: Custom Merge Sort by Price Ascending ---");
        List<Product> productsToSort = service.getAllProducts();
        SortUtil.sortByPriceAscending(productsToSort);

        System.out.println("Sorted products by price:");
        for (Product p : productsToSort) {
            System.out.println(" -> " + p.getId() + " | Price: $" + String.format("%.2f", p.getPrice()) + " | " + p.getName());
        }

        String[] expectedOrder = {"P101", "P103", "P105", "P102", "P104"};
        boolean sortMatches = true;
        for (int i = 0; i < expectedOrder.length; i++) {
            if (!productsToSort.get(i).getId().equals(expectedOrder[i])) {
                sortMatches = false;
                break;
            }
        }
        if (sortMatches) {
            System.out.println("[PASS] Merge Sort produced expected order: P101 (25.50) -> P103 (34.99) -> P105 (45.00) -> P102 (75.00) -> P104 (189.00).");
        } else {
            System.out.println("[FAIL] Sorting order mismatch.");
            allPassed = false;
        }

        // -------------------------------------------------------------
        // Test 7: Binary Search for 'Mechanical Keyboard'
        // -------------------------------------------------------------
        System.out.println("\n--- TEST 7: Binary Search for 'Mechanical Keyboard' ---");
        List<Product> nameList = service.getAllProducts();
        // Mandatory Pre-condition: sort by Name using Merge Sort
        SortUtil.sortByNameAscending(nameList);
        System.out.println("Sorted by Name for Binary Search:");
        for (int i = 0; i < nameList.size(); i++) {
            System.out.println(" [" + i + "] " + nameList.get(i).getName());
        }

        Product found = SearchUtil.binarySearchByName(nameList, "Mechanical Keyboard");
        if (found != null && found.getId().equals("P102")) {
            System.out.println("[PASS] Binary Search successfully located 'Mechanical Keyboard' (ID: " + found.getId() + ") in O(log n) time.");
        } else {
            System.out.println("[FAIL] Binary search did not find target product.");
            allPassed = false;
        }

        // -------------------------------------------------------------
        // Test 8: Add New Product & Duplicate ID Rejection
        // -------------------------------------------------------------
        System.out.println("\n--- TEST 8: Add Product & Duplicate ID Protection ---");
        Product p106 = new Product("P106", "Laptop Stand", "Accessories", 29.90, 10, 5);
        service.addProduct(p106);
        Product retrievedP106 = service.getProduct("P106");
        boolean p106Added = (retrievedP106 != null && retrievedP106.getName().equals("Laptop Stand"));

        boolean duplicateRejected = false;
        try {
            service.addProduct(new Product("P106", "Duplicate Stand", "Accessories", 35.00, 5, 2));
        } catch (IllegalArgumentException e) {
            duplicateRejected = true;
        }

        if (p106Added && duplicateRejected) {
            System.out.println("[PASS] Product P106 added successfully and duplicate P106 addition was correctly rejected.");
        } else {
            System.out.println("[FAIL] Add product or duplicate rejection failed.");
            allPassed = false;
        }

        // -------------------------------------------------------------
        // Test 9: Safe Delete Product (No pending requests)
        // -------------------------------------------------------------
        System.out.println("\n--- TEST 9: Safe Delete Product ---");
        boolean deletedP106 = service.deleteProduct("P106");
        boolean p106Gone = (service.getProduct("P106") == null);
        if (deletedP106 && p106Gone) {
            System.out.println("[PASS] Product P106 deleted successfully and is no longer found in HashMap.");
        } else {
            System.out.println("[FAIL] Product deletion failed.");
            allPassed = false;
        }

        // -------------------------------------------------------------
        // Test 10: Delete Product Rejection when Pending Request Exists
        // -------------------------------------------------------------
        System.out.println("\n--- TEST 10: Delete Rejection with Pending Requests ---");
        service.enqueueRequest(new StockRequest("REQ-4", "P101", "ADD", 5));
        boolean pendingDeleteBlocked = false;
        try {
            service.deleteProduct("P101");
        } catch (IllegalStateException e) {
            pendingDeleteBlocked = true;
            System.out.println("Blocked with expected message: " + e.getMessage());
        }
        if (pendingDeleteBlocked && service.getProduct("P101") != null) {
            System.out.println("[PASS] Deletion of P101 correctly blocked because of pending stock requests in queue.");
        } else {
            System.out.println("[FAIL] Product with pending requests was deleted or not protected!");
            allPassed = false;
        }

        System.out.println("\n===============================================================");
        if (allPassed) {
            System.out.println("  ALL TESTS PASSED SUCCESSFULLY! SYSTEM INTEGRITY VERIFIED.    ");
        } else {
            System.out.println("  ONE OR MORE TESTS FAILED. PLEASE CHECK LOGS.                 ");
        }
        System.out.println("===============================================================");
    }
}
