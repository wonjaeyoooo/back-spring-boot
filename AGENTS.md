# AGENTS.md

## 스택

- Spring Boot 4.1.0 (부모 POM), Java 21, Maven wrapper 3.9.16, 단일 모듈. 패키지 루트: `com.example.back`.
- 영속성: Spring Data JPA **및** MyBatis (`mybatis-spring-boot-starter` 4.1.0) 병행 사용.
- PostgreSQL 17 (로컬은 docker-compose 사용), Lombok, springdoc-openapi 3.1.0, Actuator.

## 명령어

- 실행: `./mvnw spring-boot:run`
- 전체 테스트: `./mvnw test` · 단일 테스트: `./mvnw test -Dtest=<클래스명>`
- 패키징: `./mvnw package`

## 로컬 개발 & DB

- 기본 프로파일은 `local` (`application.yml`에 하드코딩됨; `--spring.profiles.active=...`로 재정의 가능).
- `local` 프로파일은 PostgreSQL이 `localhost:5432`에 있어야 함 (DB `back_spring_boot`, 사용자 `back` / `back1234`). `docker compose up -d`로 시작 — compose 파일이 이 자격 증명과 일치함.
- **Postgres가 내려가 있으면 `@SpringBootTest` 컨텍스트 테스트가 실패함**: JPA/MyBatis 자동 설정이 라이브 연결을 필요로 하기 때문. 테스트 실행 전에 DB부터 시작할 것.
- **마이그레이션은 Flyway로 관리**: SQL 파일은 `src/main/resources/db/migration/V<버전>__<설명>.sql`에 작성 (예: `V1__init.sql`). 엔티티 추가/변경 시 대응 마이그레이션 작성 필수 — JPA `ddl-auto: validate`로 설정되어 있어 자동 생성되지 않음. `spring.flyway.baseline-on-migrate: true`라 히스토리 없는 기존 스키마는 자동 baseline 처리됨. `prod` 프로파일은 주석에만 언급되어 있고 정의되어 있지 않음.

## 컨벤션 & 주의사항

- **Spring Boot 4는 스타터 이름과 자동 구성 모듈이 분리됨**: 웹 스타터는 `spring-boot-starter-webmvc`, 테스트 스타터는 분리됨 (예: `spring-boot-starter-webmvc-test`). 통합 도구의 자동 구성도 별도 모듈로 분리 — Flyway는 `spring-boot-flyway` 모듈을 추가해야 자동 구성이 동작함 (`flyway-core`만 추가하면 실행되지 않음). 의존성 추가 시 `pom.xml`에 이미 있는 이름을 따를 것 — 예전 `spring-boot-starter-web` / `spring-boot-starter-test` 이름은 여기서 해석되지 않음.
- **MyBatis 매퍼**: XML 파일은 `src/main/resources/mapper/**/*.xml`에 위치 (`mybatis.mapper-locations`); 타입 별칭은 `com.example.back.domain`; `map-underscore-to-camel-case: true` 켜져 있음. 기능 추가 시 한 가지 영속성 스타일(JPA 엔티티 또는 MyBatis 매퍼)을 선택해서 그 기능 안에서는 섞지 말고 일관성 유지할 것.
- **Lombok**은 `maven-compiler-plugin`에 애노테이션 프로세서로 연결되어 있음; `@Getter`/`@Builder` 등의 사용이 기대됨.
- **OpenAPI**: Swagger UI는 `/swagger-ui.html`, 스펙은 `/v3/api-docs`. OpenAPI 설정은 `com.example.back.config.OpenApiConfig`에 있음.
- **Actuator**: `health, info`만 노출됨 (`/actuator/health`, `/actuator/info`) — 그 외 엔드포인트는 404.
- **스타일**: 들여쓰기는 탭, 포맷터/Spotless 설정 없음 — 주변 코드를 따를 것. 설정 파일의 주석은 한국어로 작성됨 (`application.yml` 참고).
- 새 코드는 `com.example.back.<레이어>` 하위에 작성 (현재 controller/config 존재; service/repository/mapper/domain이 이어짐).
- `.omo/` (에이전트 상태)와 `target/` (빌드 산출물)은 gitignore 처리됨 — 커밋하거나 수정하지 말 것.
