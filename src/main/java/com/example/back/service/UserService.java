package com.example.back.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.back.domain.User;
import com.example.back.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * JWT의 사용자 정보와 로컬 users 테이블을 동기화한다.
 */
@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepository userRepository;

	/**
	 * 같은 UUID에 대해 중복 행을 만들지 않는다.
	 * 이미 있으면 이메일 변경만 반영하고, 없으면 새로 저장한다.
	 */
	@Transactional
	public User syncUser(UUID supabaseUserId, String email, String nickname) {
		return userRepository.findBySupabaseUserId(supabaseUserId)
			.map(user -> {
				user.updateEmail(email);
				return user;
			})
			.orElseGet(() -> userRepository.save(new User(supabaseUserId, email, nickname)));
	}

	@Transactional(readOnly = true)
	public Optional<User> findBySupabaseUserId(UUID supabaseUserId) {
		return userRepository.findBySupabaseUserId(supabaseUserId);
	}
}
