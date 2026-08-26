package com.example.back.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.example.back.domain.User;
import com.example.back.repository.UserRepository;

/**
 * syncUser 멱등성 검증 — 동일 UUID 재동기화 시 중복 생성 없이 갱신만 수행.
 */
@SpringBootTest
@Transactional
class UserServiceTest {

	@Autowired
	private UserService userService;

	@Autowired
	private UserRepository userRepository;

	@Test
	void createsUserOnFirstSyncAndUpdatesWithoutDuplication() {
		UUID supabaseUserId = UUID.randomUUID();

		userService.syncUser(supabaseUserId, "sync@example.com", "닉네임");
		User resynced = userService.syncUser(supabaseUserId, "changed@example.com", "닉네임");

		assertThat(resynced.getEmail()).isEqualTo("changed@example.com");
		assertThat(userRepository.findAll())
			.filteredOn(user -> user.getSupabaseUserId().equals(supabaseUserId))
			.hasSize(1);
	}
}
