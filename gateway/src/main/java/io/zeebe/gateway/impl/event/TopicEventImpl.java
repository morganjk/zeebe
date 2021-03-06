/*
 * Copyright © 2017 camunda services GmbH (info@camunda.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.zeebe.gateway.impl.event;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.zeebe.gateway.api.events.TopicEvent;
import io.zeebe.gateway.api.events.TopicState;
import io.zeebe.gateway.impl.data.ZeebeObjectMapperImpl;
import io.zeebe.gateway.impl.record.TopicRecordImpl;
import io.zeebe.protocol.clientapi.RecordType;
import java.util.ArrayList;
import java.util.List;

public class TopicEventImpl extends TopicRecordImpl implements TopicEvent {
  private final List<Integer> partitionIds = new ArrayList<>();

  @JsonCreator
  public TopicEventImpl(@JacksonInject ZeebeObjectMapperImpl objectMapper) {
    super(objectMapper, RecordType.EVENT);
  }

  @Override
  public List<Integer> getPartitionIds() {
    return partitionIds;
  }

  @JsonIgnore
  @Override
  public TopicState getState() {
    return TopicState.valueOf(getMetadata().getIntent());
  }

  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder();
    builder.append("TopicEvent [state=");
    builder.append(getState());
    builder.append(", name=");
    builder.append(getName());
    builder.append(", partitionIds=");
    builder.append(partitionIds);
    builder.append("]");
    return builder.toString();
  }
}
