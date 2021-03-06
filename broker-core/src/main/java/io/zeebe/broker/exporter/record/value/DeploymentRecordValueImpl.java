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
package io.zeebe.broker.exporter.record.value;

import io.zeebe.broker.exporter.record.RecordValueImpl;
import io.zeebe.exporter.record.value.DeploymentRecordValue;
import io.zeebe.exporter.record.value.deployment.DeployedWorkflow;
import io.zeebe.exporter.record.value.deployment.DeploymentResource;
import io.zeebe.gateway.impl.data.ZeebeObjectMapperImpl;
import java.util.List;
import java.util.Objects;

public class DeploymentRecordValueImpl extends RecordValueImpl implements DeploymentRecordValue {
  private List<DeployedWorkflow> deployedWorkflows;
  private List<DeploymentResource> resources;

  public DeploymentRecordValueImpl(
      ZeebeObjectMapperImpl objectMapper,
      List<DeployedWorkflow> deployedWorkflows,
      List<DeploymentResource> resources) {
    super(objectMapper);
    this.deployedWorkflows = deployedWorkflows;
    this.resources = resources;
  }

  @Override
  public List<DeployedWorkflow> getDeployedWorkflows() {
    return deployedWorkflows;
  }

  @Override
  public List<DeploymentResource> getResources() {
    return resources;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final DeploymentRecordValueImpl that = (DeploymentRecordValueImpl) o;
    return Objects.equals(deployedWorkflows, that.deployedWorkflows)
        && Objects.equals(resources, that.resources);
  }

  @Override
  public int hashCode() {
    return Objects.hash(deployedWorkflows, resources);
  }

  @Override
  public String toString() {
    return "DeploymentRecordValueImpl{"
        + "deployedWorkflows="
        + deployedWorkflows
        + ", resources="
        + resources
        + '}';
  }
}
