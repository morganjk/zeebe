/*
 * Zeebe Broker Core
 * Copyright © 2017 camunda services GmbH (info@camunda.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.zeebe.broker.util;

import io.zeebe.broker.clustering.orchestration.topic.TopicRecord;
import io.zeebe.broker.incident.data.IncidentRecord;
import io.zeebe.broker.job.data.JobRecord;
import io.zeebe.broker.subscription.message.data.MessageSubscriptionRecord;
import io.zeebe.broker.subscription.message.data.WorkflowInstanceSubscriptionRecord;
import io.zeebe.broker.topic.Records;
import io.zeebe.broker.workflow.data.WorkflowInstanceRecord;
import io.zeebe.broker.workflow.deployment.data.DeploymentRecord;
import io.zeebe.logstreams.log.LoggedEvent;
import io.zeebe.protocol.intent.Intent;
import io.zeebe.protocol.intent.WorkflowInstanceIntent;
import io.zeebe.test.util.stream.StreamWrapper;
import io.zeebe.util.buffer.BufferUtil;
import java.util.stream.Stream;
import org.agrona.DirectBuffer;

public class RecordStream extends StreamWrapper<LoggedEvent> {

  public RecordStream(Stream<LoggedEvent> stream) {
    super(stream);
  }

  public RecordStream withIntent(Intent intent) {
    return new RecordStream(filter(r -> Records.hasIntent(r, intent)));
  }

  public TypedRecordStream<JobRecord> onlyJobRecords() {
    return new TypedRecordStream<>(
        filter(Records::isJobRecord).map(e -> CopiedTypedEvent.toTypedEvent(e, JobRecord.class)));
  }

  public TypedRecordStream<IncidentRecord> onlyIncidentRecords() {
    return new TypedRecordStream<>(
        filter(Records::isIncidentRecord)
            .map(e -> CopiedTypedEvent.toTypedEvent(e, IncidentRecord.class)));
  }

  public TypedRecordStream<DeploymentRecord> onlyDeploymentRecords() {
    return new TypedRecordStream<>(
        filter(Records::isDeploymentRecord)
            .map(e -> CopiedTypedEvent.toTypedEvent(e, DeploymentRecord.class)));
  }

  public TypedRecordStream<WorkflowInstanceRecord> onlyWorkflowInstanceRecords() {
    return new TypedRecordStream<>(
        filter(Records::isWorkflowInstanceRecord)
            .map(e -> CopiedTypedEvent.toTypedEvent(e, WorkflowInstanceRecord.class)));
  }

  public TypedRecordStream<MessageSubscriptionRecord> onlyMessageSubscriptionRecords() {
    return new TypedRecordStream<>(
        filter(Records::isMessageSubscriptionRecord)
            .map(e -> CopiedTypedEvent.toTypedEvent(e, MessageSubscriptionRecord.class)));
  }

  public TypedRecordStream<WorkflowInstanceSubscriptionRecord>
      onlyWorkflowInstanceSubscriptionRecords() {
    return new TypedRecordStream<>(
        filter(Records::isWorkflowInstanceSubscriptionRecord)
            .map(e -> CopiedTypedEvent.toTypedEvent(e, WorkflowInstanceSubscriptionRecord.class)));
  }

  public TypedRecordStream<TopicRecord> onlyTopicRecords() {
    return new TypedRecordStream<>(
        filter(Records::isTopicRecord)
            .map(e -> CopiedTypedEvent.toTypedEvent(e, TopicRecord.class)));
  }

  /**
   * This method makes only sense when the stream contains only entries of one workflow instance and
   * the element is only instantiated once within that instance.
   */
  public Stream<WorkflowInstanceIntent> onlyStatesOf(String elementId) {
    final DirectBuffer elementIdBuffer = BufferUtil.wrapString(elementId);

    return onlyWorkflowInstanceRecords()
        .onlyEvents()
        .filter(r -> elementIdBuffer.equals(r.getValue().getActivityId()))
        .map(r -> (WorkflowInstanceIntent) r.getMetadata().getIntent());
  }
}
