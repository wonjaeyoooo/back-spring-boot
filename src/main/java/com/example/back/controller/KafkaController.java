package com.example.back.controller;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api/kafka")
public class KafkaController {

	private final KafkaTemplate<String, String> kafkaTemplate;

	public KafkaController(KafkaTemplate<String, String> kafkaTemplate) {
		this.kafkaTemplate = kafkaTemplate;
	}

	/** topic에 메시지를 발행한다. 브로커가 수신 확인(ack)하면 partition/offset을 반환한다. */
	@PostMapping("/publish")
	public Map<String, Object> publish(@Valid @RequestBody PublishRequest request) throws Exception {
		SendResult<String, String> result = kafkaTemplate
				.send(request.topic(), request.key(), request.message())
				.get(10, TimeUnit.SECONDS);

		Map<String, Object> response = new HashMap<>();
		response.put("topic", result.getRecordMetadata().topic());
		response.put("key", request.key());
		response.put("partition", result.getRecordMetadata().partition());
		response.put("offset", result.getRecordMetadata().offset());
		response.put("timestamp", Instant.ofEpochMilli(result.getRecordMetadata().timestamp()).toString());
		return response;
	}

	public record PublishRequest(
			@NotBlank String topic,
			String key,
			@NotBlank String message) {
	}
}