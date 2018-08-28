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
package io.zeebe.broker.system;

import static io.zeebe.broker.clustering.base.ClusterBaseLayerServiceNames.LEADER_PARTITION_GROUP_NAME;
import static io.zeebe.broker.clustering.base.ClusterBaseLayerServiceNames.LEADER_PARTITION_SYSTEM_GROUP_NAME;
import static io.zeebe.broker.clustering.base.ClusterBaseLayerServiceNames.TOPOLOGY_MANAGER_SERVICE;
import static io.zeebe.broker.clustering.orchestration.ClusterOrchestrationLayerServiceNames.KNOWN_TOPICS_SERVICE_NAME;
import static io.zeebe.broker.logstreams.LogStreamServiceNames.STREAM_PROCESSOR_SERVICE_FACTORY;
import static io.zeebe.broker.system.SystemServiceNames.DEPLOYMENT_MANAGER_SERVICE;
import static io.zeebe.broker.system.SystemServiceNames.FETCH_CREATED_TOPIC_HANDLER;
import static io.zeebe.broker.system.SystemServiceNames.LEADER_MANAGEMENT_REQUEST_HANDLER;
import static io.zeebe.broker.system.SystemServiceNames.METRICS_FILE_WRITER;
import static io.zeebe.broker.transport.TransportServiceNames.CLIENT_API_SERVER_NAME;
import static io.zeebe.broker.transport.TransportServiceNames.MANAGEMENT_API_CLIENT_NAME;
import static io.zeebe.broker.transport.TransportServiceNames.MANAGEMENT_API_SERVER_NAME;
import static io.zeebe.broker.transport.TransportServiceNames.bufferingServerTransport;
import static io.zeebe.broker.transport.TransportServiceNames.clientTransport;
import static io.zeebe.broker.transport.TransportServiceNames.serverTransport;

import io.zeebe.broker.system.management.LeaderManagementRequestHandler;
import io.zeebe.broker.system.management.topics.FetchCreatedTopicsRequestHandlerService;
import io.zeebe.broker.system.metrics.MetricsFileWriterService;
import io.zeebe.broker.transport.TransportServiceNames;
import io.zeebe.broker.workflow.deployment.distribute.service.DeploymentManager;
import io.zeebe.servicecontainer.ServiceContainer;

public class SystemComponent implements Component {

  @Override
  public void init(SystemContext context) {
    final ServiceContainer serviceContainer = context.getServiceContainer();

    final MetricsFileWriterService metricsFileWriterService =
        new MetricsFileWriterService(context.getBrokerConfiguration().getMetrics());
    serviceContainer.createService(METRICS_FILE_WRITER, metricsFileWriterService).install();

    final LeaderManagementRequestHandler requestHandlerService =
        new LeaderManagementRequestHandler();
    serviceContainer
        .createService(LEADER_MANAGEMENT_REQUEST_HANDLER, requestHandlerService)
        .dependency(
            bufferingServerTransport(MANAGEMENT_API_SERVER_NAME),
            requestHandlerService.getManagementApiServerTransportInjector())
        .groupReference(
            LEADER_PARTITION_GROUP_NAME, requestHandlerService.getLeaderPartitionsGroupReference())
        .install();

    final DeploymentManager deploymentManagerService = new DeploymentManager();
    serviceContainer
        .createService(DEPLOYMENT_MANAGER_SERVICE, deploymentManagerService)
        .dependency(
            LEADER_MANAGEMENT_REQUEST_HANDLER,
            deploymentManagerService.getRequestHandlerServiceInjector())
        .dependency(
            STREAM_PROCESSOR_SERVICE_FACTORY,
            deploymentManagerService.getStreamProcessorServiceFactoryInjector())
        .dependency(
            serverTransport(CLIENT_API_SERVER_NAME),
            deploymentManagerService.getClientApiTransportInjector())
        .dependency(
            TransportServiceNames.CONTROL_MESSAGE_HANDLER_MANAGER,
            deploymentManagerService.getControlMessageHandlerManagerServiceInjector())
        .dependency(TOPOLOGY_MANAGER_SERVICE, deploymentManagerService.getTopologyManagerInjector())
        .dependency(
            clientTransport(MANAGEMENT_API_CLIENT_NAME),
            deploymentManagerService.getManagementApiClientInjector())
        .groupReference(
            LEADER_PARTITION_GROUP_NAME, deploymentManagerService.getPartitionsGroupReference())
        .install();

    final FetchCreatedTopicsRequestHandlerService fetchCreatedTopicsRequestHandler =
        new FetchCreatedTopicsRequestHandlerService();
    serviceContainer
        .createService(FETCH_CREATED_TOPIC_HANDLER, fetchCreatedTopicsRequestHandler)
        .dependency(
            KNOWN_TOPICS_SERVICE_NAME, fetchCreatedTopicsRequestHandler.getKnownTopicsInjector())
        .dependency(
            LEADER_MANAGEMENT_REQUEST_HANDLER,
            fetchCreatedTopicsRequestHandler.getRequestHandlerServiceInjector())
        .groupReference(
            LEADER_PARTITION_SYSTEM_GROUP_NAME,
            fetchCreatedTopicsRequestHandler.getPartitionsGroupReference())
        .install();
  }
}
