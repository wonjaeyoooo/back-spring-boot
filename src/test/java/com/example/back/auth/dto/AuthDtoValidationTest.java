package com.example.back.auth.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/**
 * 인증 DTO Bean Validation 제약 직접 검증.
 */
class AuthDtoValidationTest {

	private final Validator validator;

	AuthDtoValidationTest() {
		try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
			this.validator = factory.getValidator();
		}
	}

	@Test
	void signupRejectsInvalidEmailAndShortPassword() {
		SignupRequest request = new SignupRequest();
		request.setEmail("not-an-email");
		request.setPassword("12345");

		Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);

		assertThat(violations).extracting(violation -> violation.getPropertyPath().toString())
			.containsExactlyInAnyOrder("email", "password");
	}

	@Test
	void signupAcceptsValidRequest() {
		SignupRequest request = new SignupRequest();
		request.setEmail("user@example.com");
		request.setPassword("secret123");

		assertThat(validator.validate(request)).isEmpty();
	}

	@Test
	void loginRejectsBlankFields() {
		LoginRequest request = new LoginRequest();

		Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

		assertThat(violations).isNotEmpty();
	}

	@Test
	void refreshRejectsBlankToken() {
		RefreshRequest request = new RefreshRequest();

		Set<ConstraintViolation<RefreshRequest>> violations = validator.validate(request);

		assertThat(violations).extracting(violation -> violation.getPropertyPath().toString())
			.containsExactly("refreshToken");
	}
}
