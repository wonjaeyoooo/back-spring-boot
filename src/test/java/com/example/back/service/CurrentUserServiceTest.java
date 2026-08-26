package com.example.back.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.back.auth.client.SupabaseAuthException;
import com.example.back.domain.User;

/**
 * JWT sub → 사용자 연결 단위 테스트.
 */
@ExtendWith(MockitoExtension.class)
class CurrentUserServiceTest {

	@Mock
	private UserService userService;

	@InjectMocks
	private CurrentUserService currentUserService;

	@Test
	void resolvesValidUuidSubjectToSyncedUser() {
		UUID supabaseUserId = UUID.randomUUID();
		User synced = new User(supabaseUserId, "me@example.com", null);
		when(userService.syncUser(eq(supabaseUserId), eq("me@example.com"), isNull())).thenReturn(synced);

		User result = currentUserService.resolve(supabaseUserId.toString(), "me@example.com");

		assertThat(result).isSameAs(synced);
	}

	@Test
	void rejectsNonUuidSubjectAsAuthenticationFailureNot500() {
		assertThatThrownBy(() -> currentUserService.resolve("not-a-uuid", "me@example.com"))
			.isInstanceOfSatisfying(SupabaseAuthException.class, exception -> {
				assertThat(exception.getHttpStatus()).isEqualTo(401);
				assertThat(exception.getCode()).isEqualTo("INVALID_TOKEN_SUBJECT");
			});
		verify(userService, org.mockito.Mockito.never()).syncUser(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
	}
}
