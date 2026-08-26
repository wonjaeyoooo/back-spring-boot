package com.example.back.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.example.back.support.JwksTestServer;

/**
 * 무효 JWT 거부 매트릭스: 잘못된 서명 / 만료 / 잘못된 issuer / HS256 / unsigned(none).
 * 다섯 케이스 전부 디코더에서 인증 실패(BadJwtException 계열)로 거부되어야 한다.
 */
@SpringBootTest
class InvalidJwtRejectionTest {

	static JwksTestServer jwksServer;

	@DynamicPropertySource
	static void supabaseProps(DynamicPropertyRegistry registry) throws Exception {
		jwksServer = JwksTestServer.shared();
		registry.add("supabase.jwks-uri", jwksServer::jwksUri);
		registry.add("supabase.issuer", () -> JwksTestServer.ISSUER);
	}

	@Autowired
	private JwtDecoder jwtDecoder;

	private Instant now() {
		return Instant.now();
	}

	@Test
	void rejectsTokenSignedWithKeyNotInJwks() throws Exception {
		String token = jwksServer.es256TokenSignedByForeignIssuer(UUID.randomUUID().toString(),
			now(), now().plus(1, ChronoUnit.HOURS));

		assertThatThrownBy(() -> jwtDecoder.decode(token))
			.isInstanceOf(BadJwtException.class);
	}

	@Test
	void rejectsExpiredToken() throws Exception {
		String token = jwksServer.es256Token(UUID.randomUUID().toString(), "expired@example.com",
			now().minus(2, ChronoUnit.HOURS), now().minus(1, ChronoUnit.HOURS));

		assertThatThrownBy(() -> jwtDecoder.decode(token))
			.isInstanceOf(BadJwtException.class);
	}

	@Test
	void rejectsWrongIssuer() throws Exception {
		String token = jwksServer.es256TokenWithIssuer(UUID.randomUUID().toString(), "wrong-iss@example.com",
			now(), now().plus(1, ChronoUnit.HOURS), "https://evil.example.com/auth/v1");

		assertThatThrownBy(() -> jwtDecoder.decode(token))
			.isInstanceOf(BadJwtException.class);
	}

	@Test
	void rejectsHs256Token() throws Exception {
		String token = jwksServer.hs256Token("attacker-controlled-secret-value", UUID.randomUUID().toString(),
			now(), now().plus(1, ChronoUnit.HOURS));

		assertThatThrownBy(() -> jwtDecoder.decode(token))
			.isInstanceOf(BadJwtException.class);
	}

	@Test
	void rejectsUnsignedNoneAlgorithmToken() throws Exception {
		String token = jwksServer.unsignedToken(UUID.randomUUID().toString(), now(), now().plus(1, ChronoUnit.HOURS));

		assertThatThrownBy(() -> jwtDecoder.decode(token))
			.isInstanceOf(BadJwtException.class);
	}
}
