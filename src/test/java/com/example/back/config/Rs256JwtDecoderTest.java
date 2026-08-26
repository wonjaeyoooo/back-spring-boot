package com.example.back.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.example.back.support.JwksTestServer;

/**
 * 동일한 JWKS 서버에서 RSA 키로 서명된 RS256 토큰도 수용함을 증명한다.
 */
@SpringBootTest
class Rs256JwtDecoderTest {

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
	void decodesValidRs256TokenWithClaims() throws Exception {
		UUID subject = UUID.randomUUID();
		String token = jwksServer.rs256Token(subject.toString(), "rs-user@example.com",
			Instant.now(), Instant.now().plus(1, ChronoUnit.HOURS));

		Jwt jwt = jwtDecoder.decode(token);

		assertThat(jwt.getSubject()).isEqualTo(subject.toString());
		assertThat(jwt.getClaimAsString("email")).isEqualTo("rs-user@example.com");
	}
}
