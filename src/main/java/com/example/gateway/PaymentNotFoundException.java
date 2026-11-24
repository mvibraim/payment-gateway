package com.example.gateway;

class PaymentNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	PaymentNotFoundException() {
		super("Payment not found");
	}
}
