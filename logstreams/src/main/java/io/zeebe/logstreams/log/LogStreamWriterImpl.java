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
package io.zeebe.logstreams.log;

import static io.zeebe.dispatcher.impl.log.LogBufferAppender.RESULT_PADDING_AT_END_OF_PARTITION;
import static io.zeebe.logstreams.impl.LogEntryDescriptor.headerLength;
import static io.zeebe.logstreams.impl.LogEntryDescriptor.metadataOffset;
import static io.zeebe.logstreams.impl.LogEntryDescriptor.setKey;
import static io.zeebe.logstreams.impl.LogEntryDescriptor.setMetadataLength;
import static io.zeebe.logstreams.impl.LogEntryDescriptor.setPosition;
import static io.zeebe.logstreams.impl.LogEntryDescriptor.setProducerId;
import static io.zeebe.logstreams.impl.LogEntryDescriptor.setRaftTerm;
import static io.zeebe.logstreams.impl.LogEntryDescriptor.setSourceEventPosition;
import static io.zeebe.logstreams.impl.LogEntryDescriptor.setTimestamp;
import static io.zeebe.logstreams.impl.LogEntryDescriptor.valueOffset;
import static org.agrona.BitUtil.SIZE_OF_LONG;

import io.zeebe.dispatcher.ClaimedFragment;
import io.zeebe.dispatcher.Dispatcher;
import io.zeebe.dispatcher.impl.log.DataFrameDescriptor;
import io.zeebe.logstreams.impl.LogEntryDescriptor;
import io.zeebe.util.EnsureUtil;
import io.zeebe.util.buffer.BufferWriter;
import io.zeebe.util.buffer.DirectBufferWriter;
import io.zeebe.util.sched.clock.ActorClock;
import org.agrona.DirectBuffer;
import org.agrona.LangUtil;
import org.agrona.MutableDirectBuffer;

public class LogStreamWriterImpl implements LogStreamRecordWriter {
  protected final DirectBufferWriter metadataWriterInstance = new DirectBufferWriter();
  protected final DirectBufferWriter bufferWriterInstance = new DirectBufferWriter();
  protected final ClaimedFragment claimedFragment = new ClaimedFragment();

  private LogStream logStream;

  protected boolean positionAsKey;
  protected long key;

  protected long sourceRecordPosition = -1L;
  protected int producerId = -1;

  protected final short keyLength = SIZE_OF_LONG;

  protected BufferWriter metadataWriter;

  protected BufferWriter valueWriter;

  public LogStreamWriterImpl() {}

  public LogStreamWriterImpl(LogStream log) {
    wrap(log);
  }

  @Override
  public void wrap(LogStream log) {
    this.logStream = log;
    reset();
  }

  @Override
  public LogStreamRecordWriter keyNull() {
    return key(LogEntryDescriptor.KEY_NULL_VALUE);
  }

  @Override
  public LogStreamRecordWriter positionAsKey() {
    positionAsKey = true;
    return this;
  }

  @Override
  public LogStreamRecordWriter key(long key) {
    this.key = key;
    return this;
  }

  public LogStreamRecordWriter sourceRecordPosition(long position) {
    this.sourceRecordPosition = position;
    return this;
  }

  @Override
  public LogStreamRecordWriter producerId(int producerId) {
    this.producerId = producerId;
    return this;
  }

  @Override
  public LogStreamRecordWriter metadata(DirectBuffer buffer, int offset, int length) {
    metadataWriterInstance.wrap(buffer, offset, length);
    return this;
  }

  @Override
  public LogStreamRecordWriter metadata(DirectBuffer buffer) {
    return metadata(buffer, 0, buffer.capacity());
  }

  @Override
  public LogStreamRecordWriter metadataWriter(BufferWriter writer) {
    this.metadataWriter = writer;
    return this;
  }

  @Override
  public LogStreamRecordWriter value(DirectBuffer value, int valueOffset, int valueLength) {
    return valueWriter(bufferWriterInstance.wrap(value, valueOffset, valueLength));
  }

  @Override
  public LogStreamRecordWriter value(DirectBuffer value) {
    return value(value, 0, value.capacity());
  }

  @Override
  public LogStreamRecordWriter valueWriter(BufferWriter writer) {
    this.valueWriter = writer;
    return this;
  }

  @Override
  public void reset() {
    positionAsKey = false;
    key = LogEntryDescriptor.KEY_NULL_VALUE;
    metadataWriter = metadataWriterInstance;
    valueWriter = null;
    sourceRecordPosition = -1L;
    producerId = -1;

    bufferWriterInstance.reset();
    metadataWriterInstance.reset();
  }

  @Override
  public long tryWrite() {
    EnsureUtil.ensureNotNull("value", valueWriter);

    long result = -1;

    final int valueLength = valueWriter.getLength();
    final int metadataLength = metadataWriter.getLength();

    // claim fragment in log write buffer
    final long claimedPosition = claimLogEntry(valueLength, metadataLength);

    if (claimedPosition >= 0) {
      try {
        final MutableDirectBuffer writeBuffer = claimedFragment.getBuffer();
        final int bufferOffset = claimedFragment.getOffset();

        final long keyToWrite = positionAsKey ? claimedPosition : key;

        // write log entry header
        setPosition(writeBuffer, bufferOffset, claimedPosition);
        setRaftTerm(writeBuffer, bufferOffset, logStream.getTerm());
        setProducerId(writeBuffer, bufferOffset, producerId);
        setSourceEventPosition(writeBuffer, bufferOffset, sourceRecordPosition);
        setKey(writeBuffer, bufferOffset, keyToWrite);
        setTimestamp(writeBuffer, bufferOffset, ActorClock.currentTimeMillis());
        setMetadataLength(writeBuffer, bufferOffset, (short) metadataLength);

        if (metadataLength > 0) {
          metadataWriter.write(writeBuffer, metadataOffset(bufferOffset));
        }

        // write log entry
        valueWriter.write(writeBuffer, valueOffset(bufferOffset, metadataLength));

        result = claimedPosition;
        claimedFragment.commit();
      } catch (Exception e) {
        claimedFragment.abort();
        LangUtil.rethrowUnchecked(e);
      } finally {
        reset();
      }
    }

    return result;
  }

  private long claimLogEntry(final int valueLength, final int metadataLength) {
    final int framedLength = valueLength + headerLength(metadataLength);

    long claimedPosition = -1;
    final Dispatcher logWriteBuffer = logStream.getWriteBuffer();
    final int logId = logStream.getPartitionId();

    do {

      claimedPosition = logWriteBuffer.claim(claimedFragment, framedLength, logId);
    } while (claimedPosition == RESULT_PADDING_AT_END_OF_PARTITION);

    return claimedPosition - DataFrameDescriptor.alignedFramedLength(framedLength);
  }
}
