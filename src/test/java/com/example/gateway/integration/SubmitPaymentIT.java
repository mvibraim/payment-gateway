package com.example.gateway.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SubmitPaymentIT {

	@TempDir
	static Path tempDir;

	@DynamicPropertySource
	static void properties(DynamicPropertyRegistry registry) {
		registry.add("gateway.db.path", () -> tempDir.resolve("submit.db").toString());
	}

	@Autowired
	MockMvc mvc;

	@Autowired
	JdbcClient jdbc;

	private static String body(String cardNumber) {
		return """
				{"cardNumber":"%s","expiryMonth":12,"expiryYear":2030,"cvv":"123","amount":5000,"currency":"USD"}
				""".formatted(cardNumber);
	}

	private MvcResult submit(String merchant, String cardNumber) throws Exception {
		return mvc.perform(post("/payments").header("X-Merchant-Id", merchant)
				.contentType(MediaType.APPLICATION_JSON).content(body(cardNumber))).andReturn();
	}

	@Test
	void validPaymentIsAuthorizedAndChargedOnce() throws Exception {
		MvcResult result = mvc.perform(post("/payments").header("X-Merchant-Id", "m-1")
				.contentType(MediaType.APPLICATION_JSON).content(body("4242424242424240")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("authorized"))
				.andExpect(jsonPath("$.amount").value(5000))
				.andExpect(jsonPath("$.currency").value("USD"))
				.andExpect(jsonPath("$.card.last4").value("4240"))
				.andExpect(jsonPath("$.card.expiryMonth").value(12))
				.andExpect(jsonPath("$.card.expiryYear").value(2030))
				.andReturn();
		String content = result.getResponse().getContentAsString();
		String paymentId = JsonPath.read(content, "$.id");
		UUID.fromString(paymentId);

		assertThat(content).doesNotContain("4242424242424240").doesNotContain("\"cvv\"");
		Map<String, Object> bank = jdbc.sql("SELECT result, charged FROM bank_requests WHERE payment_id = ?")
				.param(paymentId).query().singleRow();
		assertThat(bank.get("result")).isEqualTo("authorized");
		assertThat(((Number) bank.get("charged")).intValue()).isEqualTo(1);
	}

	@Test
	void declinedPaymentIsReportedDeclinedAndNotCharged() throws Exception {
		MvcResult result = mvc.perform(post("/payments").header("X-Merchant-Id", "m-1")
				.contentType(MediaType.APPLICATION_JSON).content(body("4242424242424241")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("declined"))
				.andReturn();
		String paymentId = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
		Map<String, Object> bank = jdbc.sql("SELECT result, charged FROM bank_requests WHERE payment_id = ?")
				.param(paymentId).query().singleRow();
		assertThat(bank.get("result")).isEqualTo("declined");
		assertThat(((Number) bank.get("charged")).intValue()).isZero();
	}

	@Test
	void storedPaymentHoldsNoFullCardNumberOrCvv() throws Exception {
		MvcResult result = submit("m-2", "5555555555554444");
		String paymentId = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
		Map<String, Object> row = jdbc.sql("SELECT * FROM payments WHERE id = ?").param(paymentId).query().singleRow();
		assertThat(row.get("merchant_id")).isEqualTo("m-2");
		assertThat(row.get("last4")).isEqualTo("4444");
		assertThat(row.values().stream().map(String::valueOf).toList())
				.noneMatch(v -> v.contains("5555555555554444") || v.equals("123"));
		List<String> columns = jdbc.sql("SELECT name FROM pragma_table_info('payments')").query(String.class).list();
		assertThat(columns).doesNotContain("card_number", "cvv");
	}

	@Test
	void missingMerchantHeaderIsRejectedWith400() throws Exception {
		mvc.perform(post("/payments").contentType(MediaType.APPLICATION_JSON).content(body("4242424242424240")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("missing_merchant_id"));
	}

	@Test
	void blankMerchantHeaderIsRejectedWith400() throws Exception {
		mvc.perform(post("/payments").header("X-Merchant-Id", "  ").contentType(MediaType.APPLICATION_JSON)
				.content(body("4242424242424240"))).andExpect(status().isBadRequest());
	}

	@Test
	void rejectedRequestIsNotSentToTheBankOrRecorded() throws Exception {
		int before = jdbc.sql("SELECT count(*) FROM bank_requests").query(Integer.class).single();
		mvc.perform(post("/payments").contentType(MediaType.APPLICATION_JSON).content(body("4242424242424240")))
				.andExpect(status().isBadRequest());
		int after = jdbc.sql("SELECT count(*) FROM bank_requests").query(Integer.class).single();
		assertThat(after).isEqualTo(before);
	}
}
