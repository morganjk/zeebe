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
package io.zeebe.gateway;

import io.zeebe.gateway.api.commands.PartitionInfo;
import io.zeebe.gateway.api.commands.Topology;
import io.zeebe.gateway.api.commands.Workflow;
import io.zeebe.gateway.api.events.DeploymentEvent;
import io.zeebe.gateway.cmd.ClientException;
import io.zeebe.gateway.protocol.GatewayOuterClass.BrokerInfo;
import io.zeebe.gateway.protocol.GatewayOuterClass.BrokerInfo.Builder;
import io.zeebe.gateway.protocol.GatewayOuterClass.DeployWorkflowResponse;
import io.zeebe.gateway.protocol.GatewayOuterClass.HealthResponse;
import io.zeebe.gateway.protocol.GatewayOuterClass.Partition;
import io.zeebe.gateway.protocol.GatewayOuterClass.Partition.PartitionBrokerRole;
import io.zeebe.gateway.protocol.GatewayOuterClass.WorkflowInfoResponse;
import java.util.ArrayList;
import java.util.List;

public class ResponseMapper {

  public DeployWorkflowResponse toDeployWorkflowResponse(final DeploymentEvent brokerResponse) {
    final DeployWorkflowResponse.Builder deployWorkflowBuilder =
        DeployWorkflowResponse.newBuilder();
    final List<Workflow> workflows = brokerResponse.getDeployedWorkflows();
    for (final Workflow workflow : workflows) {
      final WorkflowInfoResponse.Builder responseWorkflow = WorkflowInfoResponse.newBuilder();
      responseWorkflow.setBpmnProcessId(workflow.getBpmnProcessId());
      responseWorkflow.setVersion(workflow.getVersion());
      responseWorkflow.setWorkflowKey(workflow.getWorkflowKey());
      responseWorkflow.setResourceName(workflow.getResourceName());
      deployWorkflowBuilder.addWorkflows(responseWorkflow);
    }
    return deployWorkflowBuilder.build();
  }

  private PartitionBrokerRole remapPartitionBrokerRoleEnum(
      final io.zeebe.gateway.api.commands.BrokerInfo brokerInfo, final PartitionInfo partition) {
    switch (partition.getRole()) {
      case LEADER:
        return PartitionBrokerRole.LEADER;
      case FOLLOWER:
        return PartitionBrokerRole.FOLLOW;
      default:
        throw new ClientException(
            "Unknown broker role in response for partition "
                + partition
                + " on broker "
                + brokerInfo);
    }
  }

  public HealthResponse toHealthResponse(final Topology brokerResponse) {
    final HealthResponse.Builder healthResponseBuilder = HealthResponse.newBuilder();
    final ArrayList<BrokerInfo> infos = new ArrayList<>();

    for (final io.zeebe.gateway.api.commands.BrokerInfo el : brokerResponse.getBrokers()) {
      final Builder brokerInfo = BrokerInfo.newBuilder();
      brokerInfo.setHost(el.getHost());
      brokerInfo.setPort(el.getPort());

      for (final PartitionInfo p : el.getPartitions()) {
        final Partition.Builder partitionBuilder = Partition.newBuilder();
        partitionBuilder.setPartitionId(p.getPartitionId());
        partitionBuilder.setTopicName(p.getTopicName());
        partitionBuilder.setRole(remapPartitionBrokerRoleEnum(el, p));
        brokerInfo.addPartitions(partitionBuilder);
      }

      infos.add(brokerInfo.build());
    }

    healthResponseBuilder.addAllBrokers(infos);
    return healthResponseBuilder.build();
  }
}
