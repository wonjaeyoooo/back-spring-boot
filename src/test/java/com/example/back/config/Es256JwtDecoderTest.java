package com.example.back.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.example.back.support.JwksTestServer;

/**
 * 로컬 JWKS 서버로 프로덕션 JwtDecoder 빈의 ES256 검증 경로를 증명한다.
 * Nimbus 기본값은 RS256뿐이라 ES256 명시가 빠지면 이 테스트가 실패한다.
 */
@SpringBootTest
class Es256JwtDecoderTest {

	static JwksTestServer jwksServer;

	@DynamicPropertySource
	static void supabaseProps(DynamicPropertyRegistry registry) throws Exception {
		jwksServer = JwksTestServer.shared();
		registry.add("supabase.jwks-uri", jwksServer::jwksUri);
		registry.add("supabase.issuer", () -> JwksTestServer.ISSUER);
	}

	@Autowired
	private JwtDecoder jwtDecoder;

	@Test
	void decodesValidEs256TokenWithClaims() throws Exception {
		UUID subject = UUID.randomUUID();
		String token = jwksServer.es256Token(subject.toString(), "es-user@example.com",
			Instant.now(), Instant.now().plus(1, ChronoUnit.HOURS));

		Jwt jwt = jwtDecoder.decode(token);

		assertThat(jwt.getSubject()).isEqualTo(subject.toString());
		assertThat(jwt.getClaimAsString("email")).isEqualTo("es-user@example.com");
		assertThat(jwt.getIssuer().toString()).isEqualTo(JwksTestServer.ISSUER);
		assertThat(jwt.getAudience()).containsExactly("authenticated");
	}
}
