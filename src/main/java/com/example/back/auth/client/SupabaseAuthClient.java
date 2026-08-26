package com.example.back.auth.client;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.example.back.auth.dto.SupabaseSession;

import lombok.RequiredArgsConstructor;

/**
 * Supabase GoTrue Auth REST 클라이언트.
 */
@Component
@RequiredArgsConstructor
public class SupabaseAuthClient {

	private final RestClient supabaseRestClient;

	public SupabaseSession signup(String email, String password) {
		return supabaseRestClient.post()
			.uri("/auth/v1/signup")
			.contentType(MediaType.APPLICATION_JSON)
			.body(new SignupPayload(email, password))
			.retrieve()
			.body(SupabaseSession.class);
	}

	private record SignupPayload(String email, String password) {
	}
}
