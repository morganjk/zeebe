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
package io.zeebe.gateway.impl.subscription.topic;

import io.zeebe.gateway.impl.CommandImpl;
import io.zeebe.gateway.impl.RequestManager;
import io.zeebe.gateway.impl.command.TopicSubscriberCommandImpl;
import io.zeebe.gateway.impl.event.TopicSubscriberEventImpl;
import io.zeebe.gateway.impl.record.RecordImpl;
import io.zeebe.protocol.intent.SubscriberIntent;

public class CreateTopicSubscriptionCommandImpl extends CommandImpl<TopicSubscriberEventImpl> {
  private final TopicSubscriberCommandImpl command =
      new TopicSubscriberCommandImpl(SubscriberIntent.SUBSCRIBE);

  public CreateTopicSubscriptionCommandImpl(
      final RequestManager commandManager, final String topicName, final int partitionId) {
    super(commandManager);

    this.command.setTopicName(topicName);
    this.command.setPartitionId(partitionId);
  }

  public CreateTopicSubscriptionCommandImpl startPosition(long startPosition) {
    this.command.setStartPosition(startPosition);
    return this;
  }

  public CreateTopicSubscriptionCommandImpl name(String name) {
    this.command.setName(name);
    return this;
  }

  public CreateTopicSubscriptionCommandImpl bufferSize(int bufferSize) {
    this.command.setBufferSize(bufferSize);
    return this;
  }

  public CreateTopicSubscriptionCommandImpl forceStart(boolean forceStart) {
    this.command.setForceStart(forceStart);
    return this;
  }

  @Override
  public RecordImpl getCommand() {
    return command;
  }
}
