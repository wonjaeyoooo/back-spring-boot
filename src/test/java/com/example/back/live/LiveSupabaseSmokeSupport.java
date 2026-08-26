package com.example.back.live;

import java.util.Map;
import java.util.UUID;

/**
 * 라이브 Supabase 스모크 공통 헬퍼.
 * 각 테스트 클래스는 @EnabledIfEnvironmentVariable로 4개 자격증명 변수를 게이트로 사용하며,
 * 변수가 없으면 해당 클래스 전체가 SKIP된다. 실제 키는 환경변수로만 주입한다(커밋 금지).
 */
public abstract class LiveSupabaseSmokeSupport {

	protected static String uniqueEmail() {
		return "smoke-" + UUID.randomUUID() + "@example.com";
	}

	protected static Map<String, String> credentials(String email, String password) {
		return Map.of("email", email, "password", password);
	}

	public static final String PASSWORD = "smoke-secret-123";
}
