package com.example.back.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;

/**
 * Supabase 인증 연동 설정.
 * 실제 값은 환경변수(SUPABASE_*)로 주입되며, 기본값은 플레이스홀더다(비밀 하드코딩 금지).
 */
@Getter
@ConfigurationProperties(prefix = "supabase")
public class SupabaseProperties {

	private final String baseUrl;
	private final String publishableKey;
	private final String jwksUri;
	private final String issuer;

	public SupabaseProperties(String baseUrl, String publishableKey, String jwksUri, String issuer) {
		this.baseUrl = baseUrl;
		this.publishableKey = publishableKey;
		this.jwksUri = jwksUri;
		this.issuer = issuer;
	}
}
