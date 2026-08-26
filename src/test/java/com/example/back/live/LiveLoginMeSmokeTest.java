package com.example.back.live;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Map;
import java.util.UUID;

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

import com.example.back.repository.UserRepository;

/**
 * 라이브 signup → login → GET /api/me 흐름.
 * 단언 6항목: signup 성공 / login 200 / access_token 존재 / me 200 / email 일치 / users 행 존재.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnabledIfEnvironmentVariable(named = "SUPABASE_BASE_URL", matches = ".+")
@EnabledIfEnvironmentVariable(named = "SUPABASE_PUBLISHABLE_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "SUPABASE_JWKS_URI", matches = ".+")
@EnabledIfEnvironmentVariable(named = "SUPABASE_ISSUER", matches = ".+")
class LiveLoginMeSmokeTest extends LiveSupabaseSmokeSupport {

	@LocalServerPort
	private int port;

	private final RestTemplate rest = new RestTemplate();

	@Autowired
	private UserRepository userRepository;

	@Test
	@SuppressWarnings("unchecked")
	void liveLoginThenMeReturnsSyncedProfile() {
		String email = uniqueEmail();
		Map<String, Object> signupResponse = post("/api/auth/signup", credentials(email, PASSWORD));
		assertNotNull(signupResponse, "signup 응답이 비어 있으면 안 된다");

		ResponseEntity<Map<String, Object>> loginResponse =
			postForEntity("/api/auth/login", credentials(email, PASSWORD));
		assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		String accessToken = (String) loginResponse.getBody().get("accessToken");
		assertThat(accessToken).isNotBlank();

		HttpHeaders bearerHeaders = new HttpHeaders();
		bearerHeaders.setBearerAuth(accessToken);
		ResponseEntity<Map<String, Object>> meResponse = rest.exchange(
			base() + "/api/me",
			org.springframework.http.HttpMethod.GET,
			new HttpEntity<>(bearerHeaders),
			(Class<Map<String, Object>>) (Class<?>) Map.class);

		assertThat(meResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(meResponse.getBody().get("email")).isEqualTo(email);

		UUID supabaseUserId = UUID.fromString((String) meResponse.getBody().get("supabaseUserId"));
		assertThat(userRepository.findBySupabaseUserId(supabaseUserId))
			.as("login 동기화로 users 행이 생성되어야 한다")
			.isPresent();
	}

	private String base() {
		return "http://localhost:" + port;
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> post(String path, Map<String, String> body) {
		return postForEntity(path, body).getBody();
	}

	@SuppressWarnings("unchecked")
	private ResponseEntity<Map<String, Object>> postForEntity(String path, Map<String, String> body) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		return rest.exchange(base() + path,
			org.springframework.http.HttpMethod.POST,
			new HttpEntity<>(body, headers),
			(Class<Map<String, Object>>) (Class<?>) Map.class);
	}
}
