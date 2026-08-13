package com.example.back.controller;

import java.util.Map;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api/redis")
public class RedisController {

	private final StringRedisTemplate redisTemplate;

	public RedisController(StringRedisTemplate redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	/** key에 value를 저장한다. 이미 존재하는 key면 덮어쓴다. */
	@PutMapping("/{key}")
	public Map<String, String> set(@PathVariable String key, @Valid @RequestBody SetValueRequest request) {
		redisTemplate.opsForValue().set(key, request.value());
		return Map.of("key", key, "value", request.value());
	}

	/** key를 삭제한다. 삭제 대상이 없으면 deleted=false를 반환한다. */
	@DeleteMapping("/{key}")
	public Map<String, Object> delete(@PathVariable String key) {
		Boolean deleted = redisTemplate.delete(key);
		return Map.of("key", key, "deleted", deleted);
	}

	public record SetValueRequest(@NotBlank String value) {
	}
}