package com.example.back.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import lombok.RequiredArgsConstructor;

/**
 * GoTrue(Supabase Auth) REST 호출용 RestClient.
 * 모든 요청에 apikey 헤더를 기본으로 부착한다.
 */
@Configuration
@RequiredArgsConstructor
public class SupabaseClientConfig {

	private final SupabaseProperties supabaseProperties;

	@Bean
	public RestClient supabaseRestClient(RestClient.Builder builder) {
		return builder
			.baseUrl(supabaseProperties.getBaseUrl())
			.defaultHeader("apikey", supabaseProperties.getPublishableKey())
			.build();
	}
}
