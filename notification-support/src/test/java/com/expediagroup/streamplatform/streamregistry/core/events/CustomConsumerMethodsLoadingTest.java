/**
 * Copyright (C) 2018-2020 Expedia, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.expediagroup.streamplatform.streamregistry.core.events;

import static com.expediagroup.streamplatform.streamregistry.core.events.NotificationEventUtils.getWarningMessageOnNotDefinedProp;
import static com.expediagroup.streamplatform.streamregistry.core.events.ObjectNodeMapper.deserialise;
import static com.expediagroup.streamplatform.streamregistry.core.events.ObjectNodeMapper.serialise;
import static com.expediagroup.streamplatform.streamregistry.core.events.config.NotificationEventConfig.CUSTOM_CONSUMER_KEY_PARSER_CLASS_PROPERTY;
import static com.expediagroup.streamplatform.streamregistry.core.events.config.NotificationEventConfig.CUSTOM_CONSUMER_KEY_PARSER_METHOD_PROPERTY;
import static com.expediagroup.streamplatform.streamregistry.core.events.config.NotificationEventConfig.CUSTOM_CONSUMER_PARSER_ENABLED_PROPERTY;
import static com.expediagroup.streamplatform.streamregistry.core.events.config.NotificationEventConfig.CUSTOM_CONSUMER_VALUE_PARSER_CLASS_PROPERTY;
import static com.expediagroup.streamplatform.streamregistry.core.events.config.NotificationEventConfig.CUSTOM_CONSUMER_VALUE_PARSER_METHOD_PROPERTY;
import static com.expediagroup.streamplatform.streamregistry.core.events.config.NotificationEventConfig.KAFKA_BOOTSTRAP_SERVERS_PROPERTY;
import static com.expediagroup.streamplatform.streamregistry.core.events.config.NotificationEventConfig.KAFKA_NOTIFICATIONS_ENABLED_PROPERTY;
import static com.expediagroup.streamplatform.streamregistry.core.events.config.NotificationEventConfig.KAFKA_SCHEMA_REGISTRY_URL_PROPERTY;
import static com.expediagroup.streamplatform.streamregistry.core.events.config.NotificationEventConfig.KAFKA_TOPIC_NAME_PROPERTY;
import static com.expediagroup.streamplatform.streamregistry.core.events.config.NotificationEventConfig.KAFKA_TOPIC_SETUP_PROPERTY;

import java.time.Instant;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import lombok.val;

import org.apache.avro.specific.SpecificRecord;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.test.context.junit4.SpringRunner;

import com.expediagroup.streamplatform.streamregistry.avro.AvroConsumer;
import com.expediagroup.streamplatform.streamregistry.avro.AvroEvent;
import com.expediagroup.streamplatform.streamregistry.avro.AvroKey;
import com.expediagroup.streamplatform.streamregistry.avro.AvroKeyType;
import com.expediagroup.streamplatform.streamregistry.core.events.config.NewTopicProperties;
import com.expediagroup.streamplatform.streamregistry.core.events.config.NotificationEventConfig;
import com.expediagroup.streamplatform.streamregistry.core.events.handlers.ConsumerEventHandlerForKafka;
import com.expediagroup.streamplatform.streamregistry.model.Consumer;
import com.expediagroup.streamplatform.streamregistry.model.Specification;
import com.expediagroup.streamplatform.streamregistry.model.Status;
import com.expediagroup.streamplatform.streamregistry.model.Tag;
import com.expediagroup.streamplatform.streamregistry.model.keys.ConsumerKey;

@RunWith(SpringRunner.class)// Explicitly defined prop with true as value
@SpringBootTest(classes = CustomConsumerMethodsLoadingTest.MockListenerConfiguration.class,
    properties = {
        KAFKA_NOTIFICATIONS_ENABLED_PROPERTY + "=true",
        KAFKA_TOPIC_NAME_PROPERTY + "=my-topic",
        KAFKA_TOPIC_SETUP_PROPERTY + "=false", // We don't test setup topic here but in  the integration test
        KAFKA_BOOTSTRAP_SERVERS_PROPERTY + "=localhost:9092",
        KAFKA_SCHEMA_REGISTRY_URL_PROPERTY + "=foo:8081",
        CUSTOM_CONSUMER_PARSER_ENABLED_PROPERTY + "=true",
        CUSTOM_CONSUMER_KEY_PARSER_CLASS_PROPERTY + "=com.expediagroup.streamplatform.streamregistry.core.events.CustomConsumerMethodsLoadingTest",
        CUSTOM_CONSUMER_KEY_PARSER_METHOD_PROPERTY + "=myCustomKey",
        CUSTOM_CONSUMER_VALUE_PARSER_CLASS_PROPERTY + "=com.expediagroup.streamplatform.streamregistry.core.events.CustomConsumerMethodsLoadingTest",
        CUSTOM_CONSUMER_VALUE_PARSER_METHOD_PROPERTY + "=myCustomEvent"
    })
public class CustomConsumerMethodsLoadingTest {
  private static final AtomicReference<AvroKey> testAvroKeyResult = new AtomicReference<>();
  private static final AtomicReference<AvroEvent> testAvroEventResult = new AtomicReference<>();

  @Autowired
  private ConsumerEventHandlerForKafka consumerEventHandlerForKafka;

  @Before
  public void before() {
    testAvroKeyResult.set(null);
    testAvroEventResult.set(null);
  }

  @Test
  public void having_defined_custom_parser_methods_verify_they_execute_properly() {
    val avrokey = (AvroKey) consumerEventHandlerForKafka.getConsumerToKeyRecord().apply(getDummyConsumer());
    val avroEvent = (AvroEvent) consumerEventHandlerForKafka.getConsumerToValueRecord().apply(getDummyConsumer());

    Assert.assertNotNull("Avro key shouldn't be null", avrokey);
    Assert.assertNotNull("Avro event shouldn't be null", avroEvent);
    Assert.assertNotNull("Consumer entity shouldn't be null", avroEvent.getConsumerEntity());

    Assert.assertEquals(avrokey, testAvroKeyResult.get());
    Assert.assertEquals(avroEvent, testAvroEventResult.get());
  }

  public static AvroKey myCustomKey(Consumer consumer) {
    val name = consumer.getKey().getName();
    val streamName = consumer.getKey().getStreamName();
    val version = consumer.getKey().getStreamVersion();
    val domainName = consumer.getKey().getStreamDomain();
    val zone = consumer.getKey().getZone();

    var domainKey = AvroKey.newBuilder()
        .setId(domainName)
        .setType(AvroKeyType.DOMAIN)
        .build();

    var zoneKey = AvroKey.newBuilder()
        .setId(zone)
        .setParent(domainKey)
        .setType(AvroKeyType.ZONE)
        .build();

    var streamKey = AvroKey.newBuilder()
        .setId(streamName)
        .setParent(zoneKey)
        .setType(AvroKeyType.STREAM)
        .build();

    var streamVersionKey = AvroKey.newBuilder()
        .setId(version.toString())
        .setParent(streamKey)
        .setType(AvroKeyType.STREAM)
        .build();

    AvroKey avroKey = AvroKey.newBuilder()
        .setId(name)
        .setParent(streamVersionKey)
        .setType(AvroKeyType.CONSUMER)
        .build();

    testAvroKeyResult.set(avroKey);

    return avroKey;
  }

  public static AvroEvent myCustomEvent(Consumer consumer) {

    val avroEvent = AvroConsumer.newBuilder()
        .setName(consumer.getKey().getName())
        .setStreamDomain(consumer.getKey().getStreamDomain())
        .setStreamVersion(consumer.getKey().getStreamVersion())
        .setStreamName(consumer.getKey().getStreamName())
        .setZone(consumer.getKey().getZone())
        .setDescription(consumer.getSpecification().getDescription())
        .setTags(Collections.emptyList())
        .setType(consumer.getSpecification().getType())
        .setConfigurationString(serialise(consumer.getSpecification().getConfiguration()))
        .setStatusString(serialise(consumer.getStatus().getObjectNode()))
        .build();

    val event = AvroEvent.newBuilder().setConsumerEntity(avroEvent).build();

    testAvroEventResult.set(event);

    return event;
  }

  public static Consumer getDummyConsumer() {
    val name = Instant.now().toString();
    val domain = "domain";
    val description = "description";
    val type = "type";
    val configJson = "{}";
    String statusJson = "{\"foo\":\"bar\"}";
    val tags = Collections.singletonList(new Tag("tag-name", "tag-value"));
    val version = 1;
    val zone = "aws_us_east_1";
    val streamName = "stream01";

    // Key
    val key = new ConsumerKey();
    key.setName(name);
    key.setStreamName(streamName);
    key.setStreamDomain(domain);
    key.setStreamVersion(version);
    key.setZone(zone);

    // Spec
    val spec = new Specification();
    spec.setDescription(description);
    spec.setType(type);
    spec.setConfiguration(deserialise(configJson));
    spec.setTags(tags);

    // Status
    val status = new Status(deserialise(statusJson));

    val consumer = new Consumer();
    consumer.setKey(key);
    consumer.setSpecification(spec);
    consumer.setStatus(status);

    return consumer;
  }

  @Configuration
  public static class MockListenerConfiguration extends NotificationEventConfig {
    @Value("${" + KAFKA_TOPIC_NAME_PROPERTY + ":#{null}}")
    private String notificationEventsTopic;

    @Value("${" + KAFKA_BOOTSTRAP_SERVERS_PROPERTY + ":#{null}}")
    private String bootstrapServers;

    @Bean(initMethod = "setup")
    @ConditionalOnProperty(name = KAFKA_NOTIFICATIONS_ENABLED_PROPERTY)
    public KafkaSetupHandler kafkaSetupHandler(NewTopicProperties newTopicProperties) {
      Objects.requireNonNull(notificationEventsTopic, getWarningMessageOnNotDefinedProp("enabled notification events", KAFKA_TOPIC_NAME_PROPERTY));
      Objects.requireNonNull(bootstrapServers, getWarningMessageOnNotDefinedProp("enabled notification events", KAFKA_BOOTSTRAP_SERVERS_PROPERTY));

      return Mockito.mock(KafkaSetupHandler.class);
    }

    @Bean(name = "consumerFactory")
    @ConditionalOnProperty(name = KAFKA_NOTIFICATIONS_ENABLED_PROPERTY)
    public ProducerFactory<SpecificRecord, SpecificRecord> consumerFactory() {
      return Mockito.mock(ProducerFactory.class);
    }

    @Bean(name = "kafkaTemplate")
    @ConditionalOnProperty(name = KAFKA_NOTIFICATIONS_ENABLED_PROPERTY)
    public KafkaTemplate<SpecificRecord, SpecificRecord> kafkaTemplate() {
      return Mockito.mock(KafkaTemplate.class);
    }
  }
}