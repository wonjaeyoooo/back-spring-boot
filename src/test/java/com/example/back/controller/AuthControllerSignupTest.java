package com.example.back.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.example.back.auth.client.SupabaseAuthClient;
import com.example.back.auth.client.SupabaseAuthException;
import com.example.back.auth.dto.SupabaseSession;

/**
 * POST /api/auth/signup 4케이스: 세션 있음 200 / 세션 없음 202 / 검증 실패 400 / GoTrue 오류 표준화.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerSignupTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private SupabaseAuthClient supabaseAuthClient;

	private SupabaseSession fullSession() {
		SupabaseSession session = new SupabaseSession();
		session.setAccessToken("eyJ.access");
		session.setRefreshToken("refresh-token");
		return session;
	}

	@Test
	void returns200WithTokensWhenSignupReturnsSession() throws Exception {
		when(supabaseAuthClient.signup(anyString(), anyString())).thenReturn(fullSession());

		mockMvc.perform(post("/api/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"with-session@example.com\",\"password\":\"secret123\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.sessionCreated").value(true))
			.andExpect(jsonPath("$.accessToken").value("eyJ.access"));
	}

	@Test
	void returns202WithGuidanceWhenEmailConfirmationRequired() throws Exception {
		SupabaseSession userOnly = new SupabaseSession();
		when(supabaseAuthClient.signup(anyString(), anyString())).thenReturn(userOnly);

		mockMvc.perform(post("/api/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"confirm-me@example.com\",\"password\":\"secret123\"}"))
			.andExpect(status().isAccepted())
			.andExpect(jsonPath("$.sessionCreated").value(false))
			.andExpect(jsonPath("$.message").isNotEmpty());
	}

	@Test
	void returns400WithStandardBodyForInvalidPayload() throws Exception {
		mockMvc.perform(post("/api/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"not-an-email\",\"password\":\"12345\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.message").isNotEmpty());
	}

	@Test
	void mapsSupabaseFailureToStandardizedError() throws Exception {
		when(supabaseAuthClient.signup(any(), any()))
			.thenThrow(new SupabaseAuthException(400, "AUTHENTICATION_FAILED", null));

		mockMvc.perform(post("/api/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"blocked@example.com\",\"password\":\"secret123\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"))
			.andExpect(jsonPath("$.message").value("인증 요청을 처리하지 못했습니다."));
	}
}
