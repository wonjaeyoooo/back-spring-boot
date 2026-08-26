package com.example.back.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 토큰 재발급 요청.
 */
@Getter
@Setter
public class RefreshRequest {

	@NotBlank
	private String refreshToken;
}
