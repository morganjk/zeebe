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
package io.zeebe.broker.workflow.processor;

import io.zeebe.logstreams.snapshot.SerializableWrapper;
import io.zeebe.logstreams.spi.ComposableSnapshotSupport;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class ComposeableSerializableSnapshot<T extends Serializable> extends SerializableWrapper<T>
    implements ComposableSnapshotSupport {

  public ComposeableSerializableSnapshot(T object) {
    super(object);
  }

  @Override
  public long snapshotSize() {

    // this is very ineffecient, but disappears when we move to rocksdb
    final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
    try {
      final ObjectOutputStream oos = new ObjectOutputStream(byteStream);
      oos.writeObject(object);
      oos.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return byteStream.toByteArray().length;
  }
}
