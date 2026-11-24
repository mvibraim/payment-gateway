package com.example.gateway;

import java.time.Instant;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class PaymentRepository {

	private final JdbcClient jdbc;

	PaymentRepository(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	void insert(Payment payment, String merchantId) {
		jdbc.sql("""
				INSERT INTO payments (id, merchant_id, last4, expiry_month, expiry_year, amount, currency, status, created_at)
				VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
				""")
				.params(payment.id().toString(), merchantId, payment.card().last4(), payment.card().expiryMonth(),
						payment.card().expiryYear(), payment.amount(), payment.currency(), payment.status(),
						Instant.now().toString())
				.update();
	}
}
