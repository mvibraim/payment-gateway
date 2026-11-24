package com.example.gateway;

import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** SQLite datasource with a single connection, so writes are serialized (ADR-0002). */
@Configuration
class DatabaseConfig {

	@Bean(destroyMethod = "close")
	DataSource dataSource(@Value("${gateway.db.path}") String dbPath) {
		Path file = Path.of(dbPath).toAbsolutePath();
		try {
			Files.createDirectories(file.getParent());
		} catch (IOException e) {
			throw new UncheckedIOException("Cannot create database folder " + file.getParent(), e);
		}
		HikariDataSource dataSource = new HikariDataSource();
		dataSource.setDriverClassName("org.sqlite.JDBC");
		dataSource.setJdbcUrl("jdbc:sqlite:" + file);
		dataSource.setMaximumPoolSize(1);
		dataSource.setMinimumIdle(1);
		return dataSource;
	}
}
