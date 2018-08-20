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
package io.zeebe.example.workflow;

import io.zeebe.gateway.ZeebeClient;
import io.zeebe.gateway.ZeebeClientBuilder;
import io.zeebe.gateway.api.clients.WorkflowClient;
import io.zeebe.gateway.api.events.WorkflowInstanceEvent;
import io.zeebe.util.sched.future.ActorFuture;

public class NonBlockingWorkflowInstanceCreator {
  public static void main(String[] args) {
    final String broker = "127.0.0.1:26501";
    final int numberOfInstances = 100_000;
    final String bpmnProcessId = "demoProcess";

    final ZeebeClientBuilder builder = ZeebeClient.newClientBuilder().brokerContactPoint(broker);

    try (ZeebeClient client = builder.build()) {
      final WorkflowClient workflowClient = client.topicClient().workflowClient();

      System.out.println("Creating " + numberOfInstances + " workflow instances");

      final long startTime = System.currentTimeMillis();

      long instancesCreating = 0;

      while (instancesCreating < numberOfInstances) {
        // this is non-blocking/async => returns a future
        final ActorFuture<WorkflowInstanceEvent> future =
            workflowClient
                .newCreateInstanceCommand()
                .bpmnProcessId(bpmnProcessId)
                .latestVersion()
                .send();

        // could put the future somewhere and eventually wait for its completion

        instancesCreating++;
      }

      // creating one more instance; joining on this future ensures
      // that all the other create commands were handled
      workflowClient
          .newCreateInstanceCommand()
          .bpmnProcessId(bpmnProcessId)
          .latestVersion()
          .send()
          .join();

      System.out.println("Took: " + (System.currentTimeMillis() - startTime));
    }
  }
}
