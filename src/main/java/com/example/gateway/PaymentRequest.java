package com.example.gateway;

/** Body of POST /payments (ADR-0003). Amount is a whole number in the currency's smallest unit. */
record PaymentRequest(String cardNumber, int expiryMonth, int expiryYear, String cvv, long amount, String currency) {
}
