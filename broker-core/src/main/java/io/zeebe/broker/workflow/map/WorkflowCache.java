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
package io.zeebe.broker.workflow.map;

import static io.zeebe.protocol.Protocol.DEPLOYMENT_PARTITION;

import io.zeebe.broker.Loggers;
import io.zeebe.broker.clustering.base.topology.NodeInfo;
import io.zeebe.broker.clustering.base.topology.PartitionInfo;
import io.zeebe.broker.clustering.base.topology.TopologyManager;
import io.zeebe.broker.clustering.base.topology.TopologyPartitionListener;
import io.zeebe.broker.workflow.deployment.data.DeploymentRecord;
import io.zeebe.broker.workflow.deployment.data.DeploymentResource;
import io.zeebe.broker.workflow.deployment.data.Workflow;
import io.zeebe.broker.workflow.model.ExecutableWorkflow;
import io.zeebe.broker.workflow.model.transformation.BpmnTransformer;
import io.zeebe.model.bpmn.Bpmn;
import io.zeebe.model.bpmn.BpmnModelInstance;
import io.zeebe.transport.ClientTransport;
import io.zeebe.transport.RemoteAddress;
import io.zeebe.transport.SocketAddress;
import io.zeebe.util.buffer.BufferUtil;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.agrona.DirectBuffer;
import org.agrona.collections.Int2ObjectHashMap;
import org.agrona.collections.Long2ObjectHashMap;
import org.agrona.io.DirectBufferInputStream;

public class WorkflowCache implements TopologyPartitionListener {

  private final Long2ObjectHashMap<DeployedWorkflow> workflowsByKey = new Long2ObjectHashMap<>();
  private final Map<DirectBuffer, Int2ObjectHashMap<DeployedWorkflow>>
      workflowsByProcessIdAndVersion = new HashMap<>();
  private final Map<DirectBuffer, DeployedWorkflow> latestWorkflowsByProcessId = new HashMap<>();

  private final ClientTransport clientTransport;
  private final TopologyManager topologyManager;

  private final BpmnTransformer transformer = new BpmnTransformer();

  private volatile RemoteAddress defaultTopicLeaderAddress;

  public WorkflowCache(ClientTransport clientTransport, TopologyManager topologyManager) {
    this.clientTransport = clientTransport;
    this.topologyManager = topologyManager;
    topologyManager.addTopologyPartitionListener(this);
  }

  public void close() {
    topologyManager.removeTopologyPartitionListener(this);
  }

  public void addWorkflow(DeploymentRecord deploymentRecord) {
    for (Workflow workflow : deploymentRecord.workflows()) {
      final long key = workflow.getKey();

      final DirectBuffer resourceName = workflow.getResourceName();
      for (DeploymentResource resource : deploymentRecord.resources()) {
        if (resource.getResourceName().equals(resourceName)) {
          addWorkflowToCache(workflow, key, resource);
        }
      }
    }
  }

  private void addWorkflowToCache(Workflow workflow, long key, DeploymentResource resource) {
    final int version = workflow.getVersion();
    final DirectBuffer bpmnProcessId = workflow.getBpmnProcessId();
    final DirectBuffer bpmnXml = resource.getResource();
    Loggers.WORKFLOW_REPOSITORY_LOGGER.trace(
        "Workflow {} with version {} added", bpmnProcessId, version);
    // TODO: pull these things apart
    // TODO: may wanna catch exceptions
    final BpmnModelInstance modelInstance =
        Bpmn.readModelFromStream(new DirectBufferInputStream(bpmnXml));

    // TODO: do design time and runtime validation

    final List<ExecutableWorkflow> definitions = transformer.transformDefinitions(modelInstance);

    final ExecutableWorkflow executableWorkflow =
        definitions
            .stream()
            .filter((w) -> BufferUtil.equals(bpmnProcessId, w.getId()))
            .findFirst()
            .get();

    final DeployedWorkflow deployedWorkflow =
        new DeployedWorkflow(executableWorkflow, key, version);

    addDeployedWorkflowToState(key, deployedWorkflow, version, bpmnProcessId);
  }

  private void addDeployedWorkflowToState(
      long key, DeployedWorkflow deployedWorkflow, int version, DirectBuffer bpmnProcessId) {
    workflowsByKey.put(key, deployedWorkflow);

    Int2ObjectHashMap<DeployedWorkflow> versionMap =
        workflowsByProcessIdAndVersion.get(bpmnProcessId);

    if (versionMap == null) {
      versionMap = new Int2ObjectHashMap<>();
      workflowsByProcessIdAndVersion.put(bpmnProcessId, versionMap);
    }

    versionMap.put(version, deployedWorkflow);

    final DeployedWorkflow latestVersion = latestWorkflowsByProcessId.get(bpmnProcessId);
    if (latestVersion == null || latestVersion.getVersion() < version) {
      latestWorkflowsByProcessId.put(bpmnProcessId, deployedWorkflow);
    }
  }

  private RemoteAddress systemTopicLeader() {
    return defaultTopicLeaderAddress;
  }

  public DeployedWorkflow getLatestWorkflowVersionByProcessId(DirectBuffer processId) {
    return latestWorkflowsByProcessId.get(processId);
  }

  public DeployedWorkflow getWorkflowByProcessIdAndVersion(DirectBuffer processId, int version) {
    final Int2ObjectHashMap<DeployedWorkflow> versionMap =
        workflowsByProcessIdAndVersion.get(processId);

    if (versionMap != null) {
      return versionMap.get(version);
    } else {
      return null;
    }
  }

  public DeployedWorkflow getWorkflowByKey(long key) {
    return workflowsByKey.get(key);
  }

  @Override
  public void onPartitionUpdated(PartitionInfo partitionInfo, NodeInfo member) {
    final RemoteAddress currentLeader = defaultTopicLeaderAddress;

    if (partitionInfo.getPartitionId() == DEPLOYMENT_PARTITION) {
      if (member.getLeaders().contains(partitionInfo)) {
        final SocketAddress managementApiAddress = member.getManagementApiAddress();
        if (currentLeader == null || currentLeader.getAddress().equals(managementApiAddress)) {
          defaultTopicLeaderAddress = clientTransport.registerRemoteAddress(managementApiAddress);
        }
      }
    }
  }
}
