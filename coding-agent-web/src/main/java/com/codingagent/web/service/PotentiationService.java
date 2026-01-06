package com.codingagent.web.service;

import org.springframework.stereotype.Service;

@Service
public class PotentiationService {

    public double calculatePotentiation(double base, double exponent) {
        return Math.pow(base, exponent);
    }
}