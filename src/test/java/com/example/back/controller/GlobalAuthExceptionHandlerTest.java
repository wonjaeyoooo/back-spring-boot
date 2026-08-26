package com.example.back.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import com.example.back.auth.client.SupabaseAuthException;

/**
 * 4종 예외의 표준 응답 매핑 검증.
 */
class GlobalAuthExceptionHandlerTest {

	private final GlobalAuthExceptionHandler handler = new GlobalAuthExceptionHandler();

	@Test
	void mapsSupabaseAuthExceptionWithPreservedStatusAndCode() {
		ResponseEntity<Map<String, String>> response = handler.handleSupabaseAuthException(
			new SupabaseAuthException(429, "RATE_LIMITED", null));

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
		assertThat(response.getBody())
			.containsEntry("code", "RATE_LIMITED")
			.doesNotContainKey("stackTrace");
	}

	@Test
	void mapsAuthenticationExceptionTo401() {
		AuthenticationException exception = new StubAuthenticationFailure("bad token");

		ResponseEntity<Map<String, String>> response = handler.handleAuthentication(exception);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		assertThat(response.getBody()).containsEntry("code", "AUTHENTICATION_FAILED");
	}

	@Test
	void mapsAccessDeniedExceptionTo403() {
		ResponseEntity<Map<String, String>> response = handler.handleAccessDenied(new AccessDeniedException("denied"));

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
		assertThat(response.getBody()).containsEntry("code", "ACCESS_DENIED");
	}

	@Test
	void mapsValidationExceptionTo400WithFieldHint() {
		BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
		bindingResult.addError(new FieldError("request", "email", "invalid"));

		ResponseEntity<Map<String, String>> response = handler.handleValidation(
			new MethodArgumentNotValidException(null, bindingResult));

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody()).containsEntry("code", "VALIDATION_FAILED");
	}

	private static final class StubAuthenticationFailure extends AuthenticationException {
		private StubAuthenticationFailure(String message) {
			super(message);
		}
	}
}
