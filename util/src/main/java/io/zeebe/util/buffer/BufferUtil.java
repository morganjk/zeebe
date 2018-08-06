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
package io.zeebe.util.buffer;

import static io.zeebe.util.EnsureUtil.ensureGreaterThanOrEqual;
import static io.zeebe.util.StringUtil.getBytes;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.agrona.DirectBuffer;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

public final class BufferUtil {
  public static final int NO_WRAP = 1;
  public static final int DEFAULT_WRAP = 16; // bytes

  private static final char[] HEX_CODE = "0123456789ABCDEF".toCharArray();

  private BufferUtil() { // avoid instantiation of util class
  }

  public static String bufferAsString(final DirectBuffer buffer) {
    return bufferAsString(buffer, 0, buffer.capacity());
  }

  public static String bufferAsString(
      final DirectBuffer buffer, final int offset, final int length) {
    final byte[] bytes = new byte[length];

    buffer.getBytes(offset, bytes);

    return new String(bytes, StandardCharsets.UTF_8);
  }

  public static DirectBuffer wrapString(String argument) {
    return new UnsafeBuffer(getBytes(argument));
  }

  /** Compare the given buffers. */
  public static boolean equals(DirectBuffer buffer1, DirectBuffer buffer2) {
    if (buffer1 instanceof UnsafeBuffer && buffer2 instanceof UnsafeBuffer) {
      return buffer1.equals(buffer2);
    } else if (buffer1 instanceof ExpandableArrayBuffer
        && buffer2 instanceof ExpandableArrayBuffer) {
      return buffer1.equals(buffer2);
    } else {
      return contentsEqual(buffer1, buffer2);
    }
  }

  /** byte-by-byte comparison of two buffers */
  public static boolean contentsEqual(DirectBuffer buffer1, DirectBuffer buffer2) {

    if (buffer1.capacity() == buffer2.capacity()) {
      boolean equal = true;

      for (int i = 0; i < buffer1.capacity() && equal; i++) {
        equal &= buffer1.getByte(i) == buffer2.getByte(i);
      }

      return equal;
    } else {
      return false;
    }
  }

  /**
   * Creates a new instance of the src buffer class and copies the underlying bytes.
   *
   * @param src the buffer to copy from
   * @return the new buffer instance
   */
  public static DirectBuffer cloneBuffer(final DirectBuffer src) {
    return cloneBuffer(src, 0, src.capacity());
  }

  /**
   * Creates a new instance of the src buffer class and copies the underlying bytes.
   *
   * @param src the buffer to copy from
   * @param offset the offset to start in the src buffer
   * @param length the number of bytes to clone
   * @return the new buffer instance
   */
  public static DirectBuffer cloneBuffer(
      final DirectBuffer src, final int offset, final int length) {
    final int availableBytes = src.capacity() - offset;

    ensureGreaterThanOrEqual("available bytes", availableBytes, length);

    if (src instanceof UnsafeBuffer) {
      final byte[] dst = new byte[length];
      src.getBytes(offset, dst);
      return new UnsafeBuffer(dst);
    } else if (src instanceof ExpandableArrayBuffer) {
      final ExpandableArrayBuffer dst = new ExpandableArrayBuffer(length);
      src.getBytes(offset, dst, 0, length);
      return dst;
    } else {
      throw new RuntimeException(
          "Unable to clone buffer of class " + src.getClass().getSimpleName());
    }
  }

  public static String bufferAsHexString(final BufferWriter writer) {
    return bufferAsHexString(writer, DEFAULT_WRAP);
  }

  public static String bufferAsHexString(final BufferWriter writer, final int wrap) {
    final byte[] bytes = new byte[writer.getLength()];
    final UnsafeBuffer buffer = new UnsafeBuffer(bytes);

    writer.write(buffer, 0);

    return bytesAsHexString(bytes, wrap);
  }

  public static String bufferAsHexString(final DirectBuffer buffer) {
    return bufferAsHexString(buffer, DEFAULT_WRAP);
  }

  public static String bufferAsHexString(final DirectBuffer buffer, final int wrap) {
    return bufferAsHexString(buffer, 0, buffer.capacity(), wrap);
  }

  public static String bufferAsHexString(
      final DirectBuffer buffer, final int offset, final int length) {
    return bufferAsHexString(buffer, offset, length, DEFAULT_WRAP);
  }

  public static String bufferAsHexString(
      final DirectBuffer buffer, final int offset, final int length, final int wrap) {
    final byte[] bytes = new byte[length];
    buffer.getBytes(offset, bytes, 0, length);

    return bytesAsHexString(bytes, wrap);
  }

  public static String bytesAsHexString(final byte[] bytes) {
    return bytesAsHexString(bytes, DEFAULT_WRAP);
  }

  public static String bytesAsHexString(final byte[] bytes, final int wrap) {
    final int length = bytes.length;

    final StringBuilder builder = new StringBuilder(length * 4);
    final StringBuilder hexBuilder = new StringBuilder(wrap * 3);
    final StringBuilder asciiBuilder = new StringBuilder(wrap);

    for (int line = 0; line <= (length / wrap); line++) {
      builder.append(String.format("0x%08x: ", line * wrap));
      for (int i = 0; i < wrap; i++) {
        final int index = (line * wrap) + i;

        if (index < length) {
          final byte b = bytes[index];
          hexBuilder.append(HEX_CODE[(b >> 4) & 0xF]).append(HEX_CODE[(b & 0xF)]).append(' ');

          // check if byte is ASCII character range other wise use . as placeholder
          if (b > 31 && b < 126) {
            asciiBuilder.append((char) b);
          } else {
            asciiBuilder.append('.');
          }
        } else {
          // padding
          hexBuilder.append("   ");
        }
      }
      builder
          .append(hexBuilder.toString())
          .append('|')
          .append(asciiBuilder.toString())
          .append("|\n");

      asciiBuilder.delete(0, asciiBuilder.length());
      hexBuilder.delete(0, hexBuilder.length());
    }

    return builder.toString();
  }

  public static byte[] bufferAsArray(DirectBuffer buffer) {
    byte[] array = null;

    if (buffer.byteArray() != null) {
      array = buffer.byteArray();
    } else {
      array = new byte[buffer.capacity()];
      buffer.getBytes(0, array);
    }
    return array;
  }

  public static MutableDirectBuffer wrapArray(byte[] array) {
    return new UnsafeBuffer(array);
  }

  /** Does not care about overflows; just for convenience of writing int literals */
  public static MutableDirectBuffer wrapBytes(int... bytes) {
    return new UnsafeBuffer(intArrayToByteArray(bytes));
  }

  protected static byte[] intArrayToByteArray(int[] input) {
    final byte[] result = new byte[input.length];
    for (int i = 0; i < input.length; i++) {
      result[i] = (byte) input[i];
    }

    return result;
  }

  public static ByteBuffer toByteBuffer(final DirectBuffer buffer) {
    final ByteBuffer converted;

    if (buffer.byteBuffer() != null) {
      final int offset = buffer.wrapAdjustment();
      converted = buffer.byteBuffer().duplicate();
      converted.position(offset);
      converted.limit(buffer.capacity() + offset);
    } else {
      converted = ByteBuffer.wrap(buffer.byteArray(), buffer.wrapAdjustment(), buffer.capacity());
    }

    return converted;
  }
}
