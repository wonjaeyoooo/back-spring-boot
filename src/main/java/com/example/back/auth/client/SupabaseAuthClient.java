package com.example.back.auth.client;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.example.back.auth.dto.SupabaseSession;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.RequiredArgsConstructor;

/**
 * Supabase GoTrue Auth REST 클라이언트.
 * 오류는 HTTP 상태와 표준 코드만 담은 SupabaseAuthException으로 변환된다.
 */
@Component
@RequiredArgsConstructor
public class SupabaseAuthClient {

	private final RestClient supabaseRestClient;

	public SupabaseSession signup(String email, String password) {
		return execute("/auth/v1/signup", new EmailPasswordPayload(email, password), SupabaseSession.class);
	}

	public SupabaseSession login(String email, String password) {
		return execute("/auth/v1/token?grant_type=password",
			new EmailPasswordPayload(email, password), SupabaseSession.class);
	}

	/** refresh 토큰은 절대 저장하지 않고 GoTrue에 전달해 회전된 새 세션만 돌려받는다 */
	public SupabaseSession refresh(String refreshToken) {
		return execute("/auth/v1/token?grant_type=refresh_token",
			new RefreshTokenPayload(refreshToken), SupabaseSession.class);
	}

	public void logout(String accessToken) {
		try {
			supabaseRestClient.post()
				.uri("/auth/v1/logout")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
				.retrieve()
				.toBodilessEntity();
		} catch (RestClientResponseException exception) {
			throw toAuthException(exception);
		}
	}

	private <T> T execute(String uri, Object payload, Class<T> responseType) {
		try {
			return supabaseRestClient.post()
				.uri(uri)
				.contentType(MediaType.APPLICATION_JSON)
				.body(payload)
				.retrieve()
				.body(responseType);
		} catch (RestClientResponseException exception) {
			throw toAuthException(exception);
		}
	}

	private SupabaseAuthException toAuthException(RestClientResponseException exception) {
		int status = exception.getStatusCode().value();
		String code = switch (status) {
			case 400, 401, 403 -> "AUTHENTICATION_FAILED";
			case 422 -> "VALIDATION_FAILED";
			case 429 -> "RATE_LIMITED";
			default -> "SUPABASE_AUTH_ERROR";
		};
		return new SupabaseAuthException(status, code, exception);
	}

	private record EmailPasswordPayload(String email, String password) {
	}

	private record RefreshTokenPayload(@JsonProperty("refresh_token") String refreshToken) {
	}
}
