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
package io.zeebe.broker.topic;

import io.zeebe.broker.clustering.orchestration.topic.TopicRecord;
import io.zeebe.broker.job.data.JobRecord;
import io.zeebe.broker.workflow.deployment.data.DeploymentRecord;
import io.zeebe.logstreams.log.LoggedEvent;
import io.zeebe.msgpack.UnpackedObject;
import io.zeebe.protocol.clientapi.RecordType;
import io.zeebe.protocol.clientapi.ValueType;
import io.zeebe.protocol.impl.RecordMetadata;
import io.zeebe.protocol.intent.Intent;
import io.zeebe.util.ReflectUtil;
import io.zeebe.util.buffer.BufferUtil;
import org.agrona.DirectBuffer;

public class Records {

  public static DeploymentRecord asDeploymentRecord(LoggedEvent event) {
    return readValueAs(event, DeploymentRecord.class);
  }

  public static TopicRecord asTopicRecord(LoggedEvent event) {
    return readValueAs(event, TopicRecord.class);
  }

  public static JobRecord asJobRecord(LoggedEvent event) {
    return readValueAs(event, JobRecord.class);
  }

  protected static <T extends UnpackedObject> T readValueAs(
      LoggedEvent event, Class<T> valueClass) {
    final DirectBuffer copy =
        BufferUtil.cloneBuffer(
            event.getValueBuffer(), event.getValueOffset(), event.getValueLength());
    final T valuePojo = ReflectUtil.newInstance(valueClass);
    valuePojo.wrap(copy);
    return valuePojo;
  }

  public static boolean isDeploymentRecord(LoggedEvent event) {
    return isRecordOfType(event, ValueType.DEPLOYMENT);
  }

  public static boolean isTopicRecord(LoggedEvent event) {
    return isRecordOfType(event, ValueType.TOPIC);
  }

  public static boolean isJobRecord(LoggedEvent event) {
    return isRecordOfType(event, ValueType.JOB);
  }

  public static boolean isIncidentRecord(LoggedEvent event) {
    return isRecordOfType(event, ValueType.INCIDENT);
  }

  public static boolean isWorkflowInstanceRecord(LoggedEvent event) {
    return isRecordOfType(event, ValueType.WORKFLOW_INSTANCE);
  }

  public static boolean isMessageRecord(LoggedEvent event) {
    return isRecordOfType(event, ValueType.MESSAGE);
  }

  public static boolean isMessageSubscriptionRecord(LoggedEvent event) {
    return isRecordOfType(event, ValueType.MESSAGE_SUBSCRIPTION);
  }

  public static boolean isWorkflowInstanceSubscriptionRecord(LoggedEvent event) {
    return isRecordOfType(event, ValueType.WORKFLOW_INSTANCE_SUBSCRIPTION);
  }

  public static boolean hasIntent(LoggedEvent event, Intent intent) {
    if (event == null) {
      return false;
    }

    final RecordMetadata metadata = getMetadata(event);

    return metadata.getIntent() == intent;
  }

  private static RecordMetadata getMetadata(LoggedEvent event) {
    final RecordMetadata metadata = new RecordMetadata();
    event.readMetadata(metadata);

    return metadata;
  }

  public static boolean isRejection(LoggedEvent event) {
    final RecordMetadata metadata = getMetadata(event);
    return metadata.getRecordType() == RecordType.COMMAND_REJECTION;
  }

  public static boolean isRejection(LoggedEvent event, ValueType valueType, Intent intent) {
    return isRejection(event) && isRecordOfType(event, valueType) && hasIntent(event, intent);
  }

  public static boolean isEvent(LoggedEvent event) {
    final RecordMetadata metadata = getMetadata(event);
    return metadata.getRecordType() == RecordType.EVENT;
  }

  public static boolean isEvent(LoggedEvent event, ValueType valueType, Intent intent) {
    return isEvent(event) && isRecordOfType(event, valueType) && hasIntent(event, intent);
  }

  public static boolean isCommand(LoggedEvent event) {
    final RecordMetadata metadata = getMetadata(event);
    return metadata.getRecordType() == RecordType.COMMAND;
  }

  public static boolean isCommand(LoggedEvent event, ValueType valueType, Intent intent) {
    return isCommand(event) && isRecordOfType(event, valueType) && hasIntent(event, intent);
  }

  protected static boolean isRecordOfType(LoggedEvent event, ValueType type) {
    if (event == null) {
      return false;
    }

    final RecordMetadata metadata = getMetadata(event);

    return metadata.getValueType() == type;
  }
}
