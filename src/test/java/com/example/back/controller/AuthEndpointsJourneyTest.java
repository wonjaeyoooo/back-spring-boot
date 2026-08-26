package com.example.back.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

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
import org.springframework.transaction.annotation.Transactional;

import com.example.back.auth.client.SupabaseAuthClient;
import com.example.back.auth.dto.SupabaseSession;

/**
 * signup → login → refresh → logout → me 전체 흐름을 한 번에 통과시키는 여정 테스트.
 * 외부 GoTrue는 @MockitoBean으로 대체한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthEndpointsJourneyTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private SupabaseAuthClient supabaseAuthClient;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	private SupabaseSession session(String access, String refresh) {
		SupabaseSession session = new SupabaseSession();
		session.setAccessToken(access);
		session.setRefreshToken(refresh);
		return session;
	}

	@BeforeEach
	void stubJwtDecoder() {
		when(jwtDecoder.decode(anyString())).thenReturn(new Jwt(
			"stub", Instant.now(), Instant.now().plusSeconds(3600),
			Map.of("alg", "ES256"),
			Map.of("sub", UUID.randomUUID().toString(), "email", "journey@example.com")));
	}

	@Test
	void completesSignupLoginRefreshLogoutAndMe() throws Exception {
		String uniqueEmail = "journey-" + UUID.randomUUID() + "@example.com";

		when(supabaseAuthClient.signup(anyString(), anyString())).thenReturn(session(null, null));
		mockMvc.perform(post("/api/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"" + uniqueEmail + "\",\"password\":\"secret123\"}"))
			.andExpect(status().isAccepted())
			.andExpect(jsonPath("$.sessionCreated").value(false));

		when(supabaseAuthClient.login(anyString(), anyString())).thenReturn(session("eyJ.login", "refresh-1"));
		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"" + uniqueEmail + "\",\"password\":\"secret123\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accessToken").value("eyJ.login"))
			.andExpect(jsonPath("$.refreshToken").value("refresh-1"));

		when(supabaseAuthClient.refresh(anyString())).thenReturn(session("eyJ.rotated", "refresh-2"));
		mockMvc.perform(post("/api/auth/refresh")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"refreshToken\":\"refresh-1\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accessToken").value("eyJ.rotated"))
			.andExpect(jsonPath("$.refreshToken").value("refresh-2"));

		mockMvc.perform(post("/api/auth/logout")
				.header(HttpHeaders.AUTHORIZATION, "Bearer eyJ.rotated"))
			.andExpect(status().isOk());

		mockMvc.perform(get("/api/me").with(jwt().jwt(token -> token
				.subject(UUID.randomUUID().toString())
				.claim("email", "journey-me@example.com"))))
			.andExpect(status().isOk());
	}
}
