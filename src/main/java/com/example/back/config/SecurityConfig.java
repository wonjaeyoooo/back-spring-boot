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
			.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
			.authorizeHttpRequests(auth -> auth
				// 기존 공개 엔드포인트 유지
				.requestMatchers("/api/ping").permitAll()
				// Swagger / OpenAPI 문서 공개
				.requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
				// Actuator는 헬스체크와 info만 공개
				.requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
				// 기존 Redis/Kafka API는 현재까지 공개로 운영됨 — 잠금은 별도 후속 과제
				.requestMatchers("/api/redis/**", "/api/kafka/**").permitAll()
				// 인증 API 자체는 물론 공개 (logout은 컨트롤러에서 Authorization 헤더 유무를 검증)
				.requestMatchers("/api/auth/signup", "/api/auth/login", "/api/auth/refresh", "/api/auth/logout")
				.permitAll()
				// 그 외 모든 요청은 유효한 Supabase JWT 필요
				.anyRequest().authenticated());
		return http.build();
	}
}
