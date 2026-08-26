package com.example.back.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 공개/보호 엔드포인트 접근 정책 검증.
 * 익명 요청 기준: ping 200 / me 401 / redis 401 아님(매핑 없는 메서드라 405).
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityPolicyTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void allowsAnonymousAccessToPing() throws Exception {
		mockMvc.perform(get("/api/ping"))
			.andExpect(status().isOk());
	}

	@Test
	void rejectsAnonymousAccessToMe() throws Exception {
		mockMvc.perform(get("/api/me"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void keepsRedisEndpointsPubliclyReachable() throws Exception {
		// 기존 공개 동작 보존 검증: 인증 필터를 통과했다면 매핑되지 않은 HTTP 메서드로 405가 반환된다
		int status = mockMvc.perform(get("/api/redis/policy-check-key"))
			.andReturn()
			.getResponse()
			.getStatus();
		assertThat(status).isNotEqualTo(401);
	}
}
