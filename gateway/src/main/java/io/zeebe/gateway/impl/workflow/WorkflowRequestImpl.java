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
package io.zeebe.gateway.impl.workflow;

import io.zeebe.gateway.api.commands.WorkflowRequestStep1;
import io.zeebe.gateway.api.commands.Workflows;
import io.zeebe.gateway.impl.ControlMessageRequest;
import io.zeebe.gateway.impl.RequestManager;
import io.zeebe.protocol.Protocol;
import io.zeebe.protocol.clientapi.ControlMessageType;

public class WorkflowRequestImpl extends ControlMessageRequest<Workflows>
    implements WorkflowRequestStep1 {
  private final Request request;

  public WorkflowRequestImpl(RequestManager client) {
    super(client, ControlMessageType.LIST_WORKFLOWS, WorkflowsImpl.class);

    setTargetPartition(Protocol.SYSTEM_PARTITION);

    request = new Request();
  }

  @Override
  public WorkflowRequestStep1 bpmnProcessId(String bpmnProcessId) {
    request.setBpmnProcessId(bpmnProcessId);
    return this;
  }

  @Override
  public Object getRequest() {
    return request;
  }

  class Request {
    private String bpmnProcessId;

    public String getBpmnProcessId() {
      return bpmnProcessId;
    }

    public void setBpmnProcessId(String bpmnProcessId) {
      this.bpmnProcessId = bpmnProcessId;
    }
  }
}
