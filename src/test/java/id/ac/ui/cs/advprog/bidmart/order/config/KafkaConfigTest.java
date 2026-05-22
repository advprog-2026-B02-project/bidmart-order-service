package id.ac.ui.cs.advprog.bidmart.order.config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class KafkaConfigTest {

    @Test
    void kafkaBeansUseConfiguredProperties() {
        KafkaConfig config = new KafkaConfig();
        ReflectionTestUtils.setField(config, "bootstrapServers", "kafka:9092");
        ReflectionTestUtils.setField(config, "createTopics", true);
        ReflectionTestUtils.setField(config, "notificationTopic", "notifications");

        KafkaAdmin admin = config.kafkaAdmin();
        assertEquals("kafka:9092", admin.getConfigurationProperties().get(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG));

        var producerFactory = config.producerFactory();
        assertInstanceOf(DefaultKafkaProducerFactory.class, producerFactory);
        @SuppressWarnings("unchecked")
        Map<String, Object> producerProps =
                ((DefaultKafkaProducerFactory<String, String>) producerFactory).getConfigurationProperties();
        assertEquals("kafka:9092", producerProps.get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG));
        assertEquals(StringSerializer.class, producerProps.get(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG));
        assertEquals(StringSerializer.class, producerProps.get(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG));

        KafkaTemplate<String, String> template = config.kafkaTemplate();
        assertNotNull(template);

        NewTopic topic = config.notificationRequestsTopic();
        assertEquals("notifications", topic.name());
        assertEquals(1, topic.numPartitions());
        assertEquals(1, topic.replicationFactor());
    }

    @Test
    void constantHasDefaultNotificationTopic() {
        assertFalse(KafkaConfig.TOPIC_NOTIFICATION_REQUESTS.isBlank());
    }
}
