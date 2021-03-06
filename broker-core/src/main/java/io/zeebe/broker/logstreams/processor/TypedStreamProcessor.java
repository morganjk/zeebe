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
package io.zeebe.broker.logstreams.processor;

import io.zeebe.logstreams.log.LogStream;
import io.zeebe.logstreams.log.LogStreamRecordWriter;
import io.zeebe.logstreams.log.LoggedEvent;
import io.zeebe.logstreams.processor.EventLifecycleContext;
import io.zeebe.logstreams.processor.EventProcessor;
import io.zeebe.logstreams.processor.StreamProcessor;
import io.zeebe.logstreams.processor.StreamProcessorContext;
import io.zeebe.logstreams.spi.SnapshotSupport;
import io.zeebe.logstreams.state.StateController;
import io.zeebe.msgpack.UnpackedObject;
import io.zeebe.protocol.clientapi.ValueType;
import io.zeebe.protocol.impl.RecordMetadata;
import io.zeebe.transport.ServerOutput;
import io.zeebe.util.ReflectUtil;
import io.zeebe.util.sched.ActorControl;
import io.zeebe.util.sched.future.ActorFuture;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

@SuppressWarnings({"unchecked"})
public class TypedStreamProcessor implements StreamProcessor {

  // TODO: remove once we remove snapshot support
  protected StateController stateController;

  protected final SnapshotSupport snapshotSupport;
  protected final ServerOutput output;
  protected final RecordProcessorMap recordProcessors;
  protected final List<StreamProcessorLifecycleAware> lifecycleListeners = new ArrayList<>();
  private final KeyGenerator keyGenerator;

  protected final RecordMetadata metadata = new RecordMetadata();
  protected final EnumMap<ValueType, Class<? extends UnpackedObject>> eventRegistry;
  protected final EnumMap<ValueType, UnpackedObject> eventCache;

  protected final TypedEventImpl typedEvent = new TypedEventImpl();
  private final TypedStreamEnvironment environment;

  protected DelegatingEventProcessor eventProcessorWrapper;
  protected ActorControl actor;
  private StreamProcessorContext streamProcessorContext;

  public TypedStreamProcessor(
      StateController stateController,
      SnapshotSupport snapshotSupport,
      ServerOutput output,
      RecordProcessorMap recordProcessors,
      List<StreamProcessorLifecycleAware> lifecycleListeners,
      EnumMap<ValueType, Class<? extends UnpackedObject>> eventRegistry,
      KeyGenerator keyGenerator,
      TypedStreamEnvironment environment) {
    this.stateController = stateController;
    this.snapshotSupport = snapshotSupport;
    this.output = output;
    this.recordProcessors = recordProcessors;
    this.keyGenerator = keyGenerator;
    recordProcessors.values().forEachRemaining(p -> this.lifecycleListeners.add(p));

    this.lifecycleListeners.addAll(lifecycleListeners);

    this.eventCache = new EnumMap<>(ValueType.class);

    eventRegistry.forEach((t, c) -> eventCache.put(t, ReflectUtil.newInstance(c)));
    this.eventRegistry = eventRegistry;
    this.environment = environment;
  }

  @Override
  public void onOpen(StreamProcessorContext context) {
    this.eventProcessorWrapper =
        new DelegatingEventProcessor(
            context.getId(), output, context.getLogStream(), eventRegistry, keyGenerator);

    this.actor = context.getActorControl();
    this.streamProcessorContext = context;
    lifecycleListeners.forEach(e -> e.onOpen(this));
  }

  @Override
  public void onRecovered() {
    lifecycleListeners.forEach(e -> e.onRecovered(this));
  }

  @Override
  public void onClose() {
    lifecycleListeners.forEach(e -> e.onClose());
  }

  @Override
  public SnapshotSupport getStateResource() {
    return snapshotSupport;
  }

  @Override
  public StateController getStateController() {
    return stateController;
  }

  @Override
  public EventProcessor onEvent(LoggedEvent event) {
    metadata.reset();
    event.readMetadata(metadata);

    final TypedRecordProcessor<?> currentProcessor =
        recordProcessors.get(
            metadata.getRecordType(), metadata.getValueType(), metadata.getIntent().value());

    if (currentProcessor != null) {
      final UnpackedObject value = eventCache.get(metadata.getValueType());
      value.reset();
      event.readValue(value);

      typedEvent.wrap(event, metadata, value);
      eventProcessorWrapper.wrap(currentProcessor, typedEvent);
      return eventProcessorWrapper;
    } else {
      return null;
    }
  }

  public MetadataFilter buildTypeFilter() {
    return m ->
        recordProcessors.containsKey(m.getRecordType(), m.getValueType(), m.getIntent().value());
  }

  public ActorFuture<Void> runAsync(Runnable runnable) {
    return actor.call(runnable);
  }

  protected static class DelegatingEventProcessor implements EventProcessor {

    protected final int streamProcessorId;
    protected final LogStream logStream;
    protected final TypedStreamWriterImpl writer;
    protected final TypedResponseWriterImpl responseWriter;

    protected TypedRecordProcessor<?> eventProcessor;
    protected TypedEventImpl event;
    private SideEffectProducer sideEffectProducer;

    public DelegatingEventProcessor(
        int streamProcessorId,
        ServerOutput output,
        LogStream logStream,
        EnumMap<ValueType, Class<? extends UnpackedObject>> eventRegistry,
        KeyGenerator keyGenerator) {
      this.streamProcessorId = streamProcessorId;
      this.logStream = logStream;
      this.writer = new TypedStreamWriterImpl(logStream, eventRegistry, keyGenerator);
      this.responseWriter = new TypedResponseWriterImpl(output, logStream.getPartitionId());
    }

    public void wrap(TypedRecordProcessor<?> eventProcessor, TypedEventImpl event) {
      this.eventProcessor = eventProcessor;
      this.event = event;
    }

    @Override
    public void processEvent(EventLifecycleContext ctx) {
      writer.reset();
      responseWriter.reset();

      this.writer.configureSourceContext(streamProcessorId, event.getPosition());

      // default side effect is responses; can be changed by processor
      sideEffectProducer = responseWriter;

      eventProcessor.processRecord(event, responseWriter, writer, this::setSideEffectProducer, ctx);
    }

    public void setSideEffectProducer(SideEffectProducer sideEffectProducer) {
      this.sideEffectProducer = sideEffectProducer;
    }

    @Override
    public boolean executeSideEffects() {
      return sideEffectProducer.flush();
    }

    @Override
    public long writeEvent(LogStreamRecordWriter writer) {
      return this.writer.flush();
    }
  }

  public ActorControl getActor() {
    return actor;
  }

  public StreamProcessorContext getStreamProcessorContext() {
    return streamProcessorContext;
  }

  public TypedStreamEnvironment getEnvironment() {
    return environment;
  }

  public KeyGenerator getKeyGenerator() {
    return keyGenerator;
  }
}
