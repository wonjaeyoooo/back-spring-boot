package com.example.back.config;

import java.util.Set;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jws.SignatureAlgorithm;
import org.springframework.security.web.SecurityFilterChain;

import lombok.RequiredArgsConstructor;

/**
 * Supabase JWT 리소스서버 보안 설정.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final SupabaseProperties supabaseProperties;

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

	/**
	 * Supabase JWKS 기반 JwtDecoder.
	 * Nimbus builder 기본값은 RS256뿐이라 신규 Supabase 프로젝트의 ES256 토큰이 전부 401로
	 * 거부되는 함정이 있으므로 ES256/RS256을 반드시 명시한다.
	 */
	@Bean
	public JwtDecoder jwtDecoder() {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(supabaseProperties.getJwksUri())
			.jwsAlgorithms(algorithms -> {
				algorithms.add(SignatureAlgorithm.ES256);
				algorithms.add(SignatureAlgorithm.RS256);
			})
			.build();
		decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(supabaseProperties.getIssuer()));
		return decoder;
	}
}
