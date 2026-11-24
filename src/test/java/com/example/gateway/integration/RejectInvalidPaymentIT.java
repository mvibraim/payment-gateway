package com.example.gateway.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@Import(RejectInvalidPaymentIT.FixedClock.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RejectInvalidPaymentIT {

	@TestConfiguration
	static class FixedClock {
		@Bean
		@Primary
		Clock fixedClock() {
			return Clock.fixed(Instant.parse("2026-11-23T12:00:00Z"), ZoneOffset.UTC);
		}
	}

	@TempDir
	static Path tempDir;

	@DynamicPropertySource
	static void properties(DynamicPropertyRegistry registry) {
		registry.add("gateway.db.path", () -> tempDir.resolve("invalid.db").toString());
	}

	@Autowired
	MockMvc mvc;

	@Autowired
	JdbcClient jdbc;

	private ResultActions submit(String json) throws Exception {
		return mvc.perform(post("/payments").header("X-Merchant-Id", "m-1").contentType(MediaType.APPLICATION_JSON)
				.content(json));
	}

	private static String body(String card, String month, String year, String cvv, String amount, String currency) {
		return """
				{"cardNumber":%s,"expiryMonth":%s,"expiryYear":%s,"cvv":%s,"amount":%s,"currency":%s}
				""".formatted(card, month, year, cvv, amount, currency);
	}

	@BeforeEach
	void clearPayments() {
		jdbc.sql("DELETE FROM payments").update();
	}

	private int recorded() {
		return jdbc.sql("SELECT COUNT(*) FROM payments").query(Integer.class).single();
	}

	@Test
	void pastExpiryAndUnsupportedCurrencyAreBothReportedAndNothingIsRecorded() throws Exception {
		submit(body("\"4242424242424240\"", "1", "2020", "\"123\"", "5000", "\"JPY\""))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.errors.length()").value(2))
				.andExpect(jsonPath("$.errors[?(@.field=='expiry')]").exists())
				.andExpect(jsonPath("$.errors[?(@.field=='currency')]").exists());
		assertThat(recorded()).isZero();
	}

	@Test
	void missingFieldsAreReported() throws Exception {
		submit("{}").andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.errors.length()").value(6));
		assertThat(recorded()).isZero();
	}

	@Test
	void malformedJsonIsReportedAsValidationFailure() throws Exception {
		submit("{not json").andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.errors[0].field").value("body"))
				.andExpect(jsonPath("$.errors[0].message").isString());
		assertThat(recorded()).isZero();
	}

	@Test
	void wrongTypeIsReportedAsValidationFailure() throws Exception {
		submit(body("\"4242424242424240\"", "12", "2030", "\"123\"", "\"lots\"", "\"USD\""))
				.andExpect(status().isUnprocessableContent());
		assertThat(recorded()).isZero();
	}

	@Test
	void fractionalAmountIsRejected() throws Exception {
		submit(body("\"4242424242424240\"", "12", "2030", "\"123\"", "10.5", "\"USD\""))
				.andExpect(status().isUnprocessableContent());
		assertThat(recorded()).isZero();
	}

	@Test
	void invalidCardNumberAndCvvAreBothRejected() throws Exception {
		submit(body("\"4242-4242\"", "12", "2030", "\"12\"", "5000", "\"USD\""))
				.andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.errors.length()").value(2));
		assertThat(recorded()).isZero();
	}

	@Test
	void cardExpiringThisMonthIsAcceptedAndOneExpiredLastMonthIsRejected() throws Exception {
		submit(body("\"4242424242424240\"", "11", "2026", "\"123\"", "5000", "\"USD\""))
				.andExpect(status().isCreated());
		submit(body("\"4242424242424240\"", "10", "2026", "\"123\"", "5000", "\"USD\""))
				.andExpect(status().isUnprocessableContent());
		assertThat(recorded()).isEqualTo(1);
	}
}
