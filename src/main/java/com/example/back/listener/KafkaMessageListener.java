package com.example.back.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** debug-topic에 발행된 메시지를 소비해서 로그로 남긴다. */
@Component
public class KafkaMessageListener {

	private static final Logger log = LoggerFactory.getLogger(KafkaMessageListener.class);

	@KafkaListener(topics = "debug-topic", groupId = "back-spring-boot")
	public void listen(String message) {
		log.info("Kafka 메시지 수신: {}", message);
	}
}