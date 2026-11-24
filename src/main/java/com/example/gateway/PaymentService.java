package com.example.gateway;

import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
class PaymentService {

	private final Bank bank;
	private final PaymentRepository payments;

	PaymentService(Bank bank, PaymentRepository payments) {
		this.bank = bank;
		this.payments = payments;
	}

	/** Calls the bank, then records the payment. The bank call is outside any transaction (ADR-0002). */
	Payment submit(String merchantId, PaymentRequest request) {
		UUID id = UUID.randomUUID();
		BankResult result = bank.charge(id, request.cardNumber(), request.amount(), request.currency());
		String number = request.cardNumber();
		Payment payment = new Payment(id, result.name().toLowerCase(), request.amount(), request.currency(),
				new Payment.Card(number.substring(number.length() - 4), request.expiryMonth(), request.expiryYear()));
		payments.insert(payment, merchantId);
		return payment;
	}
}
