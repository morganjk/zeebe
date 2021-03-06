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

import io.zeebe.client.api.commands.BrokerInfo;
import io.zeebe.client.api.commands.PartitionBrokerRole;
import io.zeebe.client.api.commands.PartitionInfo;
import io.zeebe.gateway.protocol.GatewayOuterClass;
import java.util.ArrayList;
import java.util.List;

public class BrokerInfoImpl implements BrokerInfo {

  class PartitionInfoImpl implements PartitionInfo {

    private int partitionId;
    private String topicName;
    private PartitionBrokerRole role;

    PartitionInfoImpl(final GatewayOuterClass.Partition partition) {
      serialize(partition);
    }

    private void serialize(final GatewayOuterClass.Partition partition) throws RuntimeException {
      this.partitionId = partition.getPartitionId();
      this.topicName = partition.getTopicName();

      if (partition.getRole() == GatewayOuterClass.Partition.PartitionBrokerRole.LEADER) {
        this.role = PartitionBrokerRole.LEADER;
      } else if (partition.getRole() == GatewayOuterClass.Partition.PartitionBrokerRole.FOLLOW) {
        this.role = PartitionBrokerRole.FOLLOWER;
      } else {
        throw new RuntimeException("unknown partition broker role");
      }
    }

    @Override
    public int getPartitionId() {
      return this.partitionId;
    }

    @Override
    public String getTopicName() {
      return this.topicName;
    }

    @Override
    public PartitionBrokerRole getRole() {
      return this.role;
    }

    @Override
    public boolean isLeader() {
      return this.role == PartitionBrokerRole.LEADER;
    }
  }

  private String host;
  private int port;
  private List<PartitionInfo> partitions;

  public BrokerInfoImpl(final GatewayOuterClass.BrokerInfo broker) {
    serialize(broker);
  }

  private void serialize(final GatewayOuterClass.BrokerInfo broker) {
    this.host = broker.getHost();
    this.port = broker.getPort();

    this.partitions = new ArrayList<>();
    for (final GatewayOuterClass.Partition partition : broker.getPartitionsList()) {
      this.partitions.add(new PartitionInfoImpl(partition));
    }
  }

  @Override
  public String getHost() {
    return this.host;
  }

  @Override
  public int getPort() {
    return this.port;
  }

  @Override
  public String getAddress() {
    return String.format("%s:%d", this.host, this.port);
  }

  @Override
  public List<PartitionInfo> getPartitions() {
    return this.partitions;
  }
}
