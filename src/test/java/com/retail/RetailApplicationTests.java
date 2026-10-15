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
    void testShoeCatalogInitialization() {
        ResponseEntity<List<Product>> response = retailController.getAllProducts();
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().size() >= 5, "Shoe catalog should contain seeded sneakers");
    }

    @Test
    void testFindShoeById() {
        ResponseEntity<Product> response = retailController.getProductById("SHOE-001");
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getName().contains("Air Jordan"));
        assertNotNull(response.getBody().getAvailableSizes());
    }
}
