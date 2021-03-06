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
package io.zeebe.broker.workflow.processor;

import io.zeebe.util.buffer.BufferUtil;
import io.zeebe.util.metrics.Metric;
import io.zeebe.util.metrics.MetricsManager;
import org.agrona.DirectBuffer;

public class WorkflowInstanceMetrics implements AutoCloseable {
  private Metric workflowInstanceEventCanceled;
  private Metric workflowInstanceEventCompleted;

  public WorkflowInstanceMetrics(
      MetricsManager metricsManager, DirectBuffer topicName, int partitionId) {
    final String topicNameString = BufferUtil.bufferAsString(topicName);
    final String partitionIdString = Integer.toString(partitionId);

    workflowInstanceEventCanceled =
        metricsManager
            .newMetric("workflow_instance_events_count")
            .type("counter")
            .label("topic", topicNameString)
            .label("partition", partitionIdString)
            .label("type", "canceled")
            .create();

    workflowInstanceEventCompleted =
        metricsManager
            .newMetric("workflow_instance_events_count")
            .type("counter")
            .label("topic", topicNameString)
            .label("partition", partitionIdString)
            .label("type", "completed")
            .create();
  }

  public void countInstanceCanceled() {
    workflowInstanceEventCanceled.incrementOrdered();
  }

  public void coundInstanceCompleted() {
    workflowInstanceEventCompleted.incrementOrdered();
  }

  @Override
  public void close() {
    workflowInstanceEventCanceled.close();
    workflowInstanceEventCompleted.close();
  }
}
