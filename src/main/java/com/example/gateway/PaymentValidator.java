package com.example.gateway;

import java.time.Clock;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Validation rules of BR-008 and BR-012. Reports every failure, not just the first. */
@Component
public class PaymentValidator {

	private static final Set<String> CURRENCIES = Set.of("USD", "EUR", "GBP");

	private final Clock clock;

	public PaymentValidator(Clock clock) {
		this.clock = clock;
	}

	public List<FieldError> validate(PaymentRequest request) {
		List<FieldError> errors = new ArrayList<>();
		validateCardNumber(request.cardNumber(), errors);
		validateExpiry(request.expiryMonth(), request.expiryYear(), errors);
		validateCvv(request.cvv(), errors);
		validateAmount(request.amount(), errors);
		validateCurrency(request.currency(), errors);
		return errors;
	}

	private static void validateCardNumber(String cardNumber, List<FieldError> errors) {
		if (cardNumber == null) {
			errors.add(new FieldError("cardNumber", "is required"));
		} else if (!cardNumber.matches("[0-9]{14,19}")) {
			errors.add(new FieldError("cardNumber", "must be 14 to 19 digits"));
		}
	}

	private void validateExpiry(Integer month, Integer year, List<FieldError> errors) {
		boolean valid = true;
		if (month == null) {
			errors.add(new FieldError("expiryMonth", "is required"));
			valid = false;
		} else if (month < 1 || month > 12) {
			errors.add(new FieldError("expiryMonth", "must be between 1 and 12"));
			valid = false;
		}
		if (year == null) {
			errors.add(new FieldError("expiryYear", "is required"));
			valid = false;
		} else if (year < 1 || year > 9999) {
			errors.add(new FieldError("expiryYear", "must be a four-digit year"));
			valid = false;
		}
		// A card is valid through the last day of its expiry month, judged in UTC (BR-012).
		if (valid && YearMonth.of(year, month).isBefore(YearMonth.now(clock))) {
			errors.add(new FieldError("expiry", "must not be in the past"));
		}
	}

	private static void validateCvv(String cvv, List<FieldError> errors) {
		if (cvv == null) {
			errors.add(new FieldError("cvv", "is required"));
		} else if (!cvv.matches("[0-9]{3,4}")) {
			errors.add(new FieldError("cvv", "must be 3 or 4 digits"));
		}
	}

	private static void validateAmount(Long amount, List<FieldError> errors) {
		if (amount == null) {
			errors.add(new FieldError("amount", "is required"));
		} else if (amount <= 0) {
			errors.add(new FieldError("amount", "must be a positive whole number"));
		}
	}

	private static void validateCurrency(String currency, List<FieldError> errors) {
		if (currency == null) {
			errors.add(new FieldError("currency", "is required"));
		} else if (!CURRENCIES.contains(currency)) {
			errors.add(new FieldError("currency", "must be one of USD, EUR, GBP"));
		}
	}
}
