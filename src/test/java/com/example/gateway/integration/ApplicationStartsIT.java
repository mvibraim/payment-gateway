package com.example.gateway.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ApplicationStartsIT {

	@TempDir
	static Path tempDir;

	@DynamicPropertySource
	static void properties(DynamicPropertyRegistry registry) {
		registry.add("gateway.db.path", () -> tempDir.resolve("nested/test.db").toString());
	}

	@Autowired
	JdbcClient jdbc;

	@Test
	void contextStartsAndCreatesTheDatabaseFile() {
		Integer one = jdbc.sql("SELECT 1").query(Integer.class).single();
		assertThat(one).isEqualTo(1);
		assertThat(Files.exists(tempDir.resolve("nested/test.db"))).isTrue();
	}
}
