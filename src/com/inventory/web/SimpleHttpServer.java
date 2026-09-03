package com.inventory.web;

import com.inventory.model.Product;
import com.inventory.model.StockRequest;
import com.inventory.model.Transaction;
import com.inventory.service.InventoryService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Lightweight HTTP server built entirely with standard JDK com.sun.net.httpserver.HttpServer.
 * Provides REST-like JSON API endpoints and serves frontend static files.
 * Zero external libraries or frameworks.
 */
public class SimpleHttpServer {

    public static final int PORT = 8080;
    private final HttpServer server;
    private final InventoryService inventoryService;
    private int webRequestIdCounter = 100;

    public SimpleHttpServer(InventoryService inventoryService) throws IOException {
        this.inventoryService = inventoryService;
        this.server = HttpServer.create(new InetSocketAddress(PORT), 0);
        registerRoutes();
    }

    private void registerRoutes() {
        // API Endpoints
        server.createContext("/api/auth/login", new AuthLoginHandler());
        server.createContext("/api/products", new ProductsHandler());
        server.createContext("/api/low-stock", new LowStockHandler());
        server.createContext("/api/requests", new RequestsHandler());
        server.createContext("/api/requests/process", new ProcessRequestHandler());
        server.createContext("/api/transactions", new TransactionsHandler());

        // Static Web Dashboard Files
        server.createContext("/", new StaticFileHandler());

        server.setExecutor(null); // default executor
    }

    public void start() {
        server.start();
        System.out.println("=================================================");
        System.out.println(" Web Dashboard is running at: http://localhost:" + PORT);
        System.out.println(" API Endpoints available:");
        System.out.println("   POST /api/auth/login");
        System.out.println("   GET  /api/products");
        System.out.println("   GET  /api/low-stock");
        System.out.println("   GET  /api/requests");
        System.out.println("   POST /api/requests/process");
        System.out.println("   GET  /api/transactions");
        System.out.println("=================================================");
    }

    public void stop() {
        server.stop(0);
        System.out.println("Web server stopped.");
    }

    // -------------------------------------------------------------------------
    // API Handlers
    // -------------------------------------------------------------------------

    /**
     * Handles POST /api/auth/login
     */
    private class AuthLoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }

            String body = readRequestBody(exchange);
            String username = extractJsonField(body, "username").trim();
            String password = extractJsonField(body, "password").trim();

            // Staff: Username "STAFF" (case-insensitive), password "staff"
            if (username.equalsIgnoreCase("STAFF") && password.equals("staff")) {
                String userJson = "{\"success\":true,\"user\":{\"username\":\"STAFF\",\"role\":\"STAFF\",\"displayName\":\"Staff Member\"}}";
                sendJsonResponse(exchange, 200, userJson);
                return;
            }

            // Manager: Username "Manager" or Email "manager@inventory.com" (case-insensitive), password "manager@123"
            if ((username.equalsIgnoreCase("Manager") || username.equalsIgnoreCase("manager@inventory.com")) && password.equals("manager@123")) {
                String userJson = "{\"success\":true,\"user\":{\"username\":\"Manager\",\"role\":\"MANAGER\",\"displayName\":\"Inventory Manager\"}}";
                sendJsonResponse(exchange, 200, userJson);
                return;
            }

            sendJsonResponse(exchange, 401, "{\"success\":false,\"message\":\"Invalid username/email or password.\"}");
        }
    }

    /**
     * Handles:
     * - GET    /api/products          (returns all products)
     * - GET    /api/products/{id}     (returns single product)
     * - POST   /api/products          (adds a new product)
     * - DELETE /api/products/{id}     (deletes a product)
     */
    private class ProductsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            String method = exchange.getRequestMethod();

            if ("OPTIONS".equalsIgnoreCase(method)) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            String path = exchange.getRequestURI().getPath();

            if ("GET".equalsIgnoreCase(method)) {
                if (path.equals("/api/products") || path.equals("/api/products/")) {
                    List<Product> products = inventoryService.getAllProducts();
                    StringBuilder json = new StringBuilder("[");
                    for (int i = 0; i < products.size(); i++) {
                        json.append(productToJson(products.get(i)));
                        if (i < products.size() - 1) json.append(",");
                    }
                    json.append("]");
                    sendJsonResponse(exchange, 200, json.toString());
                } else if (path.startsWith("/api/products/")) {
                    String id = path.substring("/api/products/".length()).trim();
                    Product p = inventoryService.getProduct(id);
                    if (p != null) {
                        sendJsonResponse(exchange, 200, productToJson(p));
                    } else {
                        sendJsonResponse(exchange, 404, "{\"success\":false,\"message\":\"Product " + escapeJson(id) + " not found.\"}");
                    }
                } else {
                    sendJsonResponse(exchange, 404, "{\"success\":false,\"message\":\"Endpoint not found.\"}");
                }
            } else if ("POST".equalsIgnoreCase(method)) {
                String body = readRequestBody(exchange);
                try {
                    String id = extractJsonField(body, "id");
                    if (id.isEmpty()) {
                        id = extractJsonField(body, "productId");
                    }
                    String name = extractJsonField(body, "name");
                    String category = extractJsonField(body, "category");
                    String priceStr = extractJsonField(body, "price");
                    String qtyStr = extractJsonField(body, "quantity");
                    if (qtyStr.isEmpty()) {
                        qtyStr = extractJsonField(body, "initialStock");
                    }
                    String reorderStr = extractJsonField(body, "reorderLevel");

                    if (id == null || id.trim().isEmpty()) {
                        sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"Product ID cannot be empty.\"}");
                        return;
                    }
                    if (name == null || name.trim().isEmpty()) {
                        sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"Product Name cannot be empty.\"}");
                        return;
                    }
                    if (priceStr.isEmpty()) {
                        sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"Price is required.\"}");
                        return;
                    }
                    double price;
                    try {
                        price = Double.parseDouble(priceStr);
                    } catch (NumberFormatException e) {
                        sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"Invalid price format.\"}");
                        return;
                    }
                    if (price <= 0) {
                        sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"Price must be strictly greater than 0.\"}");
                        return;
                    }

                    int quantity = 0;
                    if (!qtyStr.isEmpty()) {
                        try {
                            quantity = Integer.parseInt(qtyStr);
                        } catch (NumberFormatException e) {
                            sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"Invalid stock quantity format.\"}");
                            return;
                        }
                    }
                    if (quantity < 0) {
                        sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"Initial stock quantity must not be negative.\"}");
                        return;
                    }

                    int reorderLevel = 0;
                    if (!reorderStr.isEmpty()) {
                        try {
                            reorderLevel = Integer.parseInt(reorderStr);
                        } catch (NumberFormatException e) {
                            sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"Invalid reorder level format.\"}");
                            return;
                        }
                    }
                    if (reorderLevel < 0) {
                        sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"Reorder level must not be negative.\"}");
                        return;
                    }

                    // Check for duplicate ID using inventoryService
                    if (inventoryService.getProduct(id.trim()) != null) {
                        sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"Product ID already exists: " + escapeJson(id.trim()) + "\"}");
                        return;
                    }

                    Product newProduct = new Product(id.trim(), name.trim(), category, price, quantity, reorderLevel);
                    inventoryService.addProduct(newProduct);

                    sendJsonResponse(exchange, 201, "{\"success\":true,\"message\":\"Product " + escapeJson(newProduct.getId()) + " added successfully.\"}");
                } catch (IllegalArgumentException e) {
                    sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"" + escapeJson(e.getMessage()) + "\"}");
                } catch (Exception e) {
                    sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"Failed to add product: " + escapeJson(e.getMessage()) + "\"}");
                }
            } else if ("DELETE".equalsIgnoreCase(method)) {
                if (!path.startsWith("/api/products/")) {
                    sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"Product ID is required in URL path (e.g. /api/products/{id}).\"}");
                    return;
                }
                String productId = path.substring("/api/products/".length()).trim();
                if (productId.isEmpty()) {
                    sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"Product ID cannot be empty.\"}");
                    return;
                }

                try {
                    inventoryService.deleteProduct(productId);
                    sendJsonResponse(exchange, 200, "{\"success\":true,\"message\":\"Product " + escapeJson(productId) + " deleted successfully.\"}");
                } catch (IllegalArgumentException e) {
                    // Product not found
                    sendJsonResponse(exchange, 404, "{\"success\":false,\"message\":\"Product " + escapeJson(productId) + " not found.\"}");
                } catch (IllegalStateException e) {
                    // Has pending requests
                    sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"" + escapeJson(e.getMessage()) + "\"}");
                } catch (Exception e) {
                    sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"Error deleting product: " + escapeJson(e.getMessage()) + "\"}");
                }
            } else if ("PUT".equalsIgnoreCase(method)) {
                if (!path.startsWith("/api/products/")) {
                    sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"Product ID is required in URL path (e.g. /api/products/{id}).\"}");
                    return;
                }
                String productId = path.substring("/api/products/".length()).trim();
                if (productId.isEmpty()) {
                    sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"Product ID cannot be empty.\"}");
                    return;
                }

                String body = readRequestBody(exchange);
                try {
                    String name = extractJsonField(body, "name");
                    String category = extractJsonField(body, "category");
                    String priceStr = extractJsonField(body, "price");
                    String qtyStr = extractJsonField(body, "quantity");
                    String reorderStr = extractJsonField(body, "reorderLevel");

                    if (name == null || name.trim().isEmpty()) {
                        sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"Product Name cannot be empty.\"}");
                        return;
                    }
                    if (priceStr.isEmpty()) {
                        sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"Price is required.\"}");
                        return;
                    }
                    double price;
                    try {
                        price = Double.parseDouble(priceStr);
                    } catch (NumberFormatException e) {
                        sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"Invalid price format.\"}");
                        return;
                    }
                    if (price <= 0) {
                        sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"Price must be strictly greater than 0.\"}");
                        return;
                    }

                    int quantity = 0;
                    if (!qtyStr.isEmpty()) {
                        try {
                            quantity = Integer.parseInt(qtyStr);
                        } catch (NumberFormatException e) {
                            sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"Invalid stock quantity format.\"}");
                            return;
                        }
                    }
                    if (quantity < 0) {
                        sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"Stock quantity must not be negative.\"}");
                        return;
                    }

                    int reorderLevel = 0;
                    if (!reorderStr.isEmpty()) {
                        try {
                            reorderLevel = Integer.parseInt(reorderStr);
                        } catch (NumberFormatException e) {
                            sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"Invalid reorder level format.\"}");
                            return;
                        }
                    }
                    if (reorderLevel < 0) {
                        sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"Reorder level must not be negative.\"}");
                        return;
                    }

                    inventoryService.updateProduct(productId, name.trim(), category, price, quantity, reorderLevel);
                    sendJsonResponse(exchange, 200, "{\"success\":true,\"message\":\"Product " + escapeJson(productId) + " updated successfully.\"}");
                } catch (IllegalArgumentException e) {
                    sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"" + escapeJson(e.getMessage()) + "\"}");
                } catch (Exception e) {
                    sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"Error updating product: " + escapeJson(e.getMessage()) + "\"}");
                }
            } else {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            }
        }
    }

    /**
     * Handles GET /api/low-stock
     */
    private class LowStockHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                List<Product> lowStock = inventoryService.getLowStockProducts();
                StringBuilder json = new StringBuilder("[");
                for (int i = 0; i < lowStock.size(); i++) {
                    json.append(productToJson(lowStock.get(i)));
                    if (i < lowStock.size() - 1) json.append(",");
                }
                json.append("]");
                sendJsonResponse(exchange, 200, json.toString());
            } else {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            }
        }
    }

    /**
     * Handles GET /api/requests and POST /api/requests
     */
    private class RequestsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                List<StockRequest> requests = inventoryService.getPendingRequests();
                StringBuilder json = new StringBuilder("[");
                for (int i = 0; i < requests.size(); i++) {
                    json.append(requestToJson(requests.get(i)));
                    if (i < requests.size() - 1) json.append(",");
                }
                json.append("]");
                sendJsonResponse(exchange, 200, json.toString());
            } else if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                // Parse body (e.g. {"productId":"P101","type":"ADD","quantity":5})
                String body = readRequestBody(exchange);
                try {
                    String productId = extractJsonField(body, "productId");
                    String type = extractJsonField(body, "type");
                    String qtyStr = extractJsonField(body, "quantity");
                    int qty = Integer.parseInt(qtyStr);

                    String reqId = "REQ-W" + (webRequestIdCounter++);
                    StockRequest req = new StockRequest(reqId, productId, type, qty);
                    inventoryService.enqueueRequest(req);

                    sendJsonResponse(exchange, 201, "{\"message\":\"Stock request submitted successfully.\",\"requestId\":\"" + reqId + "\"}");
                } catch (Exception e) {
                    sendJsonResponse(exchange, 400, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
                }
            } else {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            }
        }
    }

    /**
     * Handles POST /api/requests/process
     */
    private class ProcessRequestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String result = inventoryService.processNextRequest();
                sendJsonResponse(exchange, 200, "{\"result\":\"" + escapeJson(result) + "\"}");
            } else {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            }
        }
    }

    /**
     * Handles GET /api/transactions
     */
    private class TransactionsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                List<Transaction> txns = inventoryService.getTransactions();
                StringBuilder json = new StringBuilder("[");
                for (int i = 0; i < txns.size(); i++) {
                    json.append(transactionToJson(txns.get(i)));
                    if (i < txns.size() - 1) json.append(",");
                }
                json.append("]");
                sendJsonResponse(exchange, 200, json.toString());
            } else {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            }
        }
    }

    /**
     * Static file handler serving frontend files: index.html, style.css, app.js
     */
    private static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path == null || path.equals("/") || path.isEmpty()) {
                path = "/index.html";
            }

            // Map requested path to local frontend folder
            File file = new File("frontend" + path);
            if (!file.exists() || file.isDirectory()) {
                file = new File("frontend/index.html");
            }

            if (!file.exists()) {
                String notFound = "404 Not Found - Frontend assets missing.";
                exchange.sendResponseHeaders(404, notFound.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(notFound.getBytes());
                }
                return;
            }

            String contentType = "text/html";
            if (file.getName().endsWith(".css")) {
                contentType = "text/css";
            } else if (file.getName().endsWith(".js")) {
                contentType = "application/javascript";
            } else if (file.getName().endsWith(".json")) {
                contentType = "application/json";
            }

            exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=UTF-8");
            exchange.sendResponseHeaders(200, file.length());

            try (InputStream is = new FileInputStream(file);
                 OutputStream os = exchange.getResponseBody()) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Helper serialization methods (Zero External Dependencies)
    // -------------------------------------------------------------------------

    private static String productToJson(Product p) {
        return String.format(
                "{\"id\":\"%s\",\"name\":\"%s\",\"category\":\"%s\",\"price\":%.2f,\"quantity\":%d,\"reorderLevel\":%d,\"isLowStock\":%b,\"status\":\"%s\"}",
                escapeJson(p.getId()),
                escapeJson(p.getName()),
                escapeJson(p.getCategory()),
                p.getPrice(),
                p.getQuantity(),
                p.getReorderLevel(),
                p.isLowStock(),
                escapeJson(p.getStockStatus())
        );
    }

    private static String requestToJson(StockRequest r) {
        return String.format(
                "{\"requestId\":\"%s\",\"productId\":\"%s\",\"type\":\"%s\",\"quantity\":%d,\"status\":\"%s\"}",
                escapeJson(r.getRequestId()),
                escapeJson(r.getProductId()),
                escapeJson(r.getType()),
                r.getQuantity(),
                escapeJson(r.getStatus())
        );
    }

    private static String transactionToJson(Transaction t) {
        return String.format(
                "{\"transactionId\":\"%s\",\"timestamp\":\"%s\",\"productId\":\"%s\",\"productName\":\"%s\",\"type\":\"%s\",\"quantity\":%d,\"resultingStock\":%d}",
                escapeJson(t.getTransactionId()),
                escapeJson(t.getTimestamp()),
                escapeJson(t.getProductId()),
                escapeJson(t.getProductName()),
                escapeJson(t.getType()),
                t.getQuantity(),
                t.getResultingStock()
        );
    }

    private static void sendJsonResponse(HttpExchange exchange, int statusCode, String responseJson) throws IOException {
        byte[] bytes = responseJson.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }

    private static String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            byte[] bytes = is.readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    private static String extractJsonField(String json, String field) {
        if (json == null || field == null) return "";
        String key = "\"" + field + "\"";
        int keyIndex = json.indexOf(key);
        int afterKey;
        if (keyIndex != -1) {
            afterKey = keyIndex + key.length();
        } else {
            keyIndex = json.indexOf(field);
            if (keyIndex == -1) return "";
            afterKey = keyIndex + field.length();
        }

        int colonIndex = json.indexOf(":", afterKey);
        if (colonIndex == -1) return "";

        int start = colonIndex + 1;
        while (start < json.length() && (json.charAt(start) == ' ' || json.charAt(start) == '\t' || json.charAt(start) == '\r' || json.charAt(start) == '\n')) {
            start++;
        }
        if (start >= json.length()) return "";

        boolean inQuotes = false;
        if (json.charAt(start) == '\"') {
            inQuotes = true;
            start++; // move past opening quote
        }

        int end = start;
        if (inQuotes) {
            while (end < json.length() && json.charAt(end) != '\"') {
                if (json.charAt(end) == '\\' && end + 1 < json.length()) {
                    end += 2;
                } else {
                    end++;
                }
            }
        } else {
            while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}' && json.charAt(end) != ']' && json.charAt(end) != ' ' && json.charAt(end) != '\r' && json.charAt(end) != '\n') {
                end++;
            }
        }
        return json.substring(start, end).trim();
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }

    /**
     * Standalone main method to run the Web Server with seeded sample products.
     */
    public static void main(String[] args) {
        try {
            InventoryService service = new InventoryService();
            // Seed sample data
            service.addProduct(new Product("P101", "Wireless Mouse", "Electronics", 25.50, 15, 10));
            service.addProduct(new Product("P102", "Mechanical Keyboard", "Electronics", 75.00, 4, 5));
            service.addProduct(new Product("P103", "USB-C Hub", "Accessories", 34.99, 2, 8));
            service.addProduct(new Product("P104", "Desk Chair", "Furniture", 189.00, 20, 5));
            service.addProduct(new Product("P105", "Desk Mat", "Furniture", 45.00, 0, 10));

            SimpleHttpServer server = new SimpleHttpServer(service);
            server.start();
            System.out.println("Press Ctrl+C to terminate the web server.");
        } catch (Exception e) {
            System.err.println("Failed to start HTTP server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
