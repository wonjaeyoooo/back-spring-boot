package com.example.back;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.redis.test.autoconfigure.DataRedisTest;
import org.springframework.data.redis.core.StringRedisTemplate;

@DataRedisTest
class RedisConnectionTest {

	@Autowired
	StringRedisTemplate redisTemplate;

	@Test
	void pingReturnsPong() {
		assertThat(redisTemplate.getConnectionFactory().getConnection().ping()).isEqualTo("PONG");
	}

	@Test
	void setGetRoundTrip() {
		String key = "debug:key";
		redisTemplate.opsForValue().set(key, "pong");
		assertThat(redisTemplate.opsForValue().get(key)).isEqualTo("pong");
		redisTemplate.delete(key);
	}
}