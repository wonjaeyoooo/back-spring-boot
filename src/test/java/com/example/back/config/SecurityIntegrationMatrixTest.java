package com.example.back.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.example.back.support.JwksTestServer;

/**
 * 실제 필터체인 + 로컬 JWKS 서버로 전 보안 경로를 관통하는 통합 매트릭스.
 * 유효 ES256/RS256은 통과, 잘못된 서명/만료/잘못된 issuer/HS256/unsigned는 401.
 * 기존 공개 엔드포인트(ping, Swagger, Redis/Kafka)는 익명 접근 유지.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SecurityIntegrationMatrixTest {

	static JwksTestServer jwksServer;

	@DynamicPropertySource
	static void supabaseProps(DynamicPropertyRegistry registry) throws Exception {
		jwksServer = JwksTestServer.shared();
		registry.add("supabase.jwks-uri", jwksServer::jwksUri);
		registry.add("supabase.issuer", () -> JwksTestServer.ISSUER);
	}

	@Autowired
	private MockMvc mockMvc;

	private String bearer(String token) {
		return "Bearer " + token;
	}

	@Test
	void anonymousMeIs401() throws Exception {
		mockMvc.perform(get("/api/me"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void validEs256TokenReachesMeAndReturns200() throws Exception {
		String subject = UUID.randomUUID().toString();

		mockMvc.perform(get("/api/me")
				.header(HttpHeaders.AUTHORIZATION,
					bearer(jwksServer.es256Token(subject, "matrix-es@example.com",
						Instant.now(), Instant.now().plus(1, ChronoUnit.HOURS)))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.email").value("matrix-es@example.com"));
	}

	@Test
	void validRs256TokenReachesMeAndReturns200() throws Exception {
		String subject = UUID.randomUUID().toString();

		mockMvc.perform(get("/api/me")
				.header(HttpHeaders.AUTHORIZATION,
					bearer(jwksServer.rs256Token(subject, "matrix-rs@example.com",
						Instant.now(), Instant.now().plus(1, ChronoUnit.HOURS)))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.email").value("matrix-rs@example.com"));
	}

	@Test
	void invalidSignatureIs401() throws Exception {
		String token = jwksServer.es256TokenSignedByForeignIssuer(
			UUID.randomUUID().toString(), Instant.now(), Instant.now().plus(1, ChronoUnit.HOURS));

		mockMvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void expiredTokenIs401() throws Exception {
		String token = jwksServer.es256Token(UUID.randomUUID().toString(), "expired@example.com",
			Instant.now().minus(2, ChronoUnit.HOURS), Instant.now().minus(1, ChronoUnit.HOURS));

		mockMvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void wrongIssuerIs401() throws Exception {
		String token = jwksServer.es256TokenWithIssuer(UUID.randomUUID().toString(), "evil@example.com",
			Instant.now(), Instant.now().plus(1, ChronoUnit.HOURS), "https://evil.example.com/auth/v1");

		mockMvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void hs256TokenIs401() throws Exception {
		String token = jwksServer.hs256Token("symmetric-attacker-secret-that-is-long-enough-32bytes!", UUID.randomUUID().toString(),
			Instant.now(), Instant.now().plus(1, ChronoUnit.HOURS));

		mockMvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void unsignedNoneAlgorithmTokenIs401() throws Exception {
		String token = jwksServer.unsignedToken(UUID.randomUUID().toString(),
			Instant.now(), Instant.now().plus(1, ChronoUnit.HOURS));

		mockMvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void anonymousPingIs200() throws Exception {
		mockMvc.perform(get("/api/ping"))
			.andExpect(status().isOk());
	}

	@Test
	void swaggerDocsAnonymousIs200() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk());
	}

	@Test
	void redisEndpointStaysPubliclyReachableNot401() throws Exception {
		int status = mockMvc.perform(get("/api/redis/matrix-key"))
			.andReturn()
			.getResponse()
			.getStatus();
		org.assertj.core.api.Assertions.assertThat(status).isNotEqualTo(401);
	}
}
