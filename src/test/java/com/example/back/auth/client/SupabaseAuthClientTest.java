package com.example.back.auth.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.example.back.auth.dto.SupabaseSession;

/**
 * GoTrue signup 호출 검증 — URL, apikey 헤더, JSON 바디, 응답 파싱.
 * MockRestServiceServer로 외부 네트워크 없이 실측한다.
 */
class SupabaseAuthClientTest {

	private final RestClient.Builder builder = RestClient.builder();
	private final MockRestServiceServer mockServer = MockRestServiceServer.bindTo(builder).build();
	private final SupabaseAuthClient client = new SupabaseAuthClient(
		builder
			.baseUrl("https://stub.supabase.co")
			.defaultHeader("apikey", "test-api-key")
			.build());

	@Test
	void sendsSignupWithApikeyHeaderAndJsonBody() {
		mockServer.expect(requestTo("https://stub.supabase.co/auth/v1/signup"))
			.andExpect(method(HttpMethod.POST))
			.andExpect(header("apikey", "test-api-key"))
			.andExpect(jsonPath("$.email").value("new@example.com"))
			.andExpect(jsonPath("$.password").value("secret123"))
			.andRespond(withSuccess("""
				{"user":{"id":"11111111-2222-3333-4444-555555555555","email":"new@example.com"}}
				""", MediaType.APPLICATION_JSON));

		SupabaseSession session = client.signup("new@example.com", "secret123");

		mockServer.verify();
		assertThat(session.getAccessToken()).isNull();
		assertThat(session.getUser().getId()).isEqualTo("11111111-2222-3333-4444-555555555555");
		assertThat(session.getUser().getEmail()).isEqualTo("new@example.com");
	}

	@Test
	void parsesFullSessionWhenEmailConfirmationDisabled() {
		mockServer.expect(requestTo("https://stub.supabase.co/auth/v1/signup"))
			.andRespond(withSuccess("""
				{
				  "access_token": "eyJ.stub",
				  "expires_in": 3600,
				  "refresh_token": "stub-refresh",
				  "token_type": "bearer",
				  "user": {"id":"11111111-2222-3333-4444-555555555555","email":"new@example.com"}
				}
				""", MediaType.APPLICATION_JSON));

		SupabaseSession session = client.signup("new@example.com", "secret123");

		mockServer.verify();
		assertThat(session.getAccessToken()).isEqualTo("eyJ.stub");
		assertThat(session.getExpiresIn()).isEqualTo(3600);
		assertThat(session.getRefreshToken()).isEqualTo("stub-refresh");
	}

	@Test
	void sendsPasswordGrantLoginToTokenEndpoint() {
		mockServer.expect(requestTo("https://stub.supabase.co/auth/v1/token?grant_type=password"))
			.andExpect(method(HttpMethod.POST))
			.andExpect(jsonPath("$.email").value("user@example.com"))
			.andExpect(jsonPath("$.password").value("secret123"))
			.andRespond(withSuccess(fullSessionJson(), MediaType.APPLICATION_JSON));

		SupabaseSession session = client.login("user@example.com", "secret123");

		mockServer.verify();
		assertThat(session.getAccessToken()).isEqualTo("eyJ.stub");
		assertThat(session.getUser().getEmail()).isEqualTo("user@example.com");
	}

	@Test
	void sendsRefreshTokenGrantAndParsesRotatedSession() {
		mockServer.expect(requestTo("https://stub.supabase.co/auth/v1/token?grant_type=refresh_token"))
			.andExpect(method(HttpMethod.POST))
			.andExpect(jsonPath("$.refresh_token").value("old-refresh-token"))
			.andRespond(withSuccess(fullSessionJson(), MediaType.APPLICATION_JSON));

		SupabaseSession session = client.refresh("old-refresh-token");

		mockServer.verify();
		assertThat(session.getAccessToken()).isEqualTo("eyJ.stub");
		assertThat(session.getRefreshToken()).isEqualTo("stub-refresh");
	}

	@Test
	void sendsLogoutWithBearerAuthorizationHeader() {
		mockServer.expect(requestTo("https://stub.supabase.co/auth/v1/logout"))
			.andExpect(method(HttpMethod.POST))
			.andExpect(header(org.springframework.http.HttpHeaders.AUTHORIZATION, "Bearer access-token-abc"))
			.andRespond(withNoContent());

		client.logout("access-token-abc");

		mockServer.verify();
	}

	private String fullSessionJson() {
		return """
			{
			  "access_token": "eyJ.stub",
			  "expires_in": 3600,
			  "refresh_token": "stub-refresh",
			  "token_type": "bearer",
			  "user": {"id":"11111111-2222-3333-4444-555555555555","email":"user@example.com"}
			}
			""";
	}
}
