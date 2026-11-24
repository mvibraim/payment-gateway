package com.example.gateway;

class MissingMerchantException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	MissingMerchantException() {
		super("X-Merchant-Id header is required");
	}
}
