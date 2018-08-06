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
package io.zeebe.broker.exporter.processor;

import io.zeebe.exporter.spi.Configuration;
import io.zeebe.exporter.spi.Event;
import io.zeebe.exporter.spi.Exporter;
import io.zeebe.logstreams.log.LoggedEvent;
import io.zeebe.logstreams.processor.EventProcessor;
import io.zeebe.logstreams.processor.StreamProcessor;
import io.zeebe.logstreams.spi.SnapshotSupport;
import io.zeebe.msgpack.UnpackedObject;
import io.zeebe.protocol.impl.RecordMetadata;

public class ExporterStreamProcessor implements StreamProcessor {
  private final Exporter exporter;
  private final Configuration config;
  private final ExporterEventProcessor eventProcessor;

  public ExporterStreamProcessor(final Exporter exporter) {
    this.exporter = exporter;
    this.config = exporter.getConfiguration();
    this.eventProcessor = new ExporterEventProcessor(exporter);
  }

  @Override
  public SnapshotSupport getStateResource() {
    return null;
  }

  @Override
  public EventProcessor onEvent(LoggedEvent event) {

    return eventProcessor;
  }

  @Override
  public void onClose() {}
}
