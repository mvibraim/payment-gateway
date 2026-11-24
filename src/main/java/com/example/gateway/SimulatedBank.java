package com.example.gateway;

import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * In-process simulated bank (ADR-0004). The card number's last digit picks the outcome: 1 is declined and
 * anything else is authorized. Requests are recorded by payment ID so a repeat returns the original result
 * without charging again.
 */
@Component
public class SimulatedBank implements Bank {

	private final JdbcClient jdbc;

	SimulatedBank(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public static BankResult outcomeFor(String cardNumber) {
		char lastDigit = cardNumber.charAt(cardNumber.length() - 1);
		return lastDigit == '1' ? BankResult.DECLINED : BankResult.AUTHORIZED;
	}

	@Override
	public BankResult charge(UUID paymentId, String cardNumber, long amount, String currency) {
		String id = paymentId.toString();
		String previous = jdbc.sql("SELECT result FROM bank_requests WHERE payment_id = ?").param(id)
				.query(String.class).optional().orElse(null);
		if (previous != null) {
			return BankResult.valueOf(previous);
		}
		BankResult result = outcomeFor(cardNumber);
		jdbc.sql("INSERT INTO bank_requests (payment_id, amount, currency, result, charged) VALUES (?, ?, ?, ?, ?)")
				.params(id, amount, currency, result.name().toLowerCase(), result == BankResult.AUTHORIZED ? 1 : 0)
				.update();
		return result;
	}
}
