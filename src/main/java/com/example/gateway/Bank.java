package com.example.gateway;

import java.util.UUID;

/** The gateway's view of the bank (ADR-0004). A repeated request with the same payment ID never charges twice. */
public interface Bank {

	BankResult charge(UUID paymentId, String cardNumber, long amount, String currency);
}
