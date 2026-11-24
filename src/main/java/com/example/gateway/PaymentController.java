package com.example.gateway;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
class PaymentController {

	private final PaymentService service;

	PaymentController(PaymentService service) {
		this.service = service;
	}

	@PostMapping("/payments")
	ResponseEntity<Payment> submit(@RequestHeader(value = "X-Merchant-Id", required = false) String merchantId,
			@RequestBody PaymentRequest request) {
		if (merchantId == null || merchantId.isBlank()) {
			throw new MissingMerchantException();
		}
		return ResponseEntity.status(HttpStatus.CREATED).body(service.submit(merchantId, request));
	}
}
