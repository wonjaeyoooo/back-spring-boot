package com.example.back.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.back.auth.client.SupabaseAuthException;

/**
 * 인증 관련 오류를 표준 {"code","message"} 형태로 변환한다.
 * GoTrue 원문과 스택 트레이스는 노출하지 않는다.
 */
@RestControllerAdvice
public class GlobalAuthExceptionHandler {

	@ExceptionHandler(SupabaseAuthException.class)
	public ResponseEntity<Map<String, String>> handleSupabaseAuthException(SupabaseAuthException exception) {
		HttpStatus status = HttpStatus.resolve(exception.getHttpStatus());
		return ResponseEntity.status(status != null ? status : HttpStatus.BAD_GATEWAY)
			.body(body(exception.getCode(), "인증 요청을 처리하지 못했습니다."));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException exception) {
		FieldError fieldError = exception.getBindingResult().getFieldError();
		String message = fieldError != null ? "요청 값이 올바르지 않습니다: " + fieldError.getField() : "요청 값이 올바르지 않습니다.";
		return ResponseEntity.badRequest()
			.body(body("VALIDATION_FAILED", message));
	}

	private Map<String, String> body(String code, String message) {
		Map<String, String> response = new HashMap<>();
		response.put("code", code);
		response.put("message", message);
		return response;
	}
}
