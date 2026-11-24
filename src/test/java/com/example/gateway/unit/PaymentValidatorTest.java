package com.example.gateway.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.gateway.FieldError;
import com.example.gateway.PaymentRequest;
import com.example.gateway.PaymentValidator;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class PaymentValidatorTest {

	// 23:59:59 UTC on 2026-11-23: November is still the current month (BR-012).
	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-23T23:59:59Z"), ZoneOffset.UTC);
	private static final PaymentValidator VALIDATOR = new PaymentValidator(CLOCK);

	private static List<String> failedFields(String card, Integer month, Integer year, String cvv, Long amount,
			String currency) {
		return VALIDATOR.validate(new PaymentRequest(card, month, year, cvv, amount, currency)).stream()
				.map(FieldError::field).toList();
	}

	@Test
	void validRequestHasNoErrors() {
		assertThat(failedFields("4242424242424240", 12, 2030, "123", 5000L, "USD")).isEmpty();
	}

	@Test
	void expiryMonthOfTodayIsAccepted() {
		assertThat(failedFields("4242424242424240", 11, 2026, "123", 5000L, "USD")).isEmpty();
	}

	@Test
	void expiryMonthBeforeTodayIsRejected() {
		assertThat(failedFields("4242424242424240", 10, 2026, "123", 5000L, "USD")).containsExactly("expiry");
	}

	@Test
	void everyFailureIsReportedTogether() {
		assertThat(failedFields("12ab", 13, 2030, "12", 0L, "JPY"))
				.containsExactly("cardNumber", "expiryMonth", "cvv", "amount", "currency");
	}

	@Test
	void missingFieldsAreReported() {
		assertThat(failedFields(null, null, null, null, null, null))
				.containsExactly("cardNumber", "expiryMonth", "expiryYear", "cvv", "amount", "currency");
	}

	@Test
	void cardNumberLengthBounds() {
		assertThat(failedFields("1".repeat(14), 12, 2030, "123", 5000L, "USD")).isEmpty();
		assertThat(failedFields("1".repeat(19), 12, 2030, "123", 5000L, "USD")).isEmpty();
		assertThat(failedFields("1".repeat(13), 12, 2030, "123", 5000L, "USD")).containsExactly("cardNumber");
		assertThat(failedFields("1".repeat(20), 12, 2030, "123", 5000L, "USD")).containsExactly("cardNumber");
	}

	@Test
	void cvvMayHaveFourDigits() {
		assertThat(failedFields("4242424242424240", 12, 2030, "1234", 5000L, "EUR")).isEmpty();
	}

	@Test
	void negativeAmountAndLowercaseCurrencyAreRejected() {
		assertThat(failedFields("4242424242424240", 12, 2030, "123", -1L, "usd"))
				.containsExactly("amount", "currency");
	}
}
