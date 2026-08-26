package com.example.back.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 로그인 요청.
 */
@Getter
@Setter
public class LoginRequest {

	@Email
	@NotBlank
	private String email;

	@NotBlank
	@Size(min = 6)
	private String password;
}
