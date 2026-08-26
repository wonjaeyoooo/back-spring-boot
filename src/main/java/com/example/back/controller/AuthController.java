package com.example.back.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.back.auth.dto.AuthSessionResponse;
import com.example.back.auth.dto.LoginRequest;
import com.example.back.auth.dto.RefreshRequest;
import com.example.back.auth.dto.SignupRequest;
import com.example.back.auth.dto.SupabaseSession;
import com.example.back.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 회원가입 엔드포인트.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	@PostMapping("/signup")
	public ResponseEntity<AuthSessionResponse> signup(@Valid @RequestBody SignupRequest request) {
		AuthService.SignupResult result = authService.signup(request);
		if (!result.sessionCreated()) {
			return ResponseEntity.accepted()
				.body(AuthSessionResponse.builder()
					.sessionCreated(false)
					.message(result.message())
					.build());
		}
		return ResponseEntity.ok()
			.body(AuthSessionResponse.builder()
				.sessionCreated(true)
				.accessToken(result.session().getAccessToken())
				.refreshToken(result.session().getRefreshToken())
				.build());
	}

	@PostMapping("/login")
	public ResponseEntity<AuthSessionResponse> login(@Valid @RequestBody LoginRequest request) {
		SupabaseSession session = authService.login(request);
		return ResponseEntity.ok()
			.body(AuthSessionResponse.builder()
				.sessionCreated(true)
				.accessToken(session.getAccessToken())
				.refreshToken(session.getRefreshToken())
				.build());
	}

	@PostMapping("/refresh")
	public ResponseEntity<AuthSessionResponse> refresh(@Valid @RequestBody RefreshRequest request) {
		SupabaseSession session = authService.refresh(request.getRefreshToken());
		return ResponseEntity.ok()
			.body(AuthSessionResponse.builder()
				.sessionCreated(true)
				.accessToken(session.getAccessToken())
				.refreshToken(session.getRefreshToken())
				.build());
	}

	/**
	 * 체인이 permitAll이므로 Authorization 헤더의 유무와 형식을 여기서 검증한다.
	 */
	@PostMapping("/logout")
	public ResponseEntity<Map<String, String>> logout(
		@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
		if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
			Map<String, String> unauthorized = new HashMap<>();
			unauthorized.put("code", "AUTHENTICATION_FAILED");
			unauthorized.put("message", "인증 토큰이 필요합니다.");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(unauthorized);
		}
		authService.logout(authorization.substring(7));
		Map<String, String> body = new HashMap<>();
		body.put("message", "로그아웃되었습니다.");
		return ResponseEntity.ok(body);
	}
}
