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
package io.zeebe.logstreams.fs.snapshot;

import static io.zeebe.util.StringUtil.getBytes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.zeebe.logstreams.impl.snapshot.fs.FsReadableSnapshot;
import io.zeebe.logstreams.impl.snapshot.fs.FsSnapshotStorage;
import io.zeebe.logstreams.impl.snapshot.fs.FsSnapshotStorageConfiguration;
import io.zeebe.logstreams.impl.snapshot.fs.FsSnapshotWriter;
import io.zeebe.logstreams.impl.snapshot.fs.FsTemporarySnapshotWriter;
import java.io.File;
import java.nio.file.Files;
import java.security.MessageDigest;
import org.agrona.BitUtil;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class FsTemporarySnapshotWriterTest {
  private static final byte[] SNAPSHOT_DATA = getBytes("snapshot");

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  private FsSnapshotStorageConfiguration config;
  private File snapshotFile;
  private File checksumFile;
  private File temporaryFile;
  private FsTemporarySnapshotWriter writer;
  private FsReadableSnapshot lastSnapshot;

  @Before
  public void init() throws Exception {
    final String snapshotRootPath = tempFolder.getRoot().getAbsolutePath();

    config = new FsSnapshotStorageConfiguration();
    config.setRootPath(snapshotRootPath);

    lastSnapshot = createLastSnapshot();
    temporaryFile = tempFolder.newFile("test1239489485.tmp");
    snapshotFile = new File(tempFolder.getRoot(), "snapshot-2.snapshot");
    checksumFile = new File(tempFolder.getRoot(), "checksum-2.sha1");

    writer =
        new FsTemporarySnapshotWriter(
            config, temporaryFile, checksumFile, snapshotFile, lastSnapshot);
  }

  @Test
  public void shouldHaveTemporaryFileAsDataFile() {
    // then
    assertThat(writer.getDataFile()).isEqualTo(temporaryFile);
    assertThat(writer.getChecksumFile()).isEqualTo(checksumFile);
    assertThat(writer.getSnapshotFile()).isEqualTo(snapshotFile);
  }

  @Test
  public void shouldWriteDataToTemporaryFile() throws Exception {
    // when
    writer.getOutputStream().write(SNAPSHOT_DATA);
    writer.getOutputStream().flush();

    // then
    assertThat(Files.readAllBytes(temporaryFile.toPath())).isEqualTo(SNAPSHOT_DATA);
  }

  @Test
  public void shouldRemoveAllFilesOnAbort() {
    // when
    writer.abort();

    // then
    assertThat(temporaryFile).doesNotExist();
    assertThat(snapshotFile).doesNotExist();
    assertThat(checksumFile).doesNotExist();
  }

  @Test
  public void shouldOverwriteSnapshotFileWithTemporaryFileOnCommit() throws Exception {
    // given
    final byte[] checksumBytes =
        MessageDigest.getInstance(config.getChecksumAlgorithm()).digest(SNAPSHOT_DATA);
    final String checksum = BitUtil.toHex(checksumBytes);

    // when
    writer.getOutputStream().write(SNAPSHOT_DATA);
    writer.commit();

    // then
    assertThat(temporaryFile).doesNotExist();
    assertThat(Files.readAllBytes(snapshotFile.toPath())).isEqualTo(SNAPSHOT_DATA);
    assertThat(new String(Files.readAllBytes(checksumFile.toPath())))
        .isEqualTo(config.checksumContent(checksum, snapshotFile.getName()));
    assertThat(lastSnapshot.getDataFile()).doesNotExist();
    assertThat(lastSnapshot.getChecksumFile()).doesNotExist();
  }

  @Test
  public void shouldStillHaveLastSnapshotIfFailToMoveTemporaryFile() throws Exception {
    // given
    final byte[] badChecksum = new byte[] {0};

    // when
    writer.getOutputStream().write(SNAPSHOT_DATA);
    assertThatThrownBy(() -> writer.validateAndCommit(badChecksum))
        .isInstanceOf(RuntimeException.class);

    // then
    assertThat(temporaryFile).doesNotExist();
    assertThat(checksumFile).doesNotExist();
    assertThat(snapshotFile).doesNotExist();
    assertThat(lastSnapshot.getChecksumFile()).exists();
    assertThat(lastSnapshot.getDataFile()).exists();
  }

  private FsReadableSnapshot createLastSnapshot() throws Exception {
    final FsSnapshotStorage storage = new FsSnapshotStorage(config);
    final FsSnapshotWriter writer = storage.createSnapshot("snapshot", 1);
    writer.getOutputStream().write(SNAPSHOT_DATA);
    writer.commit();

    return storage.getLastSnapshot("snapshot");
  }
}
