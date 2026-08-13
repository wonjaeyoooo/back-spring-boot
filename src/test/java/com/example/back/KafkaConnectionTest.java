package com.example.back;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;

@SpringBootTest
class KafkaConnectionTest {

	private static final String TOPIC = "debug-topic";

	@Autowired
	KafkaTemplate<String, String> kafkaTemplate;

	@Autowired
	ConsumerFactory<String, String> consumerFactory;

	@Test
	void produceConsumeRoundTrip() throws Exception {
		String payload = "pong-" + UUID.randomUUID();
		kafkaTemplate.send(TOPIC, "debug", payload).get(10, TimeUnit.SECONDS);

		Map<String, Object> props = new HashMap<>(consumerFactory.getConfigurationProperties());
		props.put(ConsumerConfig.GROUP_ID_CONFIG, "debug-group-" + UUID.randomUUID());
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

		ConsumerRecord<String, String> record = null;
		try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
			consumer.subscribe(List.of(TOPIC));
			for (int i = 0; i < 20 && record == null; i++) {
				ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
				for (ConsumerRecord<String, String> candidate : records) {
					if (payload.equals(candidate.value())) {
						record = candidate;
						break;
					}
				}
			}
		}

		assertThat(record).isNotNull();
		assertThat(record.value()).isEqualTo(payload);
	}
}