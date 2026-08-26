package com.example.back.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

/**
 * GoTrue 토큰/세션 응답.
 * 이메일 확인(Confirm email)이 활성화된 프로젝트에서는 signup 응답에 세션 필드가 없고 user만 반환되므로
 * accessToken/refreshToken이 null일 수 있다 — null을 오류로 취급하지 않는다.
 */
@Getter
@Setter
public class SupabaseSession {

	@JsonProperty("access_token")
	private String accessToken;

	@JsonProperty("expires_in")
	private Integer expiresIn;

	@JsonProperty("refresh_token")
	private String refreshToken;

	@JsonProperty("token_type")
	private String tokenType;

	private SupabaseUser user;

	@Getter
	@Setter
	public static class SupabaseUser {

		private String id;

		private String email;
	}
}
