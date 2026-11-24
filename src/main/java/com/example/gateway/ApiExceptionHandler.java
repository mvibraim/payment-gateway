package com.example.gateway;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** The single place that builds error bodies (ADR-0003). */
@RestControllerAdvice
class ApiExceptionHandler {

	@ExceptionHandler(MissingMerchantException.class)
	ResponseEntity<Map<String, String>> missingMerchant() {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "missing_merchant_id"));
	}

	@ExceptionHandler(PaymentNotFoundException.class)
	ResponseEntity<Map<String, String>> notFound() {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "not_found"));
	}

	@ExceptionHandler(ValidationException.class)
	ResponseEntity<Map<String, List<FieldError>>> invalid(ValidationException e) {
		return ResponseEntity.unprocessableContent().body(Map.of("errors", e.errors()));
	}

	/** Malformed JSON or a value of the wrong type is a validation failure too (FR-004). */
	@ExceptionHandler(HttpMessageNotReadableException.class)
	ResponseEntity<Map<String, List<FieldError>>> unreadable() {
		return ResponseEntity.unprocessableContent()
				.body(Map.of("errors", List.of(new FieldError("body", "must be valid JSON with correctly typed fields"))));
	}
}
