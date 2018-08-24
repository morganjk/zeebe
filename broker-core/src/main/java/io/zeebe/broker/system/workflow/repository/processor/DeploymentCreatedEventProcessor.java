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
package io.zeebe.broker.system.workflow.repository.processor;

import io.zeebe.broker.Loggers;
import io.zeebe.broker.logstreams.processor.TypedBatchWriter;
import io.zeebe.broker.logstreams.processor.TypedRecord;
import io.zeebe.broker.logstreams.processor.TypedRecordProcessor;
import io.zeebe.broker.logstreams.processor.TypedResponseWriter;
import io.zeebe.broker.logstreams.processor.TypedStreamWriter;
import io.zeebe.broker.subscription.message.data.MessageSubscriptionRecord;
import io.zeebe.broker.system.workflow.repository.data.DeployedWorkflow;
import io.zeebe.broker.system.workflow.repository.data.DeploymentRecord;
import io.zeebe.broker.system.workflow.repository.processor.state.WorkflowRepositoryIndex;
import io.zeebe.broker.system.workflow.repository.processor.state.WorkflowRepositoryIndex.WorkflowMetadata;
import io.zeebe.broker.workflow.model.ExecutableStartEvent;
import io.zeebe.broker.workflow.model.ExecutableWorkflow;
import io.zeebe.broker.workflow.model.transformation.BpmnTransformer;
import io.zeebe.model.bpmn.Bpmn;
import io.zeebe.model.bpmn.BpmnModelInstance;
import io.zeebe.msgpack.value.ValueArray;
import io.zeebe.protocol.intent.MessageSubscriptionIntent;
import io.zeebe.util.buffer.BufferUtil;
import java.util.List;
import org.agrona.DirectBuffer;
import org.agrona.io.DirectBufferInputStream;

public class DeploymentCreatedEventProcessor implements TypedRecordProcessor<DeploymentRecord> {
  private WorkflowRepositoryIndex repositoryIndex;

  private final BpmnTransformer transformer = new BpmnTransformer();

  private TypedBatchWriter batchWriter;

  public DeploymentCreatedEventProcessor(WorkflowRepositoryIndex repositoryIndex) {
    this.repositoryIndex = repositoryIndex;
  }

  @Override
  public void processRecord(
      TypedRecord<DeploymentRecord> event,
      TypedResponseWriter responseWriter,
      TypedStreamWriter streamWriter) {
    addWorkflowsToIndex(event);

    batchWriter = null;

    // TODO create a message subscription per message start event
    // TODO remove message subscriptions of previous workflow
    event
        .getValue()
        .resources()
        .forEach(
            resource -> {
              final DirectBufferInputStream inputStream =
                  new DirectBufferInputStream(resource.getResource());
              final BpmnModelInstance modelInstance = Bpmn.readModelFromStream(inputStream);

              final List<ExecutableWorkflow> workflows =
                  transformer.transformDefinitions(modelInstance);
              workflows.forEach(
                  workflow -> {
                    // TODO add support for multiple start events
                    final ExecutableStartEvent startEvent = workflow.getStartEvent();
                    if (startEvent.isMessageStartEvent()) {
                      final DirectBuffer messageName = startEvent.getMessageName();

                      // TODO get workflow key by resource
                      long workflowKey = -1L;
                      for (final DeployedWorkflow deployedWorkflow :
                          event.getValue().deployedWorkflows()) {
                        if (BufferUtil.contentsEqual(
                            deployedWorkflow.getResourceName(), resource.getResourceName())) {
                          workflowKey = deployedWorkflow.getKey();
                        }
                      }

                      final MessageSubscriptionRecord subscription =
                          new MessageSubscriptionRecord();
                      subscription.setMessageName(messageName).setWorkflowKey(workflowKey);

                      if (batchWriter == null) {
                        batchWriter = streamWriter.newBatch();
                      }

                      batchWriter.addNewCommand(
                          MessageSubscriptionIntent.OPEN_FOR_START_EVENT, subscription);
                    }
                  });
            });
  }

  private void addWorkflowsToIndex(TypedRecord<DeploymentRecord> event) {
    final DeploymentRecord deploymentEvent = event.getValue();
    final ValueArray<DeployedWorkflow> deployedWorkflows = deploymentEvent.deployedWorkflows();

    for (final DeployedWorkflow deployedWorkflow : deployedWorkflows) {
      final WorkflowMetadata workflowMetadata =
          new WorkflowMetadata()
              .setKey(deployedWorkflow.getKey())
              .setBpmnProcessId(BufferUtil.bufferAsString(deployedWorkflow.getBpmnProcessId()))
              .setEventPosition(event.getPosition())
              .setVersion(deployedWorkflow.getVersion())
              .setResourceName(BufferUtil.bufferAsString(deployedWorkflow.getResourceName()));
      Loggers.WORKFLOW_REPOSITORY_LOGGER.trace(
          "Workflow {} with version {} added",
          workflowMetadata.getBpmnProcessId(),
          workflowMetadata.getVersion());
      repositoryIndex.add(workflowMetadata);
    }
  }
}
