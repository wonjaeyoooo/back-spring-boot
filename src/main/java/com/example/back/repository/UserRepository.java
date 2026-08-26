package com.example.back.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.back.domain.User;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findBySupabaseUserId(UUID supabaseUserId);

	Optional<User> findByEmail(String email);
}
