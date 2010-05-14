/*
 * DefineSound.java
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

package com.flagstone.transform.sound;

import java.util.Arrays;

import com.flagstone.transform.SWF;
import com.flagstone.transform.coder.Coder;
import com.flagstone.transform.coder.CoderException;
import com.flagstone.transform.coder.Context;
import com.flagstone.transform.coder.DefineTag;
import com.flagstone.transform.coder.MovieTypes;
import com.flagstone.transform.coder.SWFDecoder;
import com.flagstone.transform.coder.SWFEncoder;
import com.flagstone.transform.exception.IllegalArgumentRangeException;
import com.flagstone.transform.exception.IllegalArgumentValueException;

/**
 * DefineSound is used to define a sound that will be played when a given event
 * occurs.
 *
 * <p>
 * Three different types of object are used to play an event sound:
 * </p>
 *
 * <ul>
 * <li>The DefineSound object that contains the sampled sound.</li>
 * <li>A SoundInfo object that defines how the sound fades in and out, whether
 * it repeats and also defines an envelope for more sophisticated control over
 * how the sound is played.</li>
 * <li>A StartSound object that signals the Flash Player to begin playing the
 * sound.</li>
 * </ul>
 *
 * <p>
 * Five encoded formats for the sound data are supported: NATIVE_PCM, PCM,
 * ADPCM, MP3 and NELLYMOSER.
 * </p>
 *
 * @see SoundInfo
 * @see StartSound
 */
public final class DefineSound implements DefineTag {

    private static final String FORMAT = "DefineSound: { identifier=%d;"
            + " format=%s; rate=%d; channelCount=%d; sampleSize=%d "
            + " sampleCount=%d }";

    private int format;
    private int rate;
    private int channelCount;
    private int sampleSize;
    private int sampleCount;
    private byte[] sound;
    private int identifier;

    private transient int length;

    /**
     * Creates and initialises a DefineSound object using values encoded
     * in the Flash binary format.
     *
     * @param coder
     *            an SWFDecoder object that contains the encoded Flash data.
     *
     * @throws CoderException
     *             if an error occurs while decoding the data.
     */
    public DefineSound(final SWFDecoder coder) throws CoderException {

        final int start = coder.getPointer();
        length = coder.readHeader();
        final int end = coder.getPointer() + (length << Coder.BYTES_TO_BITS);

        identifier = coder.readUI16();
        format = coder.readBits(4, false);

        switch (coder.readBits(2, false)) {
        case 0:
            rate = SoundRate.KHZ_5K;
            break;
        case 1:
            rate = SoundRate.KHZ_11K;
            break;
        case 2:
            rate = SoundRate.KHZ_22K;
            break;
        case 3:
            rate = SoundRate.KHZ_44K;
            break;
        default:
            rate = 0;
            break;
        }

        sampleSize = coder.readBits(1, false) + 1;
        channelCount = coder.readBits(1, false) + 1;
        sampleCount = coder.readUI32();

        sound = coder.readBytes(new byte[length - 7]);

        if (coder.getPointer() != end) {
            throw new CoderException(getClass().getName(),
                    start >> Coder.BITS_TO_BYTES, length,
                    (coder.getPointer() - end) >> Coder.BITS_TO_BYTES);
        }
    }

    /**
     * Creates a DefineSound object specifying the unique identifier and all the
     * parameters required to describe the sound.
     *
     * @param uid
     *            the unique identifier for this sound. Must be in the range
     *            1..65535.
     * @param aFormat
     *            the encoding format for the sound. For Flash 1 the formats may
     *            be one of the format: NATIVE_PCM, PCM or ADPCM. For Flash 4 or
     *            later include MP3 and Flash 6 or later include NELLYMOSER.
     * @param playbackRate
     *            the number of samples per second that the sound is played at ,
     *            either SoundRate.KHZ_5K, SoundRate.KHZ_11K, SoundRate.KHZ_22K or SoundRate.KHZ_44K.
     * @param channels
     *            the number of channels in the sound, must be either 1 (Mono)
     *            or 2 (Stereo).
     * @param size
     *            the size of an uncompressed sound sample in bits, must be
     *            either 8 or 16.
     * @param count
     *            the number of samples in the sound data.
     * @param bytes
     *            the sound data.
     */
    public DefineSound(final int uid, final SoundFormat aFormat,
            final int playbackRate, final int channels, final int size,
            final int count, final byte[] bytes) {
        setIdentifier(uid);
        setFormat(aFormat);
        setRate(playbackRate);
        setChannelCount(channels);
        setSampleSize(size);
        setSampleCount(count);
        setSound(bytes);
    }

    /**
     * Creates and initialises a DefineSound object using the values copied
     * from another DefineSound object.
     *
     * @param object
     *            a DefineSound object from which the values will be
     *            copied.
     */
    public DefineSound(final DefineSound object) {
        identifier = object.identifier;
        format = object.format;
        rate = object.rate;
        channelCount = object.channelCount;
        sampleSize = object.sampleSize;
        sampleCount = object.sampleCount;
        sound = object.sound;
    }

    /** {@inheritDoc} */
    public int getIdentifier() {
        return identifier;
    }

    /** {@inheritDoc} */
    public void setIdentifier(final int uid) {
        if ((uid < SWF.MIN_IDENTIFIER) || (uid > SWF.MAX_IDENTIFIER)) {
            throw new IllegalArgumentRangeException(
                    SWF.MIN_IDENTIFIER, SWF.MAX_IDENTIFIER, uid);
        }
        identifier = uid;
    }

    /**
     * Returns the compression format used.
     */
    public SoundFormat getFormat() {
        SoundFormat value;

        switch (format) {
        case 0:
            value = SoundFormat.NATIVE_PCM;
            break;
        case 1:
            value = SoundFormat.ADPCM;
            break;
        case 2:
            value = SoundFormat.MP3;
            break;
        case 3:
            value = SoundFormat.PCM;
            break;
        case 5:
            value = SoundFormat.NELLYMOSER_8K;
            break;
        case 6:
            value = SoundFormat.NELLYMOSER;
            break;
        case 11:
            value = SoundFormat.SPEEX;
            break;
        default:
            throw new IllegalStateException("Unsupported sound format.");
        }
        return value;
    }

    /**
     * Returns the rate at which the sound will be played, in Hz: SoundRate.KHZ_5K, SoundRate.KHZ_11K,
     * SoundRate.KHZ_22K or SoundRate.KHZ_44K.
     */
    public int getRate() {
        return rate;
    }

    /**
     * Returns the number of sound channels, 1 (Mono) or 2 (Stereo).
     */
    public int getChannelCount() {
        return channelCount;
    }

    /**
     * Returns the size of an uncompressed sample in bytes.
     */
    public int getSampleSize() {
        return sampleSize;
    }

    /**
     * Returns the number of samples in the sound data.
     */
    public int getSampleCount() {
        return sampleCount;
    }

    /**
     * Returns a copy of the sound data.
     */
    public byte[] getSound() {
        return Arrays.copyOf(sound, sound.length);
    }

    /**
     * Sets the compression format used.
     *
     * @param encoding
     *            the format for the sound.
     */
    public void setFormat(final SoundFormat encoding) {
        switch (encoding) {
        case NATIVE_PCM:
            format = 0;
            break;
        case ADPCM:
            format = 1;
            break;
        case MP3:
            format = 2;
            break;
        case PCM:
            format = 3;
            break;
        case NELLYMOSER_8K:
            format = 5;
            break;
        case NELLYMOSER:
            format = 6;
            break;
        case SPEEX:
            format = 11;
            break;
        default:
            throw new IllegalArgumentException("Unsupported sound format.");
        }
    }

    /**
     * Sets the sampling rate in Hertz.
     *
     * @param samplingRate
     *            the rate at which the sounds is played in Hz. Must be one of:
     *            SoundRate.KHZ_5K, SoundRate.KHZ_11K, SoundRate.KHZ_22K or SoundRate.KHZ_44K.
     */
    public void setRate(final int samplingRate) {
        if ((samplingRate != SoundRate.KHZ_5K)
                && (samplingRate != SoundRate.KHZ_11K)
                && (samplingRate != SoundRate.KHZ_22K)
                && (samplingRate != SoundRate.KHZ_44K)) {
            throw new IllegalArgumentValueException(
                    new int[] {SoundRate.KHZ_5K, SoundRate.KHZ_11K, SoundRate.KHZ_22K, SoundRate.KHZ_44K}, samplingRate);
        }
        rate = samplingRate;
    }

    /**
     * Sets the number of channels defined in the sound.
     *
     * @param channels
     *            the number of channels in the sound, must be either 1 (Mono)
     *            or 2 (Stereo).
     */
    public void setChannelCount(final int channels) {
        if ((channels < 1) || (channels > 2)) {
            throw new IllegalArgumentRangeException(1, 2, channels);
        }
        channelCount = channels;
    }

    /**
     * Sets the sample size in bytes.
     *
     * @param size
     *            the size of sound samples in bytes. Must be either 1 or 2.
     */
    public void setSampleSize(final int size) {
        if ((size < 1) || (size > 2)) {
            throw new IllegalArgumentRangeException(1, 2, size);
        }
        sampleSize = size;
    }

    /**
     * Sets the number of samples in the sound data.
     *
     * @param count
     *            the number of samples for the sound.
     */
    public void setSampleCount(final int count) {
        if (count < 1) {
            throw new IllegalArgumentRangeException(1,
                    Integer.MAX_VALUE, count);
        }
        sampleCount = count;
    }

    /**
     * Sets the sound data.
     *
     * @param bytes
     *            the sound data. Must not be null.
     */
    public void setSound(final byte[] bytes) {
        if (bytes == null) {
            throw new IllegalArgumentException();
        }
        sound = Arrays.copyOf(bytes, bytes.length);
    }

    /** {@inheritDoc} */
    public DefineSound copy() {
        return new DefineSound(this);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return String.format(FORMAT, identifier, format, rate, channelCount,
                sampleSize, sampleCount);
    }

    /** {@inheritDoc} */
    public int prepareToEncode(final SWFEncoder coder, final Context context) {
        length = 7;
        length += sound.length;

        return (length > SWFEncoder.STD_LIMIT ? SWFEncoder.EXT_LENGTH
                : SWFEncoder.STD_LENGTH) + length;
    }

    /** {@inheritDoc} */
    public void encode(final SWFEncoder coder, final Context context)
            throws CoderException {

        final int start = coder.getPointer();
        coder.writeHeader(MovieTypes.DEFINE_SOUND, length);
        final int end = coder.getPointer() + (length << Coder.BYTES_TO_BITS);

        coder.writeI16(identifier);
        coder.writeBits(format, 4);

        switch (rate) {
        case SoundRate.KHZ_5K:
            coder.writeBits(0, 2);
            break;
        case SoundRate.KHZ_11K:
            coder.writeBits(1, 2);
            break;
        case SoundRate.KHZ_22K:
            coder.writeBits(2, 2);
            break;
        case SoundRate.KHZ_44K:
            coder.writeBits(3, 2);
            break;
        default:
            break;
        }
        coder.writeBits(sampleSize - 1, 1);
        coder.writeBits(channelCount - 1, 1);
        coder.writeI32(sampleCount);

        coder.writeBytes(sound);

        if (coder.getPointer() != end) {
            throw new CoderException(getClass().getName(),
                    start >> Coder.BITS_TO_BYTES, length,
                    (coder.getPointer() - end) >> Coder.BITS_TO_BYTES);
        }
    }
}
