package com.example.back.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.back.auth.client.SupabaseAuthException;
import com.example.back.domain.User;

import lombok.RequiredArgsConstructor;

/**
 * JWT의 sub 클레임을 로컬 사용자로 연결한다.
 */
@Service
@RequiredArgsConstructor
public class CurrentUserService {

	private final UserService userService;

	/**
	 * sub가 UUID 형식이 아니거나 email 클레임이 없으면 토큰을 신뢰할 수 없는 것으로 보고
	 * 500이 아니라 인증 실패(401)로 전환한다. Supabase 인증 토큰은 항상 email을 포함한다.
	 */
	public User resolve(String subject, String email) {
		final UUID supabaseUserId;
		try {
			supabaseUserId = UUID.fromString(subject);
		} catch (IllegalArgumentException | NullPointerException exception) {
			throw new SupabaseAuthException(401, "INVALID_TOKEN_SUBJECT", exception);
		}
		if (email == null || email.isBlank()) {
			throw new SupabaseAuthException(401, "MISSING_EMAIL_CLAIM", null);
		}
		return userService.syncUser(supabaseUserId, email, null);
	}
}
