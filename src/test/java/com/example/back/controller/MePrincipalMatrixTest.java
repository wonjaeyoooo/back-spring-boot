package com.example.back.controller;

import static org.assertj.core.api.Assertions.assertThat;
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

import com.example.back.repository.UserRepository;

/**
 * principal 케이스 매트릭스(네트워크 불필요):
 * 유효 UUID sub / 기존 사용자 / email 클레임 누락 / 비-UUID sub / 익명.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MePrincipalMatrixTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Test
	void acceptsValidUuidSubjectWithEmailClaim() throws Exception {
		String subject = UUID.randomUUID().toString();

		mockMvc.perform(get("/api/me").with(jwt().jwt(token -> token
				.subject(subject)
				.claim("email", "valid@example.com"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.email").value("valid@example.com"));
	}

	@Test
	void acceptsPreExistingUserWithoutCreatingDuplicate() throws Exception {
		UUID subject = UUID.randomUUID();
		userRepository.saveAndFlush(new com.example.back.domain.User(subject, "pre@example.com", "기존"));

		mockMvc.perform(get("/api/me").with(jwt().jwt(token -> token
				.subject(subject.toString())
				.claim("email", "pre@example.com"))))
			.andExpect(status().isOk());

		assertThat(userRepository.findAll())
			.filteredOn(user -> user.getSupabaseUserId().equals(subject))
			.hasSize(1);
	}

	@Test
	void rejectsMissingEmailClaimWith401() throws Exception {
		String subject = UUID.randomUUID().toString();

		mockMvc.perform(get("/api/me").with(jwt().jwt(token -> token
				.subject(subject))))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("MISSING_EMAIL_CLAIM"));
	}

	@Test
	void rejectsNonUuidSubjectWith401() throws Exception {
		mockMvc.perform(get("/api/me").with(jwt().jwt(token -> token
				.subject("definitely-not-a-uuid")
				.claim("email", "evil@example.com"))))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("INVALID_TOKEN_SUBJECT"));
	}

	@Test
	void rejectsAnonymousWith401() throws Exception {
		mockMvc.perform(get("/api/me"))
			.andExpect(status().isUnauthorized());
	}
}
