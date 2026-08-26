package com.example.back.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.example.back.repository.UserRepository;

/**
 * /api/me 첫 호출만으로 users 행이 자동 생성됨을 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MeAutoSyncTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Test
	void createsUserRowOnFirstMeCall() throws Exception {
		UUID freshSubject = UUID.randomUUID();
		assertThat(userRepository.findBySupabaseUserId(freshSubject)).isEmpty();

		mockMvc.perform(get("/api/me").with(jwt().jwt(token -> token
				.subject(freshSubject.toString())
				.claim("email", "autosync@example.com"))))
			.andExpect(status().isOk());

		assertThat(userRepository.findBySupabaseUserId(freshSubject))
			.isPresent()
			.hasValueSatisfying(user -> assertThat(user.getEmail()).isEqualTo("autosync@example.com"));
	}
}
