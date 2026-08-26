package com.example.back.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.back.auth.client.SupabaseAuthClient;
import com.example.back.auth.client.SupabaseAuthException;
import com.example.back.auth.dto.SupabaseSession;

/**
 * POST /api/auth/refresh 와 POST /api/auth/logout.
 * refresh: 회전된 세션 전달 / logout: 헤더 검증은 컨트롤러가 수행(체인 permitAll).
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerRefreshLogoutTest {

	private static final String ROTATED_SESSION_JSON = """
		{
		  "access_token": "eyJ.rotated",
		  "expires_in": 3600,
		  "refresh_token": "rotated-refresh",
		  "token_type": "bearer"
		}
		""";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private SupabaseAuthClient supabaseAuthClient;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	/**
	 * permitAll 경로도 Bearer 헤더가 있으면 필터가 JWT 검증을 시도하므로
	 * 컨트롤러 로직을 시험하려면 디코더를 스텁해 통과시킨다.
	 */
	@BeforeEach
	void stubJwtDecoder() {
		Jwt jwt = new Jwt("stubbed-token",
			java.time.Instant.now(),
			java.time.Instant.now().plusSeconds(3600),
			java.util.Map.of("alg", "ES256"),
			java.util.Map.of("sub", "11111111-2222-3333-4444-555555555555"));
		when(jwtDecoder.decode(anyString())).thenReturn(jwt);
	}

	@Test
	void refreshReturnsRotatedSessionWithoutServerSideStorage() throws Exception {
		SupabaseSession session = new SupabaseSession();
		session.setAccessToken("eyJ.rotated");
		session.setRefreshToken("rotated-refresh");
		when(supabaseAuthClient.refresh(anyString())).thenReturn(session);

		mockMvc.perform(post("/api/auth/refresh")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"refreshToken\":\"old-refresh-token\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accessToken").value("eyJ.rotated"))
			.andExpect(jsonPath("$.refreshToken").value("rotated-refresh"));

		verify(supabaseAuthClient).refresh("old-refresh-token");
	}

	@Test
	void refreshRejectsBlankBodyWith400() throws Exception {
		mockMvc.perform(post("/api/auth/refresh")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
			.andExpect(status().isBadRequest());
	}

	@Test
	void logoutSendsBearerTokenToClient() throws Exception {
		mockMvc.perform(post("/api/auth/logout")
				.header(HttpHeaders.AUTHORIZATION, "Bearer access-token-abc"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").isNotEmpty());

		verify(supabaseAuthClient).logout("access-token-abc");
	}

	@Test
	void logoutRejectsMissingAuthorizationHeader() throws Exception {
		mockMvc.perform(post("/api/auth/logout"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));
	}

	@Test
	void logoutRejectsMalformedAuthorizationHeader() throws Exception {
		mockMvc.perform(post("/api/auth/logout")
				.header(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));
	}

	@Test
	void logoutMapsSupabaseFailureToStandardizedError() throws Exception {
		doThrow(new SupabaseAuthException(401, "AUTHENTICATION_FAILED", null))
			.when(supabaseAuthClient).logout(anyString());

		mockMvc.perform(post("/api/auth/logout")
				.header(HttpHeaders.AUTHORIZATION, "Bearer expired-token"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));
	}
}
