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
package io.zeebe.client.impl;

import io.zeebe.client.api.commands.Workflow;

public class WorkflowImpl implements Workflow {

  private final String bpmnProcessId;
  private final int version;
  private final long workflowKey;
  private final String resourceName;

  WorkflowImpl(
      final String bpmnProcessId,
      final int version,
      final long workflowKey,
      final String resourceName) {
    this.bpmnProcessId = bpmnProcessId;
    this.version = version;
    this.workflowKey = workflowKey;
    this.resourceName = resourceName;
  }

  @Override
  public String getBpmnProcessId() {
    return bpmnProcessId;
  }

  @Override
  public int getVersion() {
    return version;
  }

  @Override
  public long getWorkflowKey() {
    return workflowKey;
  }

  @Override
  public String getResourceName() {
    return resourceName;
  }
}
