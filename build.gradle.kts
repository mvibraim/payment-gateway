plugins {
	java
	id("org.springframework.boot") version "4.2.0"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-jdbc")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.xerial:sqlite-jdbc:3.54.1.0")
	testImplementation("org.junit.jupiter:junit-jupiter")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.springframework.boot:spring-boot-starter-jdbc-test")
	testImplementation("org.awaitility:awaitility")
}

// Unit tests live in the `unit` package and run without Spring; integration tests live in the `integration` package under
// src/test/java and run by `integrationTest`.
tasks.test {
	useJUnitPlatform()
	include("**/unit/**")
}

val integrationTest = tasks.register<Test>("integrationTest") {
	description = "Runs the integration tests (the integration package)."
	group = "verification"
	useJUnitPlatform()
	testClassesDirs = sourceSets.test.get().output.classesDirs
	classpath = sourceSets.test.get().runtimeClasspath
	include("**/integration/**")
	shouldRunAfter(tasks.test)
}

tasks.named("check") {
	dependsOn(integrationTest)
}

tasks.withType<JavaCompile> {
	options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
}
