package com.retail.controller;

import com.retail.model.Order;
import com.retail.model.Product;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class RetailController {

    private final Map<String, Product> productCatalog = new ConcurrentHashMap<>();
    private final List<Order> orderHistory = new CopyOnWriteArrayList<>();

    // Micrometer Prometheus Custom Metrics
    private final Counter orderCounter;
    private final Counter orderRevenueCounter;
    private final Counter errorCounter;
    private final Timer orderProcessingTimer;
    private final AtomicInteger activeShoppers = new AtomicInteger(128);

    // Simulation Flags for Live Reliability Testing
    private volatile boolean simulateLatencySpike = false;
    private volatile int artificialLatencyMs = 0;
    private volatile boolean simulateErrors = false;

    public RetailController(MeterRegistry meterRegistry) {
        // Register custom Prometheus metrics
        this.orderCounter = Counter.builder("retail_orders_total")
                .description("Total number of orders placed in the retail store")
                .tag("environment", "production")
                .register(meterRegistry);

        this.orderRevenueCounter = Counter.builder("retail_order_revenue_usd")
                .description("Total revenue generated from orders in USD")
                .tag("environment", "production")
                .register(meterRegistry);

        this.errorCounter = Counter.builder("retail_simulated_errors_total")
                .description("Total number of simulated server errors")
                .tag("environment", "production")
                .register(meterRegistry);

        this.orderProcessingTimer = Timer.builder("retail_order_processing_duration_seconds")
                .description("Time taken to process and validate retail orders")
                .publishPercentileHistogram()
                .register(meterRegistry);

        meterRegistry.gauge("retail_active_shoppers", activeShoppers);

        seedInitialProducts();
    }

    private void seedInitialProducts() {
        productCatalog.put("PROD-001", new Product(
                "PROD-001", "MacBook Pro M3 Max", "Computers", 2499.00, 35,
                "https://images.unsplash.com/photo-1517336714731-489689fd1ca8?auto=format&fit=crop&w=600&q=80",
                "BESTSELLER"));
        productCatalog.put("PROD-002", new Product(
                "PROD-002", "Sony WH-1000XM5 ANC", "Audio", 398.00, 50,
                "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=600&q=80",
                "POPULAR"));
        productCatalog.put("PROD-003", new Product(
                "PROD-003", "Ergonomic Herman Chair", "Furniture", 899.00, 20,
                "https://images.unsplash.com/photo-1580481077195-c3a821a5060f?auto=format&fit=crop&w=600&q=80",
                "PREMIUM"));
        productCatalog.put("PROD-004", new Product(
                "PROD-004", "UltraWide Curved 38' Monitor", "Displays", 1199.00, 15,
                "https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?auto=format&fit=crop&w=600&q=80",
                "HOT SALE"));
        productCatalog.put("PROD-005", new Product(
                "PROD-005", "Mechanical Keychron Q1 Pro", "Accessories", 199.00, 80,
                "https://images.unsplash.com/photo-1587829741301-dc798b83add3?auto=format&fit=crop&w=600&q=80",
                "TRENDING"));
        productCatalog.put("PROD-006", new Product(
                "PROD-006", "Smart Enterprise Security Gateway", "Networking", 650.00, 42,
                "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?auto=format&fit=crop&w=600&q=80",
                "ENTERPRISE"));
    }

    @GetMapping("/products")
    public ResponseEntity<List<Product>> getAllProducts() {
        applyArtificialLatency();
        if (simulateErrors) {
            errorCounter.increment();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        return ResponseEntity.ok(new ArrayList<>(productCatalog.values()));
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable String id) {
        applyArtificialLatency();
        Product product = productCatalog.get(id);
        if (product == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(product);
    }

    @PostMapping("/orders")
    public ResponseEntity<?> createOrder(@RequestBody Order order) {
        return orderProcessingTimer.record(() -> {
            applyArtificialLatency();

            if (simulateErrors) {
                errorCounter.increment();
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(Collections.singletonMap("error", "Simulated Retail Checkout Outage (503)"));
            }

            if (order.getItems() == null || order.getItems().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Collections.singletonMap("error", "Order items list cannot be empty"));
            }

            // Generate Order ID if absent
            if (order.getOrderId() == null || order.getOrderId().isEmpty()) {
                order.setOrderId("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            }

            double total = 0.0;
            for (Order.OrderItem item : order.getItems()) {
                Product p = productCatalog.get(item.getProductId());
                if (p != null) {
                    item.setProductName(p.getName());
                    item.setPrice(p.getPrice());
                    total += p.getPrice() * item.getQuantity();
                    // decrement stock safely
                    p.setStock(Math.max(0, p.getStock() - item.getQuantity()));
                }
            }
            order.setTotalAmount(Math.round(total * 100.0) / 100.0);
            order.setStatus("CONFIRMED");
            orderHistory.add(0, order);

            // Record Prometheus metric
            orderCounter.increment();
            orderRevenueCounter.increment(order.getTotalAmount());

            return ResponseEntity.status(HttpStatus.CREATED).body(order);
        });
    }

    @GetMapping("/orders")
    public ResponseEntity<List<Order>> getRecentOrders() {
        int limit = Math.min(orderHistory.size(), 10);
        return ResponseEntity.ok(orderHistory.subList(0, limit));
    }

    @GetMapping("/metrics/summary")
    public ResponseEntity<Map<String, Object>> getMetricsSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("status", "UP");
        summary.put("ordersPlacedTotal", (long) orderCounter.count());
        summary.put("revenueGeneratedTotal", orderRevenueCounter.count());
        summary.put("activeShoppers", activeShoppers.get());
        summary.put("catalogItemCount", productCatalog.size());
        summary.put("simulateLatencySpikeActive", simulateLatencySpike);
        summary.put("artificialLatencyMs", artificialLatencyMs);
        summary.put("simulateErrorsActive", simulateErrors);
        summary.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(summary);
    }

    // Reliability & Alert Simulation Endpoints
    @PostMapping("/simulate/latency")
    public ResponseEntity<Map<String, Object>> toggleLatencySpike(@RequestParam(defaultValue = "600") int delayMs) {
        this.simulateLatencySpike = true;
        this.artificialLatencyMs = delayMs;
        activeShoppers.set(activeShoppers.get() + 350);
        return ResponseEntity.ok(Map.of(
                "message", "Simulated High Latency Spike Activated (> 500ms will trigger Grafana Alert)",
                "delayMs", delayMs,
                "simulateLatencySpike", true
        ));
    }

    @PostMapping("/simulate/error")
    public ResponseEntity<Map<String, Object>> toggleErrorSimulation(@RequestParam(defaultValue = "true") boolean enabled) {
        this.simulateErrors = enabled;
        return ResponseEntity.ok(Map.of(
                "message", enabled ? "Simulated 5xx Outage Activated (Will trigger Error Rate Alert)" : "Error simulation cleared",
                "simulateErrors", enabled
        ));
    }

    @PostMapping("/simulate/reset")
    public ResponseEntity<Map<String, Object>> resetSimulation() {
        this.simulateLatencySpike = false;
        this.artificialLatencyMs = 0;
        this.simulateErrors = false;
        this.activeShoppers.set(128);
        return ResponseEntity.ok(Map.of(
                "message", "Simulation normalized. Latency and errors cleared.",
                "status", "NORMAL"
        ));
    }

    private void applyArtificialLatency() {
        if (simulateLatencySpike && artificialLatencyMs > 0) {
            try {
                TimeUnit.MILLISECONDS.sleep(artificialLatencyMs);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
