package com.example.back.auth.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 인증 API 표준 응답.
 */
@Getter
@Builder
public class AuthSessionResponse {

	private final boolean sessionCreated;

	private final String accessToken;

	private final String refreshToken;

	private final String message;
}
