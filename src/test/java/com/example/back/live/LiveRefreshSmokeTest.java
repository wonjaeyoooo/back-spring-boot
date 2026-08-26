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
 * 라이브 refresh 스모크 — login → refresh → 회전된 새 access_token으로 /api/me 성공.
 * Supabase refresh token은 1회용 회전 정책(기본 재사용 감지 간격 10초)을 따른다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnabledIfEnvironmentVariable(named = "SUPABASE_BASE_URL", matches = ".+")
@EnabledIfEnvironmentVariable(named = "SUPABASE_PUBLISHABLE_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "SUPABASE_JWKS_URI", matches = ".+")
@EnabledIfEnvironmentVariable(named = "SUPABASE_ISSUER", matches = ".+")
class LiveRefreshSmokeTest extends LiveSupabaseSmokeSupport {

	@LocalServerPort
	private int port;

	private final RestTemplate rest = new RestTemplate();

	@Test
	@SuppressWarnings("unchecked")
	void liveRefreshReturnsRotatedUsableAccessToken() {
		String email = uniqueEmail();
		post("/api/auth/signup", credentials(email, PASSWORD));

		Map<String, Object> loginBody = post("/api/auth/login", credentials(email, PASSWORD));
		String firstRefreshToken = (String) loginBody.get("refreshToken");
		assertThat(firstRefreshToken).isNotBlank();

		Map<String, Object> refreshBody = post("/api/auth/refresh",
			Map.of("refreshToken", firstRefreshToken));
		String rotatedAccessToken = (String) refreshBody.get("accessToken");
		assertThat(rotatedAccessToken).isNotBlank();

		HttpHeaders bearerHeaders = new HttpHeaders();
		bearerHeaders.setBearerAuth(rotatedAccessToken);
		var meResponse = rest.exchange(base() + "/api/me",
			org.springframework.http.HttpMethod.GET,
			new HttpEntity<>(bearerHeaders),
			(Class<Map<String, Object>>) (Class<?>) Map.class);
		assertThat(meResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
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
		if (path.equals("/api/auth/refresh")) {
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		}
		return response.getBody();
	}
}
