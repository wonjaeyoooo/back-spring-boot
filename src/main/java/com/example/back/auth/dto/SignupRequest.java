package com.example.back.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 회원가입 요청. 비밀번호 최소 길이는 GoTrue 기본 정책(6자)에 맞춘다.
 */
@Getter
@Setter
public class SignupRequest {

	@Email
	@NotBlank
	private String email;

	@NotBlank
	@Size(min = 6)
	private String password;

	@Size(max = 50)
	private String nickname;
}
