package com.example.gateway;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
class PaymentController {

	private final PaymentService service;
	private final PaymentValidator validator;

	PaymentController(PaymentService service, PaymentValidator validator) {
		this.service = service;
		this.validator = validator;
	}

	@PostMapping("/payments")
	ResponseEntity<Payment> submit(@RequestHeader(value = "X-Merchant-Id", required = false) String merchantId,
			@RequestBody PaymentRequest request) {
		requireMerchant(merchantId);
		List<FieldError> errors = validator.validate(request);
		if (!errors.isEmpty()) {
			throw new ValidationException(errors);
		}
		return ResponseEntity.status(HttpStatus.CREATED).body(service.submit(merchantId, request));
	}

	@GetMapping("/payments/{id}")
	Payment retrieve(@RequestHeader(value = "X-Merchant-Id", required = false) String merchantId,
			@PathVariable String id) {
		requireMerchant(merchantId);
		return parseId(id).flatMap(uuid -> service.find(merchantId, uuid)).orElseThrow(PaymentNotFoundException::new);
	}

	private static void requireMerchant(String merchantId) {
		if (merchantId == null || merchantId.isBlank()) {
			throw new MissingMerchantException();
		}
	}

	private static Optional<UUID> parseId(String id) {
		try {
			return Optional.of(UUID.fromString(id));
		} catch (IllegalArgumentException e) {
			return Optional.empty();
		}
	}
}
