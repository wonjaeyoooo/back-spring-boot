package com.example.back.auth.client;

import lombok.Getter;

/**
 * GoTrue 호출 실패를 표현하는 예외.
 * HTTP 상태와 표준 코드만 노출하고 외부 오류 원문은 감춘다.
 */
@Getter
public class SupabaseAuthException extends RuntimeException {

	private final int httpStatus;
	private final String code;

	public SupabaseAuthException(int httpStatus, String code, Throwable cause) {
		super("인증 서버 요청이 실패했습니다.", cause);
		this.httpStatus = httpStatus;
		this.code = code;
	}
}
