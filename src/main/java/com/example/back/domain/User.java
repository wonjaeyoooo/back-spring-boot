package com.example.back.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Supabase 사용자와 매핑되는 로컬 users 테이블 엔티티.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users")
@Entity
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// Supabase auth.users의 UUID — Hibernate가 네이티브 uuid 타입으로 매핑
	@Column(name = "supabase_user_id", nullable = false, unique = true)
	private UUID supabaseUserId;

	@Column(nullable = false, unique = true)
	private String email;

	@Column(length = 50)
	private String nickname;

	// 타임스탬프 애노테이션 누락 시 Hibernate가 NULL을 명시 전송해 NOT NULL 위반이 발생하므로 필수
	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	public User(UUID supabaseUserId, String email, String nickname) {
		this.supabaseUserId = supabaseUserId;
		this.email = email;
		this.nickname = nickname;
	}

	public void updateEmail(String email) {
		this.email = email;
	}
}
