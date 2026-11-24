package com.example.gateway.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RetrievePaymentIT {

	private static final String CARD_AUTHORIZED = "4242424242424240";
	private static final String CARD_DECLINED = "4242424242424241";

	@TempDir
	static Path tempDir;

	@DynamicPropertySource
	static void properties(DynamicPropertyRegistry registry) {
		registry.add("gateway.db.path", () -> tempDir.resolve("retrieve.db").toString());
	}

	@Autowired
	MockMvc mvc;

	private MvcResult submit(String merchant, String cardNumber) throws Exception {
		String body = """
				{"cardNumber":"%s","expiryMonth":12,"expiryYear":2030,"cvv":"123","amount":5000,"currency":"USD"}
				""".formatted(cardNumber);
		return mvc.perform(post("/payments").header("X-Merchant-Id", merchant)
				.contentType(MediaType.APPLICATION_JSON).content(body)).andReturn();
	}

	private String submitAndGetId(String merchant, String cardNumber) throws Exception {
		return JsonPath.read(submit(merchant, cardNumber).getResponse().getContentAsString(), "$.id");
	}

	@Test
	void authorizedPaymentCanBeRetrieved() throws Exception {
		String id = submitAndGetId("m-1", CARD_AUTHORIZED);
		MvcResult result = mvc.perform(get("/payments/" + id).header("X-Merchant-Id", "m-1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id))
				.andExpect(jsonPath("$.status").value("authorized"))
				.andExpect(jsonPath("$.amount").value(5000))
				.andExpect(jsonPath("$.currency").value("USD"))
				.andExpect(jsonPath("$.card.last4").value("4240"))
				.andExpect(jsonPath("$.card.expiryMonth").value(12))
				.andExpect(jsonPath("$.card.expiryYear").value(2030))
				.andReturn();
		assertThat(result.getResponse().getContentAsString()).doesNotContain(CARD_AUTHORIZED)
				.doesNotContain("\"cvv\"").doesNotContain("123\"");
	}

	@Test
	void declinedPaymentStaysDeclinedWhenRetrieved() throws Exception {
		String id = submitAndGetId("m-1", CARD_DECLINED);
		mvc.perform(get("/payments/" + id).header("X-Merchant-Id", "m-1")).andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("declined"));
		mvc.perform(get("/payments/" + id).header("X-Merchant-Id", "m-1")).andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("declined"));
	}

	@Test
	void submitAndRetrieveResponsesNeverExposeFullCardNumberOrCvv() throws Exception {
		MvcResult submitted = submit("m-3", "5555555555554444");
		String id = JsonPath.read(submitted.getResponse().getContentAsString(), "$.id");
		String retrieved = mvc.perform(get("/payments/" + id).header("X-Merchant-Id", "m-3")).andReturn()
				.getResponse().getContentAsString();
		for (String content : new String[] { submitted.getResponse().getContentAsString(), retrieved }) {
			assertThat(content).doesNotContain("5555555555554444").doesNotContain("cvv").doesNotContain("cardNumber");
			assertThat(JsonPath.<java.util.Map<String, Object>>read(content, "$.card").keySet())
					.containsExactlyInAnyOrder("last4", "expiryMonth", "expiryYear");
		}
	}

	@Test
	void anotherMerchantsPaymentIsReportedNotFound() throws Exception {
		String id = submitAndGetId("m-1", CARD_AUTHORIZED);
		mvc.perform(get("/payments/" + id).header("X-Merchant-Id", "m-2")).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("not_found"));
	}

	@Test
	void unknownIdIsNotFound() throws Exception {
		mvc.perform(get("/payments/" + UUID.randomUUID()).header("X-Merchant-Id", "m-1"))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("not_found"));
	}

	@Test
	void malformedIdIsNotFound() throws Exception {
		mvc.perform(get("/payments/not-a-uuid").header("X-Merchant-Id", "m-1")).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("not_found"));
	}

	@Test
	void missingMerchantHeaderIsRejectedWith400() throws Exception {
		String id = submitAndGetId("m-1", CARD_AUTHORIZED);
		mvc.perform(get("/payments/" + id)).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("missing_merchant_id"));
		mvc.perform(get("/payments/" + id).header("X-Merchant-Id", " ")).andExpect(status().isBadRequest());
	}
}
