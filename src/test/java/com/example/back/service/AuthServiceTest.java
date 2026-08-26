package com.example.back.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.back.auth.client.SupabaseAuthClient;
import com.example.back.auth.dto.SignupRequest;
import com.example.back.auth.dto.SupabaseSession;

/**
 * signup 흐름 단위 테스트 — 사용자 동기화 호출과 세션 부재 구분을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@Mock
	private SupabaseAuthClient supabaseAuthClient;

	@Mock
	private UserService userService;

	@InjectMocks
	private AuthService authService;

	private SignupRequest request() {
		SignupRequest request = new SignupRequest();
		request.setEmail("new@example.com");
		request.setPassword("secret123");
		request.setNickname("닉네임");
		return request;
	}

	@Test
	void reportsMissingSessionAsGuidanceNotError() {
		SupabaseSession userOnly = new SupabaseSession();
		SupabaseSession.SupabaseUser user = new SupabaseSession.SupabaseUser();
		user.setId(UUID.randomUUID().toString());
		user.setEmail("new@example.com");
		userOnly.setUser(user);
		when(supabaseAuthClient.signup(anyString(), anyString())).thenReturn(userOnly);

		AuthService.SignupResult result = authService.signup(request());

		assertThat(result.sessionCreated()).isFalse();
		assertThat(result.message()).isNotBlank();
		verify(userService).syncUser(UUID.fromString(user.getId()), "new@example.com", "닉네임");
	}

	@Test
	void returnsSessionWhenEmailConfirmationDisabled() {
		SupabaseSession fullSession = new SupabaseSession();
		fullSession.setAccessToken("eyJ.access");
		fullSession.setRefreshToken("refresh-token");
		when(supabaseAuthClient.signup(any(), any())).thenReturn(fullSession);

		AuthService.SignupResult result = authService.signup(request());

		assertThat(result.sessionCreated()).isTrue();
		assertThat(result.session().getAccessToken()).isEqualTo("eyJ.access");
	}
}
