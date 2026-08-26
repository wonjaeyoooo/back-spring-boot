package com.example.back.support;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;

/**
 * Supabase 없이 JWT 검증을 시험하기 위한 테스트 전용 JWKS 서버.
 * JDK 내장 HttpServer로 로컬 생성 P-256 EC 키와 RSA 키의 공개 JWK를 서빙하고,
 * 같은 개인키로 각종 JWT를 발급하는 헬퍼를 제공한다.
 */
public class JwksTestServer implements AutoCloseable {

	public static final String ISSUER = "https://ittest.supabase.co/auth/v1";

	private static volatile JwksTestServer shared;

	private final HttpServer server;
	private final ECKey ecKey;
	private final RSAKey rsaKey;

	private JwksTestServer() throws Exception {
		KeyPairGenerator ecGenerator = KeyPairGenerator.getInstance("EC");
		ecGenerator.initialize(new ECGenParameterSpec("secp256r1"));
		KeyPair ecPair = ecGenerator.generateKeyPair();
		this.ecKey = new ECKey.Builder(Curve.P_256, (ECPublicKey) ecPair.getPublic())
			.privateKey((ECPrivateKey) ecPair.getPrivate())
			.keyID("test-es256")
			.build();

		KeyPairGenerator rsaGenerator = KeyPairGenerator.getInstance("RSA");
		rsaGenerator.initialize(2048);
		KeyPair rsaPair = rsaGenerator.generateKeyPair();
		this.rsaKey = new RSAKey.Builder((RSAPublicKey) rsaPair.getPublic())
			.privateKey((RSAPrivateKey) rsaPair.getPrivate())
			.keyID("test-rs256")
			.build();

		String jwksJson = "{\"keys\":["
			+ ecKey.toPublicJWK().toJSONString() + ","
			+ rsaKey.toPublicJWK().toJSONString() + "]}";
		byte[] body = jwksJson.getBytes(StandardCharsets.UTF_8);

		this.server = HttpServer.create(new InetSocketAddress(0), 0);
		server.createContext("/auth/v1/.well-known/jwks.json", exchange -> {
			exchange.getResponseHeaders().set("Content-Type", "application/json");
			exchange.sendResponseHeaders(200, body.length);
			try (OutputStream out = exchange.getResponseBody()) {
				out.write(body);
			}
		});
		server.start();
		Runtime.getRuntime().addShutdownHook(new Thread(this::stopQuietly));
	}

	/** 컨텍스트 캐시가 단일 인스턴스를 재사용하도록 모든 디코더 테스트가 하나의 서버를 공유한다 */
	public static synchronized JwksTestServer shared() throws Exception {
		if (shared == null) {
			shared = new JwksTestServer();
		}
		return shared;
	}

	public String jwksUri() {
		return "http://localhost:" + server.getAddress().getPort() + "/auth/v1/.well-known/jwks.json";
	}

	public String es256Token(String subject, String email, Instant issuedAt, Instant expiresAt)
		throws JOSEException {
		return sign(JWSAlgorithm.ES256,
			claims(subject, email, issuedAt, expiresAt, ISSUER),
			signedJwt -> signedJwt.sign(new ECDSASigner(ecKey.toECPrivateKey())));
	}

	public String rs256Token(String subject, String email, Instant issuedAt, Instant expiresAt)
		throws JOSEException {
		return sign(JWSAlgorithm.RS256,
			claims(subject, email, issuedAt, expiresAt, ISSUER),
			signedJwt -> signedJwt.sign(new RSASSASigner(rsaKey.toRSAPrivateKey())));
	}

	public String es256TokenWithIssuer(String subject, String email, Instant issuedAt, Instant expiresAt,
		String issuer) throws JOSEException {
		return sign(JWSAlgorithm.ES256,
			claims(subject, email, issuedAt, expiresAt, issuer),
			signedJwt -> signedJwt.sign(new ECDSASigner(ecKey.toECPrivateKey())));
	}

	/** JWKS에 없는 다른 EC 키로 서명해 잘못된 서명 케이스를 만든다 */
	public String es256TokenSignedByForeignIssuer(String subject, Instant issuedAt, Instant expiresAt)
		throws Exception {
		KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
		generator.initialize(new ECGenParameterSpec("secp256r1"));
		KeyPair pair = generator.generateKeyPair();
		ECKey foreignKey = new ECKey.Builder(Curve.P_256, (ECPublicKey) pair.getPublic())
			.privateKey((ECPrivateKey) pair.getPrivate())
			.keyID("foreign-key")
			.build();
		return sign(JWSAlgorithm.ES256,
			claims(subject, "foreign@example.com", issuedAt, expiresAt, ISSUER),
			signedJwt -> signedJwt.sign(new ECDSASigner(foreignKey.toECPrivateKey())));
	}

	public String hs256Token(String secret, String subject, Instant issuedAt, Instant expiresAt)
		throws JOSEException {
		MACSigner macSigner = new MACSigner(secret);
		return sign(JWSAlgorithm.HS256,
			claims(subject, "hs@example.com", issuedAt, expiresAt, ISSUER),
			signedJwt -> signedJwt.sign(macSigner));
	}

	public String unsignedToken(String subject, Instant issuedAt, Instant expiresAt) {
		return new PlainJWT(claims(subject, "none@example.com", issuedAt, expiresAt, ISSUER)).serialize();
	}

	private interface JwtSigner {
		void apply(SignedJWT signedJwt) throws JOSEException;
	}

	private String sign(JWSAlgorithm algorithm, JWTClaimsSet claims, JwtSigner jwtSigner) throws JOSEException {
		SignedJWT signedJwt = new SignedJWT(
			new JWSHeader.Builder(algorithm).type(JOSEObjectType.JWT).build(),
			claims);
		jwtSigner.apply(signedJwt);
		return signedJwt.serialize();
	}

	private JWTClaimsSet claims(String subject, String email, Instant issuedAt, Instant expiresAt, String issuer) {
		return new JWTClaimsSet.Builder()
			.subject(subject)
			.jwtID(UUID.randomUUID().toString())
			.issuer(issuer != null ? issuer : ISSUER)
			.audience("authenticated")
			.issueTime(Date.from(issuedAt))
			.expirationTime(Date.from(expiresAt))
			.claim("email", email)
			.claim("role", "authenticated")
			.build();
	}

	private void stopQuietly() {
		server.stop(0);
	}

	@Override
	public void close() {
		stopQuietly();
	}
}
