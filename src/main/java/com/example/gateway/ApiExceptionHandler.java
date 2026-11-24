package com.example.gateway;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** The single place that builds error bodies (ADR-0003). */
@RestControllerAdvice
class ApiExceptionHandler {

	@ExceptionHandler(MissingMerchantException.class)
	ResponseEntity<Map<String, String>> missingMerchant() {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "missing_merchant_id"));
	}
}
