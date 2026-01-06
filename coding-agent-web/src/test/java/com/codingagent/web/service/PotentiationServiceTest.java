package com.codingagent.web.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PotentiationServiceTest {

    private final PotentiationService potentiationService = new PotentiationService();

    @Test
    void testCalculatePotentiation() {
        // Test basic potentiation
        assertEquals(8.0, potentiationService.calculatePotentiation(2.0, 3.0), 0.0001);
        
        // Test with exponent 0
        assertEquals(1.0, potentiationService.calculatePotentiation(5.0, 0.0), 0.0001);
        
        // Test with negative exponent
        assertEquals(0.25, potentiationService.calculatePotentiation(2.0, -2.0), 0.0001);
        
        // Test with fractional exponent
        assertEquals(4.0, potentiationService.calculatePotentiation(16.0, 0.5), 0.0001);
    }
}