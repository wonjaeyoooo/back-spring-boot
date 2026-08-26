package com.example.back.auth.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.example.back.domain.User;

import lombok.Getter;

/**
 * GET /api/me 응답.
 */
@Getter
public class MeResponse {

	private final UUID supabaseUserId;
	private final String email;
	private final String nickname;
	private final OffsetDateTime createdAt;

	public MeResponse(User user) {
		this.supabaseUserId = user.getSupabaseUserId();
		this.email = user.getEmail();
		this.nickname = user.getNickname();
		this.createdAt = user.getCreatedAt();
	}
}
