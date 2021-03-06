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
package io.zeebe.gateway.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.zeebe.gateway.ZeebeClient;
import io.zeebe.gateway.api.events.JobEvent;
import io.zeebe.gateway.api.events.WorkflowInstanceEvent;
import io.zeebe.gateway.api.record.ZeebeObjectMapper;
import io.zeebe.gateway.cmd.ClientException;
import io.zeebe.gateway.impl.data.ZeebeObjectMapperImpl;
import io.zeebe.gateway.impl.event.JobEventImpl;
import io.zeebe.gateway.impl.event.WorkflowInstanceEventImpl;
import io.zeebe.gateway.util.Events;
import io.zeebe.test.util.AutoCloseableRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ZeebeObjectMapperTest {

  @Rule public AutoCloseableRule closeables = new AutoCloseableRule();

  private ZeebeObjectMapper objectMapper;

  @Before
  public void init() {
    final ZeebeClient client = ZeebeClient.newClient();
    closeables.manage(client);

    this.objectMapper = client.objectMapper();
  }

  @Test
  public void shouldSerializeRecordToJson() {
    // given
    final ZeebeObjectMapperImpl mockMapper = mock(ZeebeObjectMapperImpl.class);
    final JobEvent jobEvent = new JobEventImpl(mockMapper);

    // when
    jobEvent.toJson();

    // then
    verify(mockMapper).toJson(jobEvent);
  }

  @Test
  public void shouldSerializeJobRecordWithPayload() {
    // given
    final JobEventImpl jobEvent = Events.exampleJob();
    jobEvent.setPayload("{\"foo\":\"bar\"}");

    // when / then
    final String json = objectMapper.toJson(jobEvent);
    assertThat(json).contains("\"payload\":{\"foo\":\"bar\"}");

    final JobEvent deserializedRecord = objectMapper.fromJson(json, JobEvent.class);
    assertThat(deserializedRecord.getPayload()).isEqualTo("{\"foo\":\"bar\"}");
  }

  @Test
  public void shouldSerializeJobRecordWithoutPayload() {
    // given
    final JobEventImpl jobEvent = Events.exampleJob();
    jobEvent.setPayload((String) null);

    // when / then
    final String json = objectMapper.toJson(jobEvent);
    assertThat(json).contains("\"payload\":null");

    final JobEvent deserializedRecord = objectMapper.fromJson(json, JobEvent.class);
    assertThat(deserializedRecord.getPayload()).isNull();
  }

  @Test
  public void shouldSerializeWorkflowInstanceRecordWithPayload() {
    // given
    final WorkflowInstanceEventImpl wfInstanceEvent = Events.exampleWorfklowInstance();
    wfInstanceEvent.setPayload("{\"foo\":\"bar\"}");

    // when / then
    final String json = objectMapper.toJson(wfInstanceEvent);
    assertThat(json).contains("\"payload\":{\"foo\":\"bar\"}");

    final WorkflowInstanceEvent deserializedRecord =
        objectMapper.fromJson(json, WorkflowInstanceEvent.class);
    assertThat(deserializedRecord.getPayload()).isEqualTo("{\"foo\":\"bar\"}");
  }

  @Test
  public void shouldSerializeWorkflowInstanceRecordWithoutPayload() {
    // given
    final WorkflowInstanceEventImpl wfInstanceEvent = Events.exampleWorfklowInstance();
    wfInstanceEvent.setPayload((String) null);

    // when / then
    final String json = objectMapper.toJson(wfInstanceEvent);
    assertThat(json).contains("\"payload\":null");

    final WorkflowInstanceEvent deserializedRecord =
        objectMapper.fromJson(json, WorkflowInstanceEvent.class);
    assertThat(deserializedRecord.getPayload()).isNull();
  }

  @Test
  public void shouldThrowExceptionIfDeserializationFails() {
    assertThatThrownBy(() -> objectMapper.fromJson("invalid", JobEvent.class))
        .isInstanceOf(ClientException.class)
        .hasMessage(
            "Failed deserialize JSON 'invalid' to object of type 'io.zeebe.gateway.api.events.JobEvent'");
  }

  @Test
  public void shouldThrowExceptionIfDeserializeToWrongRecord() {
    final String json = Events.exampleJob().toJson();

    assertThatThrownBy(() -> objectMapper.fromJson(json, WorkflowInstanceEvent.class))
        .isInstanceOf(ClientException.class)
        .hasMessage(
            "Cannot deserialize JSON to object of type 'io.zeebe.gateway.api.events.WorkflowInstanceEvent'. Incompatible type for record 'EVENT - JOB'.");
  }

  @Test
  public void shouldThrowExceptionIfDeserializeToImplClass() {
    final String json = Events.exampleJob().toJson();

    assertThatThrownBy(() -> objectMapper.fromJson(json, JobEventImpl.class))
        .isInstanceOf(ClientException.class)
        .hasMessage(
            "Cannot deserialize JSON: unknown record class 'io.zeebe.gateway.impl.event.JobEventImpl'");
  }
}
