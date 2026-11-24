package com.example.gateway;

import java.util.UUID;

/** A payment as returned to merchants: the card is limited to its last four digits and expiry (BR-005). */
record Payment(UUID id, String status, long amount, String currency, Card card) {

	record Card(String last4, int expiryMonth, int expiryYear) {
	}
}
