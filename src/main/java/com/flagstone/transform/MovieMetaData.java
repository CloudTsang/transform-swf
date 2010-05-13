/*
 * MetaData.java
 * Transform
 *
 * Copyright (c) 2009-2010 Flagstone Software Ltd. All rights reserved.
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

package com.flagstone.transform;

import com.flagstone.transform.coder.CoderException;
import com.flagstone.transform.coder.Context;
import com.flagstone.transform.coder.MovieTag;
import com.flagstone.transform.coder.MovieTypes;
import com.flagstone.transform.coder.SWFDecoder;
import com.flagstone.transform.coder.SWFEncoder;

/**
 * MetaData is used to add a user-defined information into a Flash file.
 */
// TODO(doc)
//TODO(class)
public final class MovieMetaData implements MovieTag {

    private static final String FORMAT = "MetaData: { %s }";

    private String metaData;

    private transient int length;

    /**
     * Creates and initialises a MoveMetaData object using values encoded
     * in the Flash binary format.
     *
     * @param coder
     *            an SWFDecoder object that contains the encoded Flash data.
     *
     * @throws CoderException
     *             if an error occurs while decoding the data.
     */
    public MovieMetaData(final SWFDecoder coder) throws CoderException {
        length = coder.readHeader();
        metaData = coder.readString(length - 1, coder.getEncoding());
        coder.readByte();
    }

    /**
     * Creates a MoveMetaData object with the specified string containing the
     * meta-data for the movie.
     *
     * @param aString
     *            an arbitrary string containing the meta-data. Must not be
     *            null.
     */
    public MovieMetaData(final String aString) {
        setMetaData(aString);
    }

    /**
     * Creates and initialises a MovieMetaData object using the values copied
     * from another MovieMetaData object.
     *
     * @param object
     *            a MovieMetaData object from which the values will be
     *            copied.
     */
    public MovieMetaData(final MovieMetaData object) {
        metaData = object.metaData;
    }


    public String getMetaData() {
        return metaData;
    }


    public void setMetaData(final String aString) {
        if (aString == null) {
            throw new IllegalArgumentException();
        }
        metaData = aString;
    }

    /** {@inheritDoc} */
    public MovieMetaData copy() {
        return new MovieMetaData(this);
    }

    @Override
    public String toString() {
        return String.format(FORMAT, metaData);
    }

    /** {@inheritDoc} */
    public int prepareToEncode(final SWFEncoder coder, final Context context) {
        length = coder.strlen(metaData);

        return (length > 62 ? 6 : 2) + length;
    }

    /** {@inheritDoc} */
    public void encode(final SWFEncoder coder, final Context context)
            throws CoderException {

        coder.writeHeader(MovieTypes.METADATA, length);
        coder.writeString(metaData);
    }
}
