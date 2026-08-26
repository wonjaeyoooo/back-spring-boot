package com.example.back.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * GET /api/me — 유효 JWT 200 / 익명 401 / 자동 사용자 동기화.
 * jwt() post-processor는 필터를 거치지 않고 principal을 직접 주입한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MeControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void returns200WithProfileForValidJwt() throws Exception {
		String subject = UUID.randomUUID().toString();

		mockMvc.perform(get("/api/me").with(jwt().jwt(token -> token
				.subject(subject)
				.claim("email", "me@example.com"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.supabaseUserId").value(subject))
			.andExpect(jsonPath("$.email").value("me@example.com"))
			.andExpect(jsonPath("$.createdAt").isNotEmpty());
	}

	@Test
	void rejectsAnonymousRequestWith401() throws Exception {
		mockMvc.perform(get("/api/me"))
			.andExpect(status().isUnauthorized());
	}
}
