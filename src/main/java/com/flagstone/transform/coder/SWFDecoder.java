/*
 * SWFDecoder.java
 * Transform
 *
 * Copyright (c) 2001-2010 Flagstone Software Ltd. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *  * Neither the name of Flagstone Software Ltd. nor the names of its
 *    contributors may be used to endorse or promote products derived from this
 *    software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package com.flagstone.transform.coder;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Stack;


/**
 * SWFDecoder wraps an InputStream with a buffer to reduce the amount of
 * memory required to decode a movie and to improve efficiency by reading
 * data from a file or external source in blocks.
 */
@SuppressWarnings("PMD.TooManyMethods")
public final class SWFDecoder {
    /** The default size, in bytes, for the internal buffer. */
    public static final int BUFFER_SIZE = 4096;

    /** The default size, in bytes, for the reading strings. */
    private static final int STR_BUFFER_SIZE = 1024;
    /** Bit mask applied to bytes when converting to unsigned integers. */
    private static final int BYTE_MASK = 255;
    /** Number of bits to shift when aligning a value to the second byte. */
    private static final int TO_BYTE1 = 8;
    /** Number of bits to shift when aligning a value to the third byte. */
    private static final int TO_BYTE2 = 16;
    /** Number of bits to shift when aligning a value to the fourth byte. */
    private static final int TO_BYTE3 = 24;
    /** Number of bits in an int. */
    private static final int BITS_PER_INT = 32;
    /** Number of bits in a byte. */
    private static final int BITS_PER_BYTE = 8;
    /** Right shift to convert number of bits to number of bytes. */
    private static final int BITS_TO_BYTES = 3;
    /** Left shift to convert number of bytes to number of bits. */
    private static final int BYTES_TO_BITS = 3;

    /** The underlying input stream. */
    private final transient InputStream stream;
    /** The buffer for data read from the stream. */
    private final transient byte[] buffer;
    /** A buffer used for reading null terminated strings. */
    private transient byte[] stringBuffer;
    /** The character encoding used for strings. */
    private transient String encoding;
    /** Stack for storing file locations. */
    private final transient Stack<Integer>locations;
    /** The position of the buffer relative to the start of the stream. */
    private transient int pos;
    /** The position from the start of the buffer. */
    private transient int index;
    /** The offset in bits in the current buffer location. */
    private transient int offset;
    /** The number of bytes available in the current buffer. */
    private transient int size;

    /**
     * Create a new SWFDecoder for the underlying InputStream with the
     * specified buffer size.
     *
     * @param streamIn the stream from which data will be read.
     * @param length the size in bytes of the buffer.
     */
    public SWFDecoder(final InputStream streamIn, final int length) {
        stream = streamIn;
        buffer = new byte[length];
        stringBuffer = new byte[STR_BUFFER_SIZE];
        encoding = "UTF-8";
        locations = new Stack<Integer>();
        pos = 0;
    }

    /**
     * Create a new SWFDecoder for the underlying InputStream using the
     * default buffer size.
     *
     * @param streamIn the stream from which data will be read.
     */
    public SWFDecoder(final InputStream streamIn) {
        stream = streamIn;
        buffer = new byte[BUFFER_SIZE];
        stringBuffer = new byte[BUFFER_SIZE];
        encoding = "UTF-8";
        locations = new Stack<Integer>();
        pos = 0;
    }

    /**
     * Sets the character encoding scheme used when encoding or decoding
     * strings.
     *
     * @param charSet
     *            the name of the character set used to encode strings.
     */
    public void setEncoding(final String charSet) {
        encoding = charSet;
    }

    /**
     * Remember the current position.
     */
    public void mark() {
        locations.push(pos + index);
    }

    /**
     * Discard the last saved position.
     */
    public void unmark() {
        locations.pop();
    }

    /**
     * Compare the number of bytes read since the last saved position. The last
     * saved position is discarded.
     *
     * @param expected the expected number of bytes read.
     *
     * @throws IOException if the number of bytes read is different from the
     * expected number.
     */
    public void unmark(final int expected) throws IOException {
        if (bytesRead() != expected) {
            throw new CoderException(locations.peek(), expected,
                    bytesRead() - expected);
        }
        locations.pop();
    }

    /**
     * Get the number of bytes read from the last saved position.
     *
     * @return the number of bytes read since the mark() method was last called.
     */
    public int bytesRead() {
        int count;
        if (pos == 0) {
            count = index - locations.peek();
        } else {
            count = (pos + index) - locations.peek();
        }
        return count;
    }

    /**
     * Skips over and discards n bytes of data.
     *
     * @param count the number of bytes to skip.
     *
     * @throws IOException if an error occurs reading from the underlying
     * input stream.
     */
    public void skip(final int count) throws IOException {
        final int diff = size - index;
        if (count < diff) {
            index += count;
        } else {
            final int bytesSkipped = diff;
            stream.skip(count - bytesSkipped);
            pos += count - bytesSkipped;
            index = size;
            fill();
        }
    }

    /**
     * Count the number of consecutive bytes from the current buffer position
     * that match the specified value, up to the end of the buffer.
     *
     * This method is used when dealing with badly formed tags (DefineText,
     * DefineText2) that often contain blocks of zeroes, but otherwise are
     * valid.
     *
     * @param value the value to compare.
     * @return the number of matching bytes.
     *
     * @throws IOException if an error occurs reading from the underlying
     * input stream.
     */
    public int count(final int value) throws IOException {
        int count = 0;
        fill();
        for (int i = index; i < size; i++) {
            if ((buffer[index + i] & BYTE_MASK) == value) {
                count++;
            } else {
                break;
            }
        }
        return count;
    }

    /**
     * Changes the location to the next byte boundary.
     */
    public void alignToByte() {
        if (offset > 0) {
            index += 1;
            offset = 0;
        }
    }

    /**
     * Fill the internal buffer. Any unread bytes are copied to the start of
     * the buffer and the remaining space is filled with data from the
     * underlying stream.
     *
     * @throws IOException if an error occurs reading from the underlying
     * input stream.
     */
    private void fill() throws IOException {
        final int diff = size - index;
        pos += index;

        if (index < size) {
            for (int i = 0; i < diff; i++) {
                buffer[i] = buffer[index++];
            }
        }

        int bytesRead = 0;
        int bytesToRead = buffer.length - diff;

        index = diff;
        size = diff;

        do {
            bytesRead = stream.read(buffer, index, bytesToRead);
            if (bytesRead == -1) {
                bytesToRead = 0;
            } else {
                index += bytesRead;
                size += bytesRead;
                bytesToRead -= bytesRead;
            }
        } while (bytesToRead > 0);

        index = 0;
    }

    /**
     * Read a bit field.
     *
     * @param numberOfBits
     *            the number of bits to read.
     *
     * @param signed
     *            indicates whether the integer value read is signed.
     *
     * @return the value read.
     *
     * @throws IOException if an error occurs reading from the underlying
     * input stream.
     */
    public int readBits(final int numberOfBits, final boolean signed)
            throws IOException {

        int pointer = (index << BYTES_TO_BITS) + offset;

        if (((size << BYTES_TO_BITS) - pointer) < numberOfBits) {
            fill();
            pointer = (index << BYTES_TO_BITS) + offset;
        }

        int value = 0;

        if (numberOfBits > 0) {

            for (int i = BITS_PER_INT; (i > 0)
                    && (index < buffer.length); i -= BITS_PER_BYTE) {
                value |= (buffer[index++] & BYTE_MASK) << (i - BITS_PER_BYTE);
            }

            value <<= offset;

            if (signed) {
                value >>= BITS_PER_INT - numberOfBits;
            } else {
                value >>>= BITS_PER_INT - numberOfBits;
            }

            pointer += numberOfBits;
            index = pointer >>> BITS_TO_BYTES;
            offset = pointer & Coder.LOWEST3;
        }

        return value;
    }

    /**
     * Read an unsigned byte but do not advance the internal pointer.
     *
     * @return an 8-bit unsigned value.
     *
     * @throws IOException if an error occurs reading from the underlying
     * input stream.
     */
    public int scanByte() throws IOException {
        if (size - index < 1) {
            fill();
        }
        return buffer[index] & BYTE_MASK;
    }

    /**
     * Read an unsigned byte.
     *
     * @return an 8-bit unsigned value.
     *
     * @throws IOException if an error occurs reading from the underlying
     * input stream.
     */
    public int readByte() throws IOException {
        if (size - index < 1) {
            fill();
        }
        return buffer[index++] & BYTE_MASK;
    }

    /**
     * Reads an array of bytes.
     *
     * @param bytes
     *            the array that will contain the bytes read.
     *
     * @return the array of bytes.
     *
     * @throws IOException if an error occurs reading from the underlying
     * input stream.
     */
    public byte[] readBytes(final byte[] bytes) throws IOException {
        final int wanted = bytes.length;
        int dest = 0;
        int read = 0;

        int available;
        int remaining;

        while (read < wanted) {
            available = size - index;
            remaining = wanted - read;
            if (available > remaining) {
                available = remaining;
            }
            System.arraycopy(buffer, index, bytes, dest, available);
            read += available;
            index += available;
            dest += available;

            if (index == size) {
                fill();
            }
        }
        return bytes;
    }

    /**
     * Read a string using the default character set defined in the decoder.
     *
     * @param length
     *            the number of bytes to read.
     *
     * @return the decoded string.
     *
     * @throws IOException if an error occurs reading from the underlying
     * input stream.
     */
    public String readString(final int length) throws IOException {
        byte[] bytes = new byte[length];
        readBytes(bytes);
        final int len;
        if (bytes[length - 1] == 0) {
            len = length - 1;
        } else {
            len = length;
        }
        return new String(bytes, 0, len, encoding);
    }

    /**
     * Read a null-terminated string using the default character set defined in
     * the decoder.
     *
     * @return the decoded string.
     *
     * @throws IOException if an error occurs reading from the underlying
     * input stream.
     */
    public String readString() throws IOException {
        int start = index;
        int length = 0;
        int available;
        int dest = 0;
        boolean finished = false;
        int count;

        while (!finished) {
            available = size - index;
            if (available == 0) {
                fill();
                available = size - index;
            }
            start = index;
            count = 0;
            for (int i = 0; i < available; i++) {
                if (buffer[index++] == 0) {
                    finished = true;
                    break;
                } else {
                    length++;
                    count++;
                }
            }
            if (stringBuffer.length < length) {
                stringBuffer = Arrays.copyOf(stringBuffer, length << 2);
            }
            System.arraycopy(buffer, start, stringBuffer, dest, count);
            dest += length;
        }
        return new String(stringBuffer, 0, length, encoding);
    }

    /**
     * Read an unsigned 16-bit integer.
     *
     * @return the value read.
     *
     * @throws IOException if an error occurs reading from the underlying
     * input stream.
     */
    public int scanUnsignedShort() throws IOException {
        if (size - index < 2) {
            fill();
        }
        int value = buffer[index] & BYTE_MASK;
        value |= (buffer[index + 1] & BYTE_MASK) << TO_BYTE1;
        return value;
    }

    /**
     * Read an unsigned 16-bit integer.
     *
     * @return the value read.
     *
     * @throws IOException if an error occurs reading from the underlying
     * input stream.
     */
    public int readUnsignedShort() throws IOException {
        if (size - index < 2) {
            fill();
        }
        int value = buffer[index++] & BYTE_MASK;
        value |= (buffer[index++] & BYTE_MASK) << TO_BYTE1;
        return value;
    }

    /**
     * Read an unsigned 16-bit integer.
     *
     * @return the value read.
     *
     * @throws IOException if an error occurs reading from the underlying
     * input stream.
     */
    public int readSignedShort() throws IOException {
        if (size - index < 2) {
            fill();
        }
        int value = buffer[index++] & BYTE_MASK;
        value |= buffer[index++] << TO_BYTE1;
        return value;
    }

    /**
     * Read an unsigned 32-bit integer.
     *
     * @return the value read.
     *
     * @throws IOException if an error occurs reading from the underlying
     * input stream.
     */
    public int readInt() throws IOException {
        if (size - index < 4) {
            fill();
        }
        int value = buffer[index++] & BYTE_MASK;
        value |= (buffer[index++] & BYTE_MASK) << TO_BYTE1;
        value |= (buffer[index++] & BYTE_MASK) << TO_BYTE2;
        value |= (buffer[index++] & BYTE_MASK) << TO_BYTE3;
        return value;
    }

    /**
     * Read a 32-bit unsigned integer, encoded using a variable number of bytes.
     *
     * @return the value read.
     *
     * @throws IOException if an error occurs reading from the underlying
     * input stream.
     */
    public int readVarInt() throws IOException {

        if (size - index < 5) {
            fill();
        }

        int value = buffer[index++] & BYTE_MASK;
        final int mask = -1;
        int test = Coder.BIT7;
        int step = Coder.VAR_INT_SHIFT;

        while ((value & test) != 0) {
            value = ((buffer[index++] & BYTE_MASK) << step)
                + (value & mask >>> (32 - step));
            test <<= Coder.VAR_INT_SHIFT;
            step += Coder.VAR_INT_SHIFT;
        }
        return value;
    }

    private static final int HALF_SIGN_SHIFT = 15;
    private static final int HALF_EXP_SHIFT = 10;
    private static final int HALF_EXP_OFFSET = 15;
    private static final int HALF_EXP_MAX = 31;

    private static final int SIGN_SHIFT = 31;
    private static final int EXP_SHIFT = 23;
    private static final int EXP_MAX = 127;
    private static final int MANT_SHIFT = 13;
    private static final int INFINITY = 0x7f800000;

    /**
     * Read a single-precision floating point number.
     *
     * @return the value.
     *
     * @throws IOException if an error occurs reading from the underlying
     * input stream.
     */
    public float readHalf() throws IOException {
        final int bits = readUnsignedShort();
        final int sign = (bits >> HALF_SIGN_SHIFT) & Coder.BIT0;
        int exp = (bits >> HALF_EXP_SHIFT) & Coder.LOWEST5;
        int mantissa = bits & Coder.LOWEST10;
        float value;

        if (exp == 0) {
            if (mantissa == 0) { // Plus or minus zero
                value = Float.intBitsToFloat(sign << SIGN_SHIFT);
            } else { // Denormalized number -- renormalize it
                while ((mantissa & Coder.BIT10) == 0) {
                    mantissa <<= 1;
                    exp -=  1;
                }
                exp += 1;
                exp = exp + (EXP_MAX - HALF_EXP_OFFSET);
                mantissa &= ~Coder.BIT10;
                mantissa = mantissa << MANT_SHIFT;
                value = Float.intBitsToFloat((sign << SIGN_SHIFT)
                        | (exp << EXP_SHIFT) | mantissa);
            }
        } else if (exp == HALF_EXP_MAX) {
            if (mantissa == 0) { // Inf
                value = Float.intBitsToFloat((sign << SIGN_SHIFT) | INFINITY);
            } else { // NaN
                value = Float.intBitsToFloat((sign << SIGN_SHIFT)
                        | INFINITY | (mantissa << MANT_SHIFT));
            }
        } else {
            exp = exp + (EXP_MAX - HALF_EXP_OFFSET);
            mantissa = mantissa << MANT_SHIFT;
            value = Float.intBitsToFloat((sign << SIGN_SHIFT)
                    | (exp << EXP_SHIFT) | mantissa);
        }
        return value;
    }
}
