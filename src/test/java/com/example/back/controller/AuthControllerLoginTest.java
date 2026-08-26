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
 * POST /api/auth/login — 성공 시 토큰 반환, 실패 시 균일한 401.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerLoginTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private SupabaseAuthClient supabaseAuthClient;

	@Test
	void returns200WithTokensOnSuccessfulLogin() throws Exception {
		SupabaseSession session = new SupabaseSession();
		session.setAccessToken("eyJ.login-access");
		session.setRefreshToken("login-refresh");
		when(supabaseAuthClient.login(anyString(), anyString())).thenReturn(session);

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"user@example.com\",\"password\":\"secret123\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accessToken").value("eyJ.login-access"))
			.andExpect(jsonPath("$.refreshToken").value("login-refresh"));
	}

	@Test
	void returnsUniform401WithoutUserExistenceHints() throws Exception {
		when(supabaseAuthClient.login(any(), any()))
			.thenThrow(new SupabaseAuthException(400, "AUTHENTICATION_FAILED", null));

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"wrong@example.com\",\"password\":\"wrong-password\"}"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"))
			.andExpect(jsonPath("$.message").value("인증 요청을 처리하지 못했습니다."));
	}
}
