package com.example.back.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import com.example.back.domain.User;

/**
 * UserRepository 실제 Postgres 통합 테스트 (롤백 보장).
 * happy path 3건 + 유니크 제약(supabase_user_id/email) 위반 2건.
 */
@SpringBootTest
@Transactional
class UserRepositoryTest {

	@Autowired
	private UserRepository userRepository;

	@Test
	void savesAndFindsBySupabaseUserId() {
		UUID supabaseId = UUID.randomUUID();
		userRepository.save(new User(supabaseId, "user1@example.com", "닉네임1"));

		User found = userRepository.findBySupabaseUserId(supabaseId).orElseThrow();

		assertThat(found.getSupabaseUserId()).isEqualTo(supabaseId);
		assertThat(found.getEmail()).isEqualTo("user1@example.com");
		assertThat(found.getNickname()).isEqualTo("닉네임1");
		// DB 기본값이 아닌 Hibernate 타임스탬프가 채워졌는지 확인
		assertThat(found.getCreatedAt()).isNotNull();
		assertThat(found.getUpdatedAt()).isNotNull();
	}

	@Test
	void findsByEmail() {
		userRepository.save(new User(UUID.randomUUID(), "findme@example.com", null));

		User found = userRepository.findByEmail("findme@example.com").orElseThrow();

		assertThat(found.getEmail()).isEqualTo("findme@example.com");
	}

	@Test
	void rejectsDuplicateSupabaseUserId() {
		UUID duplicated = UUID.randomUUID();
		userRepository.saveAndFlush(new User(duplicated, "first@example.com", null));

		assertThatThrownBy(() -> userRepository.saveAndFlush(new User(duplicated, "second@example.com", null)))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void rejectsDuplicateEmail() {
		userRepository.saveAndFlush(new User(UUID.randomUUID(), "dup@example.com", null));

		assertThatThrownBy(() -> userRepository.saveAndFlush(new User(UUID.randomUUID(), "dup@example.com", null)))
			.isInstanceOf(DataIntegrityViolationException.class);
	}
}
