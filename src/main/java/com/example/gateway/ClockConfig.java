package com.example.gateway;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ClockConfig {

	/** UTC clock that decides today's date for card expiry (BR-012). Tests replace it. */
	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}
}
