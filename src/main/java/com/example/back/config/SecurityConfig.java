package com.example.back.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Supabase JWT 리소스서버 보안 설정.
 * JwtDecoder는 spring.security.oauth2.resourceserver.jwt.jwk-set-uri 프로퍼티로 자동 구성되며
 * ES256/RS256 명시 버전은 이후 태스크에서 교체된다.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			// stateless Bearer 토큰 API이므로 CSRF 보호는 비활성화
			.csrf(AbstractHttpConfigurer::disable)
			// 세션을 만들지 않고 모든 요청을 토큰 기반으로 처리
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
		return http.build();
	}
}
