package com.example.back.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.back.auth.client.SupabaseAuthClient;
import com.example.back.auth.client.SupabaseAuthException;
import com.example.back.auth.dto.LoginRequest;
import com.example.back.auth.dto.SignupRequest;
import com.example.back.auth.dto.SupabaseSession;

import lombok.RequiredArgsConstructor;

/**
 * 회원가입/로그인 등 인증 흐름을 조율한다.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

	private final SupabaseAuthClient supabaseAuthClient;
	private final UserService userService;

	public record SignupResult(boolean sessionCreated, SupabaseSession session, String message) {
	}

	/**
	 * 이메일 확인(Confirm email)이 활성화된 프로젝트에서는 GoTrue가 세션 없이 user만 반환하므로
	 * 세션 부재를 오류가 아니라 안내 대상으로 구분한다.
	 */
	public SignupResult signup(SignupRequest request) {
		SupabaseSession session = supabaseAuthClient.signup(request.getEmail(), request.getPassword());
		syncUserFromSession(session, request.getNickname());

		if (session.getAccessToken() == null) {
			return new SignupResult(false, session,
				"확인 이메일을 발송했습니다. 메일함에서 인증을 완료한 뒤 로그인해 주세요.");
		}
		return new SignupResult(true, session, null);
	}

	private void syncUserFromSession(SupabaseSession session, String nickname) {
		if (session.getUser() == null || session.getUser().getId() == null) {
			return;
		}
		UUID.fromString(session.getUser().getId());
		userService.syncUser(
			UUID.fromString(session.getUser().getId()),
			session.getUser().getEmail(),
			nickname);
	}

	public SupabaseSession login(LoginRequest request) {
		SupabaseSession session;
		try {
			session = supabaseAuthClient.login(request.getEmail(), request.getPassword());
		} catch (SupabaseAuthException exception) {
			// 자격증명 실패는 사용자 존재 여부를 추론할 수 없도록 항상 균일한 401로 응답한다
			if (exception.getHttpStatus() == 400 || exception.getHttpStatus() == 403) {
				throw new SupabaseAuthException(401, "AUTHENTICATION_FAILED", exception);
			}
			throw exception;
		}
		syncUserFromSession(session, null);
		return session;
	}
}
