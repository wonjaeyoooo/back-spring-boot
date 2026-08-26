package com.example.back.live;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/**
 * 라이브 signup 스모크 — 실제 Supabase GoTrue를 호출한다.
 * 사전 조건(T40): Supabase 대시보드 Authentication > Sign In/Up에서 Confirm email 비활성화.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnabledIfEnvironmentVariable(named = "SUPABASE_BASE_URL", matches = ".+")
@EnabledIfEnvironmentVariable(named = "SUPABASE_PUBLISHABLE_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "SUPABASE_JWKS_URI", matches = ".+")
@EnabledIfEnvironmentVariable(named = "SUPABASE_ISSUER", matches = ".+")
class LiveSignupSmokeTest extends LiveSupabaseSmokeSupport {

	@LocalServerPort
	private int port;

	private final RestTemplate rest = new RestTemplate();

	@Test
	@SuppressWarnings("unchecked")
	void liveSignupReturnsRealSession() {
		String email = uniqueEmail();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		ResponseEntity<Map<String, Object>> response = rest.exchange(
			base() + "/api/auth/signup",
			org.springframework.http.HttpMethod.POST,
			new HttpEntity<>(credentials(email, PASSWORD), headers),
			(Class<Map<String, Object>>) (Class<?>) Map.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().get("sessionCreated"))
			.as("Supabase 대시보드에서 Authentication > Sign In/Up의 Confirm email을 비활성화하면 세션이 즉시 발급됩니다")
			.isEqualTo(true);
		assertThat((String) response.getBody().get("accessToken")).isNotBlank();
	}

	private String base() {
		return "http://localhost:" + port;
	}
}
