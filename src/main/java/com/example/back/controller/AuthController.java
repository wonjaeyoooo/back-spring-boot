package com.example.back.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.back.auth.dto.AuthSessionResponse;
import com.example.back.auth.dto.SignupRequest;
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
}
