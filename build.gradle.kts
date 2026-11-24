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
}

testing {
	suites {
		val test by getting(JvmTestSuite::class)

		register<JvmTestSuite>("integrationTest") {
			dependencies {
				implementation(project())
				implementation("org.springframework.boot:spring-boot-starter-jdbc")
				implementation("org.springframework.boot:spring-boot-starter-webmvc")
				implementation("org.springframework.boot:spring-boot-starter-webmvc-test")
				implementation("org.springframework.boot:spring-boot-starter-jdbc-test")
				implementation("org.awaitility:awaitility")
				runtimeOnly("org.junit.platform:junit-platform-launcher")
			}
			targets.all {
				testTask.configure { shouldRunAfter(test) }
			}
		}
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.named("check") {
	dependsOn(testing.suites.named("integrationTest"))
}

tasks.withType<JavaCompile> {
	options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
}
