package com.example.gateway;

/**
 * Body of POST /payments (ADR-0003). Amount is a whole number in the currency's smallest unit. Fields are nullable
 * so that missing ones are reported by {@link PaymentValidator} together with the other failures (FR-004).
 */
public record PaymentRequest(String cardNumber, Integer expiryMonth, Integer expiryYear, String cvv, Long amount,
		String currency) {
}
