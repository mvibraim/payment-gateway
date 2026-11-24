package com.example.gateway.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.gateway.BankResult;
import com.example.gateway.SimulatedBank;
import org.junit.jupiter.api.Test;

class SimulatedBankOutcomeTest {

	@Test
	void lastDigitOneIsDeclined() {
		assertEquals(BankResult.DECLINED, SimulatedBank.outcomeFor("4111111111111111"));
	}

	@Test
	void anyOtherLastDigitIsAuthorized() {
		for (char digit : "0234789".toCharArray()) {
			assertEquals(BankResult.AUTHORIZED, SimulatedBank.outcomeFor("411111111111111" + digit));
		}
	}
}
