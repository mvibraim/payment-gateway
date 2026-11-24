package com.example.gateway;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
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

	/** Finds a payment only if it belongs to the merchant (BR-004). */
	Optional<Payment> find(String merchantId, UUID id) {
		return jdbc.sql("""
				SELECT id, status, amount, currency, last4, expiry_month, expiry_year
				FROM payments WHERE id = ? AND merchant_id = ?
				""")
				.params(id.toString(), merchantId)
				.query((rs, n) -> new Payment(UUID.fromString(rs.getString("id")), rs.getString("status"),
						rs.getLong("amount"), rs.getString("currency"),
						new Payment.Card(rs.getString("last4"), rs.getInt("expiry_month"), rs.getInt("expiry_year"))))
				.optional();
	}
}
