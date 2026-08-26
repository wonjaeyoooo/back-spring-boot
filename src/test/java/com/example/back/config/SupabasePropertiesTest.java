package com.example.back.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * SupabaseProperties 환경변수/프로퍼티 바인딩 검증.
 * 생성자 바인딩(단일 파라미터 생성자)이 4개 값을 모두 채우는지 확인한다.
 */
@SpringBootTest(properties = {
	"supabase.base-url=https://ittest.supabase.co",
	"supabase.publishable-key=it-publishable-key",
	"supabase.jwks-uri=https://ittest.supabase.co/auth/v1/.well-known/jwks.json",
	"supabase.issuer=https://ittest.supabase.co/auth/v1"
})
class SupabasePropertiesTest {

	@Autowired
	private SupabaseProperties supabaseProperties;

	@Test
	void bindsAllSupabasePropertiesFromConfiguration() {
		assertThat(supabaseProperties.getBaseUrl()).isEqualTo("https://ittest.supabase.co");
		assertThat(supabaseProperties.getPublishableKey()).isEqualTo("it-publishable-key");
		assertThat(supabaseProperties.getJwksUri())
			.isEqualTo("https://ittest.supabase.co/auth/v1/.well-known/jwks.json");
		assertThat(supabaseProperties.getIssuer()).isEqualTo("https://ittest.supabase.co/auth/v1");
	}
}
