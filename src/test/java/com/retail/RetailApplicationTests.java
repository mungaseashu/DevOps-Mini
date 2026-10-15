package com.retail;

import com.retail.controller.RetailController;
import com.retail.model.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class RetailApplicationTests {

    @Autowired
    private RetailController retailController;

    @Test
    void contextLoads() {
        assertNotNull(retailController, "RetailController should load in Spring Application Context");
    }

    @Test
    void testCatalogInitialization() {
        ResponseEntity<List<Product>> response = retailController.getAllProducts();
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().size() >= 5, "Product catalog should contain seeded products");
    }

    @Test
    void testFindProductById() {
        ResponseEntity<Product> response = retailController.getProductById("PROD-001");
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("MacBook Pro M3 Max", response.getBody().getName());
    }
}
