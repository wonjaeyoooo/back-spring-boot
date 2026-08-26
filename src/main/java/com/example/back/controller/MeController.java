package com.example.back.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.back.auth.dto.MeResponse;
import com.example.back.domain.User;
import com.example.back.service.CurrentUserService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

/**
 * 인증된 사용자 자신의 정보를 반환하는 보호 엔드포인트.
 */
@RestController
@RequiredArgsConstructor
public class MeController {

	private final CurrentUserService currentUserService;

	@Operation(summary = "내 정보 조회")
	@SecurityRequirement(name = "BearerAuth")
	@GetMapping("/api/me")
	public ResponseEntity<MeResponse> me(@AuthenticationPrincipal Jwt jwt) {
		User user = currentUserService.resolve(jwt.getSubject(), jwt.getClaimAsString("email"));
		return ResponseEntity.ok(new MeResponse(user));
	}
}
