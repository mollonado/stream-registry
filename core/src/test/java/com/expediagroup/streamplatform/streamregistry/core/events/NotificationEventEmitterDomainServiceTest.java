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

import static org.mockito.ArgumentMatchers.any;

import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.test.context.junit4.SpringRunner;

import com.expediagroup.streamplatform.streamregistry.core.handlers.HandlerService;
import com.expediagroup.streamplatform.streamregistry.core.services.DomainService;
import com.expediagroup.streamplatform.streamregistry.core.validators.DomainValidator;
import com.expediagroup.streamplatform.streamregistry.model.Domain;
import com.expediagroup.streamplatform.streamregistry.model.Specification;
import com.expediagroup.streamplatform.streamregistry.repository.DomainRepository;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestConfig.class)
public class NotificationEventEmitterDomainServiceTest {

  @MockBean
  private ApplicationEventMulticaster applicationEventMulticaster;

  @MockBean
  private HandlerService handlerService;

  @MockBean
  private DomainValidator domainValidator;

  @MockBean
  private DomainRepository domainRepository;

  private NotificationEventEmitter<Domain> domainServiceEventEmitter;
  private DomainService domainService;

  @Before
  public void before() {
    domainServiceEventEmitter = Mockito.spy(DefaultNotificationEventEmitter.<Domain>builder()
        .classType(Domain.class)
        .applicationEventMulticaster(applicationEventMulticaster)
        .build());

    domainService = Mockito.spy(new DomainService(handlerService, domainValidator, domainRepository, domainServiceEventEmitter));
  }

  @Test
  public void givenADomainForCreate_validateThatNotificationEventIsEmitted() {
    final Domain entity = new Domain();

    final EventType type = EventType.CREATE;
    final String source = domainServiceEventEmitter.getSourceEventPrefix(entity).concat(type.toString().toLowerCase());
    final NotificationEvent<Domain> event = getDummyNotificationEvent(source, type, entity);

    Mockito.when(domainRepository.findById(any())).thenReturn(Optional.empty());
    Mockito.doNothing().when(domainValidator).validateForCreate(entity);
    Mockito.when(handlerService.handleInsert(entity)).thenReturn(getDummySpecification());

    Mockito.when(domainRepository.save(any())).thenReturn(entity);
    Mockito.doNothing().when(applicationEventMulticaster).multicastEvent(event);

    domainService.create(entity);

    Mockito.verify(applicationEventMulticaster, Mockito.timeout(1000).times(1))
        .multicastEvent(event);

    Mockito.verify(domainServiceEventEmitter, Mockito.timeout(1000).times(0))
        .onFailedEmitting(any(), Mockito.eq(event));
  }

  @Test
  public void givenADomainForUpdate_validateThatNotificationEventIsEmitted() {
    final Domain entity = new Domain();

    final EventType type = EventType.UPDATE;
    final String source = domainServiceEventEmitter.getSourceEventPrefix(entity).concat(type.toString().toLowerCase());
    final NotificationEvent<Domain> event = getDummyNotificationEvent(source, type, entity);

    Mockito.when(domainRepository.findById(any())).thenReturn(Optional.of(entity));
    Mockito.doNothing().when(domainValidator).validateForUpdate(Mockito.eq(entity), any());
    Mockito.when(handlerService.handleUpdate(Mockito.eq(entity), any())).thenReturn(getDummySpecification());

    Mockito.when(domainRepository.save(any())).thenReturn(entity);
    Mockito.doNothing().when(applicationEventMulticaster).multicastEvent(event);

    domainService.update(entity);

    Mockito.verify(applicationEventMulticaster, Mockito.timeout(1000).times(1))
        .multicastEvent(event);

    Mockito.verify(domainServiceEventEmitter, Mockito.timeout(1000).times(0))
        .onFailedEmitting(any(), Mockito.eq(event));
  }

  @Test
  public void givenANullDomainRetrievedByRepositoryForCreate_validateThatNotificationEventIsNotEmitted() {
    final Domain entity = new Domain();

    Mockito.when(domainRepository.findById(any())).thenReturn(Optional.empty());
    Mockito.doNothing().when(domainValidator).validateForCreate(entity);
    Mockito.when(handlerService.handleInsert(entity)).thenReturn(getDummySpecification());

    Mockito.when(domainRepository.save(any())).thenReturn(null);
    Mockito.doNothing().when(applicationEventMulticaster).multicastEvent(any());

    domainService.create(entity);

    Mockito.verify(applicationEventMulticaster, Mockito.timeout(1000).times(0))
        .multicastEvent(any());

    Mockito.verify(domainServiceEventEmitter, Mockito.timeout(1000).times(0))
        .onFailedEmitting(any(), any());
  }

  @Test
  public void givenANullDomainRetrievedByRepositoryForUpdate_validateThatNotificationEventIsNotEmitted() {
    final Domain entity = new Domain();

    Mockito.when(domainRepository.findById(any())).thenReturn(Optional.of(entity));
    Mockito.doNothing().when(domainValidator).validateForUpdate(Mockito.eq(entity), any());
    Mockito.when(handlerService.handleUpdate(Mockito.eq(entity), any())).thenReturn(getDummySpecification());

    Mockito.when(domainRepository.save(any())).thenReturn(null);
    Mockito.doNothing().when(applicationEventMulticaster).multicastEvent(any());

    domainService.update(entity);

    Mockito.verify(applicationEventMulticaster, Mockito.timeout(1000).times(0))
        .multicastEvent(any());

    Mockito.verify(domainServiceEventEmitter, Mockito.timeout(1000).times(0))
        .onFailedEmitting(any(), any());
  }

  @Test
  public void givenADomainForUpsert_validateThatNotificationEventIsEmitted() {
    final Domain entity = new Domain();

    final EventType type = EventType.UPDATE;
    final String source = domainServiceEventEmitter.getSourceEventPrefix(entity).concat(type.toString().toLowerCase());
    final NotificationEvent<Domain> event = getDummyNotificationEvent(source, type, entity);

    Mockito.when(domainRepository.findById(any())).thenReturn(Optional.of(entity));
    Mockito.doNothing().when(domainValidator).validateForUpdate(Mockito.eq(entity), any());
    Mockito.when(handlerService.handleUpdate(Mockito.eq(entity), any())).thenReturn(getDummySpecification());

    Mockito.when(domainRepository.save(any())).thenReturn(entity);
    Mockito.doNothing().when(applicationEventMulticaster).multicastEvent(event);

    domainService.upsert(entity);

    Mockito.verify(applicationEventMulticaster, Mockito.timeout(1000).times(1))
        .multicastEvent(event);

    Mockito.verify(domainServiceEventEmitter, Mockito.timeout(1000).times(0))
        .onFailedEmitting(any(), Mockito.eq(event));
  }

  @Test
  public void givenADomainForCreate_handleAMulticasterException() {
    final Domain entity = new Domain();

    final EventType type = EventType.CREATE;
    final String source = domainServiceEventEmitter.getSourceEventPrefix(entity).concat(type.toString().toLowerCase());
    final NotificationEvent<Domain> event = getDummyNotificationEvent(source, type, entity);

    Mockito.when(domainRepository.findById(any())).thenReturn(Optional.empty());
    Mockito.doNothing().when(domainValidator).validateForCreate(entity);
    Mockito.when(handlerService.handleInsert(entity)).thenReturn(getDummySpecification());

    Mockito.when(domainRepository.save(any())).thenReturn(entity);
    Mockito.doThrow(new RuntimeException("BOOOOOOOM")).when(applicationEventMulticaster).multicastEvent(event);

    Optional<Domain> response = domainService.create(entity);

    Mockito.verify(applicationEventMulticaster, Mockito.timeout(1000).times(1))
        .multicastEvent(event);

    Mockito.verify(domainServiceEventEmitter, Mockito.timeout(1000).times(1))
        .onFailedEmitting(any(), Mockito.eq(event));

    Assert.assertTrue(response.isPresent());
    Assert.assertEquals(response.get(), entity);
  }

  public <T> NotificationEvent<T> getDummyNotificationEvent(String source, EventType type, T entity) {
    return NotificationEvent.<T>builder()
        .source(source)
        .eventType(type)
        .entity(entity)
        .build();
  }

  private final ObjectMapper mapper = new ObjectMapper();

  public Specification getDummySpecification() {
    Specification spec = new Specification();
    spec.setConfiguration(mapper.createObjectNode());
    spec.setDescription("dummy spec");
    return spec;
  }
}
