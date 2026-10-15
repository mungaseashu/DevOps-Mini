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

    // Prometheus Custom Metrics for Shoe Retail
    private final Counter sneakerOrdersCounter;
    private final Counter sneakerRevenueCounter;
    private final Counter errorCounter;
    private final Timer orderProcessingTimer;
    private final AtomicInteger activeDropShoppers = new AtomicInteger(240);

    // Latency & Error Injection Flags for SRE Testing
    private volatile boolean simulateLatencySpike = false;
    private volatile int artificialLatencyMs = 0;
    private volatile boolean simulateErrors = false;

    public RetailController(MeterRegistry meterRegistry) {
        // Register custom Prometheus metrics for Shoe Drops
        this.sneakerOrdersCounter = Counter.builder("sneaker_orders_total")
                .description("Total number of sneaker orders confirmed")
                .tag("store", "cloudkicks")
                .tag("environment", "production")
                .register(meterRegistry);

        this.sneakerRevenueCounter = Counter.builder("sneaker_revenue_usd")
                .description("Total sneaker sales revenue in USD")
                .tag("store", "cloudkicks")
                .tag("environment", "production")
                .register(meterRegistry);

        this.errorCounter = Counter.builder("sneaker_drop_errors_total")
                .description("Total checkout drop errors during peak traffic")
                .tag("store", "cloudkicks")
                .register(meterRegistry);

        this.orderProcessingTimer = Timer.builder("sneaker_order_processing_seconds")
                .description("Time taken to authenticate and process sneaker checkout transactions")
                .publishPercentileHistogram()
                .register(meterRegistry);

        // Aliases for general retail metric compatibility
        Counter.builder("retail_orders_total").register(meterRegistry);
        Counter.builder("retail_order_revenue_usd").register(meterRegistry);

        meterRegistry.gauge("sneaker_drop_waitlist_active", activeDropShoppers);

        seedShoeCatalog();
    }

    private void seedShoeCatalog() {
        productCatalog.put("SHOE-001", new Product(
                "SHOE-001", "Air Jordan 1 High OG 'Lost & Found'", "Nike / Jordan", "Basketball & Lifestyle",
                185.00, 24, List.of("US 8", "US 9", "US 9.5", "US 10", "US 11", "US 12"),
                "https://images.unsplash.com/photo-1552346154-21d32810aba3?auto=format&fit=crop&w=700&q=80",
                "HOT RELEASE"));

        productCatalog.put("SHOE-002", new Product(
                "SHOE-002", "Ultraboost Light Performance Carbon", "Adidas", "Running & Athletic",
                190.00, 48, List.of("US 7.5", "US 8.5", "US 9.5", "US 10.5", "US 11.5"),
                "https://images.unsplash.com/photo-1584735935682-2f2b69dff9d2?auto=format&fit=crop&w=700&q=80",
                "BESTSELLER"));

        productCatalog.put("SHOE-003", new Product(
                "SHOE-003", "New Balance 990v6 Made in USA", "New Balance", "Lifestyle & Heritage",
                219.99, 18, List.of("US 8", "US 9", "US 10", "US 10.5", "US 11"),
                "https://images.unsplash.com/photo-1539185441755-769473a23570?auto=format&fit=crop&w=700&q=80",
                "LIMITED DROP"));

        productCatalog.put("SHOE-004", new Product(
                "SHOE-004", "Gel-Kayano 14 Silver Metallic", "ASICS", "Sportstyle",
                160.00, 32, List.of("US 8.5", "US 9", "US 10", "US 11", "US 12"),
                "https://images.unsplash.com/photo-1608231387042-66d1773070a5?auto=format&fit=crop&w=700&q=80",
                "TRENDING"));

        productCatalog.put("SHOE-005", new Product(
                "SHOE-005", "Deviate Nitro Elite Carbon Racer", "Puma", "Marathon Racing",
                230.00, 15, List.of("US 8", "US 9", "US 9.5", "US 10", "US 11"),
                "https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=700&q=80",
                "ELITE SPEED"));

        productCatalog.put("SHOE-006", new Product(
                "SHOE-006", "Triple S Chunky Clear Sneaker", "Balenciaga", "Luxury High-Fashion",
                1050.00, 8, List.of("US 8", "US 9", "US 10", "US 11"),
                "https://images.unsplash.com/photo-1595950653106-6c9ebd614d3a?auto=format&fit=crop&w=700&q=80",
                "EXCLUSIVE"));
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
                        .body(Collections.singletonMap("error", "Sneaker Drop Traffic Outage (503): High Queue"));
            }

            if (order.getItems() == null || order.getItems().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Collections.singletonMap("error", "No sneakers selected in order items"));
            }

            if (order.getOrderId() == null || order.getOrderId().isEmpty()) {
                order.setOrderId("KICKS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            }

            double total = 0.0;
            for (Order.OrderItem item : order.getItems()) {
                Product p = productCatalog.get(item.getProductId());
                if (p != null) {
                    item.setProductName(p.getName());
                    item.setPrice(p.getPrice());
                    if (item.getSize() == null || item.getSize().isEmpty()) {
                        item.setSize(p.getAvailableSizes().get(0));
                    }
                    total += p.getPrice() * item.getQuantity();
                    p.setStock(Math.max(0, p.getStock() - item.getQuantity()));
                }
            }

            order.setTotalAmount(Math.round(total * 100.0) / 100.0);
            order.setStatus("CONFIRMED");
            orderHistory.add(0, order);

            // Record custom Prometheus counters
            sneakerOrdersCounter.increment();
            sneakerRevenueCounter.increment(order.getTotalAmount());

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
        summary.put("store", "CloudKicks Sneaker Retail");
        summary.put("status", "UP");
        summary.put("ordersPlacedTotal", (long) sneakerOrdersCounter.count());
        summary.put("revenueGeneratedTotal", sneakerRevenueCounter.count());
        summary.put("activeShoppers", activeDropShoppers.get());
        summary.put("catalogSneakersCount", productCatalog.size());
        summary.put("simulateLatencySpikeActive", simulateLatencySpike);
        summary.put("artificialLatencyMs", artificialLatencyMs);
        summary.put("simulateErrorsActive", simulateErrors);
        summary.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(summary);
    }

    // Reliability & Alert Simulation Endpoints for Professor Demo
    @PostMapping("/simulate/latency")
    public ResponseEntity<Map<String, Object>> toggleLatencySpike(@RequestParam(defaultValue = "650") int delayMs) {
        this.simulateLatencySpike = true;
        this.artificialLatencyMs = delayMs;
        activeDropShoppers.set(activeDropShoppers.get() + 500);
        return ResponseEntity.ok(Map.of(
                "message", "Sneaker Drop Bot Rush: Artificial Latency Spiked to " + delayMs + "ms (Will trigger Grafana SLA Alert)",
                "delayMs", delayMs,
                "simulateLatencySpike", true
        ));
    }

    @PostMapping("/simulate/error")
    public ResponseEntity<Map<String, Object>> toggleErrorSimulation(@RequestParam(defaultValue = "true") boolean enabled) {
        this.simulateErrors = enabled;
        return ResponseEntity.ok(Map.of(
                "message", enabled ? "Sneaker Checkout Drop Crash 5xx Active (Triggers Error Rate Alert)" : "Error simulation normalized",
                "simulateErrors", enabled
        ));
    }

    @PostMapping("/simulate/reset")
    public ResponseEntity<Map<String, Object>> resetSimulation() {
        this.simulateLatencySpike = false;
        this.artificialLatencyMs = 0;
        this.simulateErrors = false;
        this.activeDropShoppers.set(240);
        return ResponseEntity.ok(Map.of(
                "message", "Sneaker platform normalized. Zero delay, normal latency.",
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
