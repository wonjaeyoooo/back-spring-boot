package com.example.back.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * OpenAPI 스펙에 Bearer 인증 스킴이 노출되고 /api/me가 이를 요구하는지 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SwaggerBearerSchemeTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void openApiSpecExposesBearerAuthSecurityScheme() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.components.securitySchemes.BearerAuth.type").value("http"))
			.andExpect(jsonPath("$.components.securitySchemes.BearerAuth.scheme").value("bearer"))
			.andExpect(jsonPath("$.components.securitySchemes.BearerAuth.bearerFormat").value("JWT"));
	}

	@Test
	void meEndpointRequiresBearerAuthInSpec() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$['paths']['/api/me']['get']['security'][0]['BearerAuth']").exists());
	}
}
