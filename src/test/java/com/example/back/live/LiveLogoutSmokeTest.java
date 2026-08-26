package com.example.back.live;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

/**
 * 라이브 logout 스모크.
 * 정책 구분 단언: (1) logout 호출 자체는 성공, (2) stateless JWT 특성상 기발급 access token은
 * 만료 전까지 여전히 유효 — logout이 즉시 무효화하지 않는다는 것을 명시적으로 확인한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnabledIfEnvironmentVariable(named = "SUPABASE_BASE_URL", matches = ".+")
@EnabledIfEnvironmentVariable(named = "SUPABASE_PUBLISHABLE_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "SUPABASE_JWKS_URI", matches = ".+")
@EnabledIfEnvironmentVariable(named = "SUPABASE_ISSUER", matches = ".+")
class LiveLogoutSmokeTest extends LiveSupabaseSmokeSupport {

	@LocalServerPort
	private int port;

	private final RestTemplate rest = new RestTemplate();

	@Test
	@SuppressWarnings("unchecked")
	void liveLogoutSucceedsAndOldTokenRemainsValidUntilExpiry() {
		String email = uniqueEmail();
		post("/api/auth/signup", credentials(email, PASSWORD));

		Map<String, Object> loginBody = post("/api/auth/login", credentials(email, PASSWORD));
		String accessToken = (String) loginBody.get("accessToken");
		assertThat(accessToken).isNotBlank();

		HttpHeaders bearerHeaders = new HttpHeaders();
		bearerHeaders.setBearerAuth(accessToken);

		var logoutResponse = rest.exchange(base() + "/api/auth/logout",
			org.springframework.http.HttpMethod.POST,
			new HttpEntity<>(bearerHeaders),
			(Class<Map<String, Object>>) (Class<?>) Map.class);
		assertThat(logoutResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

		var meAfterLogout = rest.exchange(base() + "/api/me",
			org.springframework.http.HttpMethod.GET,
			new HttpEntity<>(bearerHeaders),
			(Class<Map<String, Object>>) (Class<?>) Map.class);
		assertThat(meAfterLogout.getStatusCode())
			.as("stateless JWT 특성: logout은 서버 세션/refresh token만 무효화하며 기발급 access token은 만료까지 유효하다 — 이는 결함이 아니라 JWT 정책이다")
			.isEqualTo(HttpStatus.OK);
	}

	private String base() {
		return "http://localhost:" + port;
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> post(String path, Map<String, String> body) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		var response = rest.exchange(base() + path,
			org.springframework.http.HttpMethod.POST,
			new HttpEntity<>(body, headers),
			(Class<Map<String, Object>>) (Class<?>) Map.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		return response.getBody();
	}
}
