/*
 * MovieData.java
 * Transform
 * 
 * Copyright (c) 2001-2009 Flagstone Software Ltd. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, 
 * are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright notice, 
 *    this list of conditions and the following disclaimer in the documentation 
 *    and/or other materials provided with the distribution.
 *  * Neither the name of Flagstone Software Ltd. nor the names of its contributors 
 *    may be used to endorse or promote products derived from this software 
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND 
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. 
 * IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, 
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, 
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, 
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF 
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE 
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED 
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.flagstone.transform.movie.shape;

import java.util.Arrays;

import com.flagstone.transform.coder.CoderException;
import com.flagstone.transform.coder.SWFDecoder;
import com.flagstone.transform.coder.SWFEncoder;

public final class ShapeData implements ShapeRecord
{
	private static final String FORMAT = "ShapeData: { data[%d] }";

	private byte[] data;
	
	public ShapeData() {
	}

	public ShapeData(ShapeData object) {
		data = Arrays.copyOf(object.data, object.data.length);
	}

	/**
	 * Returns the encoded data for the action.
	 */
	public byte[] getData()
	{
		return data;
	}

	/**
	 * Sets the encoded data for the action.
	 * 
	 * @param bytes
	 *            the encoded data for the action. May be zero length but not null.
	 */
	public void setData(byte[] bytes)
	{
		data = new byte[bytes.length];
		System.arraycopy(bytes, 0, data, 0, bytes.length);
	}
	
	public void setData(int size) {
	    data = new byte[size];
	}

	public ShapeData copy() 
	{
		return new ShapeData(this);
	}

	@Override
	public String toString()
	{
		return String.format(FORMAT, data.length);
	}

	public int prepareToEncode(final SWFEncoder coder)
	{
		return data.length;
	}

	public void encode(final SWFEncoder coder) throws CoderException
	{
		coder.writeBytes(data);
	}

	@Override
	public void decode(final SWFDecoder coder) throws CoderException
	{
		coder.readBytes(data);
	}
}
