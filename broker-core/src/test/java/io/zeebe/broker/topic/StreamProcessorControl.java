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
import io.zeebe.broker.incident.data.IncidentRecord;
import io.zeebe.broker.job.data.JobRecord;
import io.zeebe.broker.logstreams.processor.TypedRecord;
import io.zeebe.broker.subscription.message.data.MessageRecord;
import io.zeebe.broker.subscription.message.data.MessageSubscriptionRecord;
import io.zeebe.broker.subscription.message.data.WorkflowInstanceSubscriptionRecord;
import io.zeebe.broker.workflow.data.WorkflowInstanceRecord;
import io.zeebe.broker.workflow.deployment.data.DeploymentRecord;
import io.zeebe.logstreams.log.LoggedEvent;
import java.util.function.Predicate;

public interface StreamProcessorControl {

  void unblock();

  void blockAfterEvent(Predicate<LoggedEvent> test);

  void blockAfterJobEvent(Predicate<TypedRecord<JobRecord>> test);

  void blockAfterDeploymentEvent(Predicate<TypedRecord<DeploymentRecord>> test);

  void blockAfterWorkflowInstanceRecord(Predicate<TypedRecord<WorkflowInstanceRecord>> test);

  void blockAfterIncidentEvent(Predicate<TypedRecord<IncidentRecord>> test);

  void blockAfterTopicEvent(Predicate<TypedRecord<TopicRecord>> test);

  void blockAfterMessageEvent(Predicate<TypedRecord<MessageRecord>> test);

  void blockAfterMessageSubscriptionEvent(Predicate<TypedRecord<MessageSubscriptionRecord>> test);

  void blockAfterWorkflowInstanceSubscriptionEvent(
      Predicate<TypedRecord<WorkflowInstanceSubscriptionRecord>> test);

  void purgeSnapshot();

  /**
   * @return true if the event to block on has been processed and the stream processor won't handle
   *     any more events until {@link #unblock()} is called.
   */
  boolean isBlocked();

  void close();

  void start();

  void restart();
}
