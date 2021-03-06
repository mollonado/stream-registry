/**
 * Copyright (C) 2018-2019 Expedia, Inc.
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
package com.expediagroup.streamplatform.streamregistry.graphql.query.impl;

import java.util.Optional;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import com.expediagroup.streamplatform.streamregistry.core.services.ConsumerService;
import com.expediagroup.streamplatform.streamregistry.graphql.filters.ConsumerFilter;
import com.expediagroup.streamplatform.streamregistry.graphql.model.inputs.ConsumerKeyInput;
import com.expediagroup.streamplatform.streamregistry.graphql.model.queries.ConsumerKeyQuery;
import com.expediagroup.streamplatform.streamregistry.graphql.model.queries.SpecificationQuery;
import com.expediagroup.streamplatform.streamregistry.graphql.query.ConsumerQuery;
import com.expediagroup.streamplatform.streamregistry.model.Consumer;

@Component
@RequiredArgsConstructor
public class ConsumerQueryImpl implements ConsumerQuery {
  private final ConsumerService consumerService;

  @Override
  public Optional<Consumer> byKey(ConsumerKeyInput key) {
    return consumerService.read(key.asConsumerKey());
  }

  @Override
  public Iterable<Consumer> byQuery(ConsumerKeyQuery key, SpecificationQuery specification) {
    return consumerService.findAll(new ConsumerFilter(key, specification));
  }
}
