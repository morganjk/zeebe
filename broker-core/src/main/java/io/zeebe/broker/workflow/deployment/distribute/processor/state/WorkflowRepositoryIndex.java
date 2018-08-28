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
package io.zeebe.broker.workflow.deployment.distribute.processor.state;

import io.zeebe.broker.logstreams.processor.JsonSnapshotSupport;
import io.zeebe.broker.workflow.deployment.distribute.processor.state.WorkflowRepositoryIndex.WorkflowRepositoryIndexData;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class WorkflowRepositoryIndex extends JsonSnapshotSupport<WorkflowRepositoryIndexData> {
  public WorkflowRepositoryIndex() {
    super(WorkflowRepositoryIndexData.class);
  }

  public static class WorkflowRepositoryIndexData {
    private long lastGeneratedKey = 0;

    private Map<Long, WorkflowMetadata> workflows = new HashMap<>();
    private Map<String, WorkflowsByBpmnProcessId> bpmnProcessIds = new HashMap<>();

    public Map<Long, WorkflowMetadata> getWorkflows() {
      return workflows;
    }

    public void setWorkflows(Map<Long, WorkflowMetadata> workflows) {
      this.workflows = workflows;
    }

    public Map<String, WorkflowsByBpmnProcessId> getBpmnProcessIds() {
      return bpmnProcessIds;
    }

    public long getLastGeneratedKey() {
      return lastGeneratedKey;
    }

    public void setLastGeneratedKey(long lastKey) {
      this.lastGeneratedKey = lastKey;
    }
  }

  public static class WorkflowsByBpmnProcessId {
    private int lastGeneratedVersion = 0;

    private TreeMap<Integer, Long> versions = new TreeMap<>();

    public int getLastGeneratedVersion() {
      return lastGeneratedVersion;
    }

    public void setLastGeneratedVersion(int latestVersion) {
      this.lastGeneratedVersion = latestVersion;
    }

    public TreeMap<Integer, Long> getVersions() {
      return versions;
    }
  }

  public static class WorkflowMetadata {
    private long key;
    private int version;
    private String bpmnProcessId;
    private String resourceName;
    private long eventPosition;

    public long getKey() {
      return key;
    }

    public WorkflowMetadata setKey(long key) {
      this.key = key;
      return this;
    }

    public int getVersion() {
      return version;
    }

    public WorkflowMetadata setVersion(int version) {
      this.version = version;
      return this;
    }

    public String getBpmnProcessId() {
      return bpmnProcessId;
    }

    public WorkflowMetadata setBpmnProcessId(String bpmnProcessId) {
      this.bpmnProcessId = bpmnProcessId;
      return this;
    }

    public long getEventPosition() {
      return eventPosition;
    }

    public WorkflowMetadata setEventPosition(long eventPosition) {
      this.eventPosition = eventPosition;
      return this;
    }

    public String getResourceName() {
      return resourceName;
    }

    public WorkflowMetadata setResourceName(String resourceName) {
      this.resourceName = resourceName;
      return this;
    }
  }

  public long getNextKey() {
    final WorkflowRepositoryIndexData data = getData();

    final long nextKey = data.getLastGeneratedKey() + 1;

    data.setLastGeneratedKey(nextKey);

    return nextKey;
  }

  public int getNextVersion(String bpmnProcessId) {
    final WorkflowsByBpmnProcessId byBpmnProcessId =
        getData()
            .getBpmnProcessIds()
            .computeIfAbsent(bpmnProcessId, (id) -> new WorkflowsByBpmnProcessId());

    final int nextVersion = byBpmnProcessId.getLastGeneratedVersion() + 1;

    byBpmnProcessId.setLastGeneratedVersion(nextVersion);

    return nextVersion;
  }

  public void add(WorkflowMetadata workflow) {
    final WorkflowRepositoryIndexData data = getData();

    data.getWorkflows().put(workflow.getKey(), workflow);

    final WorkflowsByBpmnProcessId byBpmnProcessId =
        getData()
            .getBpmnProcessIds()
            .computeIfAbsent(workflow.getBpmnProcessId(), (id) -> new WorkflowsByBpmnProcessId());

    byBpmnProcessId.getVersions().put(workflow.getVersion(), workflow.getKey());
  }

  private WorkflowsByBpmnProcessId getWorkflowsByTopicNameAndBpmnProcessId(String bpmnProcessId) {

    final WorkflowsByBpmnProcessId workflowsByBpmnProcessId =
        getData().getBpmnProcessIds().get(bpmnProcessId);

    if (workflowsByBpmnProcessId != null) {
      return workflowsByBpmnProcessId;
    }

    return null;
  }

  public WorkflowMetadata getWorkflowByKey(long key) {
    return getData().getWorkflows().get(key);
  }

  public WorkflowMetadata getLatestWorkflowByBpmnProcessId(String bpmnProcessId) {
    final WorkflowsByBpmnProcessId byBpmnProcessId =
        getWorkflowsByTopicNameAndBpmnProcessId(bpmnProcessId);

    if (byBpmnProcessId != null) {
      final Entry<Integer, Long> latestVersion = byBpmnProcessId.getVersions().lastEntry();

      if (latestVersion != null) {
        return getData().getWorkflows().get(latestVersion.getValue());
      }
    }

    return null;
  }

  public WorkflowMetadata getWorkflowByBpmnProcessIdAndVersion(String bpmnProcessId, int version) {
    final WorkflowsByBpmnProcessId byBpmnProcessId =
        getWorkflowsByTopicNameAndBpmnProcessId(bpmnProcessId);

    if (byBpmnProcessId != null) {
      final Long key = byBpmnProcessId.getVersions().get(version);

      if (key != null) {
        return getData().getWorkflows().get(key);
      }
    }

    return null;
  }

  public List<WorkflowMetadata> getWorkflows() {
    final WorkflowRepositoryIndexData data = getData();

    return data.getBpmnProcessIds()
        .values()
        .stream()
        .map((byProcessId) -> byProcessId.getVersions().values())
        .flatMap((keys) -> keys.stream())
        .map((key) -> data.getWorkflows().get(key))
        .collect(Collectors.toList());
  }

  public List<WorkflowMetadata> getWorkflowsByBpmnProcessId(String bpmnProcessId) {
    final WorkflowRepositoryIndexData data = getData();

    return data.getBpmnProcessIds()
        .getOrDefault(bpmnProcessId, new WorkflowsByBpmnProcessId())
        .getVersions()
        .values()
        .stream()
        .map((key) -> data.getWorkflows().get(key))
        .collect(Collectors.toList());
  }
}
