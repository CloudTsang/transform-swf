/*
 * BitmapFill.java
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

package com.flagstone.transform.fillstyle;

import com.flagstone.transform.CoordTransform;
import com.flagstone.transform.Strings;
import com.flagstone.transform.coder.CoderException;
import com.flagstone.transform.coder.Context;
import com.flagstone.transform.coder.FillStyle;
import com.flagstone.transform.coder.SWFDecoder;
import com.flagstone.transform.coder.SWFEncoder;
import com.flagstone.transform.shape.DefineShape;
import com.flagstone.transform.shape.DefineShape2;
import com.flagstone.transform.shape.DefineShape3;

//TODO(doc) Review
/**
 * <p>
 * BitmapFill is used to display an image inside a shape. An image cannot be
 * displayed directly, it must be displayed inside of a shape using this style.
 * The style controls how the image is displayed inside the shape. Images may be
 * clipped if they are too large to fit or tiled across the available area if
 * they are too small. A coordinate transform can also be used to change its
 * size, location relative to the origin of the shape, orientation, etc., when
 * it is displayed. Four types of bitmap fill are supported:
 * </p>
 * 
 * <ol>
 * <li>Clipped - If the image is larger than the shape then it will be clipped.
 * Conversely if the area to be filled is larger than the image the colour at
 * the edge of the image is used to fill the remainder of the shape.</li>
 * 
 * <li>Tiled - if the area to be filled is larger than the image then the image
 * is tiled to fill the area, otherwise as with the Clipped style the colour at
 * the edge of the image will be use to fill the space available.</li>
 * 
 * <li>Unsmoothed Clipped - Same as Clipped but if the image is smaller than the
 * shape the colour used to fill the space available is not smoothed. This style
 * was added to increase performance with few visible artifacts.</li>
 * 
 * <li>Unsmoothed Tiled - Same as Tiled but no smoothing is applied if the
 * colour at the edge of the image is used to fill the space available. Again
 * this was introduced to increase performance.</li>
 * </ol>
 * 
 * <p>
 * The most common use of the coordinate transform is to scale an image so it
 * displayed at the correct resolution. When an image is loaded its width and
 * height default to twips rather than pixels. An image 300 x 200 pixels will be
 * displayed as 300 x 200 twips (15 x 10 pixels). Scaling the image by 20 (20
 * twips = 1 pixel) using the CoordTransform object will restore it to its
 * original size.
 * </p>
 * 
 * <p>
 * The coordinate transform is also used to control the image registration. An
 * image is drawn with the top left corner placed at the origin (0, 0) of the
 * shape being filled. The transform can be used to apply different translations
 * to the image so its position can be adjusted relative to the origin of the
 * enclosing shape.
 * </p>
 * 
 * @see DefineShape
 * @see DefineShape2
 * @see DefineShape3
 */
public final class BitmapFill implements FillStyle {

	private static final String FORMAT = "BitmapFill: { identifier=%d; transform=%s }";

	private int type; 
	private int identifier;
	private CoordTransform transform;

	//TODO(doc)
	public BitmapFill(final SWFDecoder coder, final Context context) throws CoderException {
		type = coder.readByte();
		identifier = coder.readWord(2, false);
		transform = new CoordTransform(coder);
	}

	/**
	 * Creates a BitmapFill object, setting the fill type, the unique identifier
	 * for the image and the coordinate transform used to set the scale and
	 * registration of the image.
	 * 
	 * @param tiled
	 *            whether the image will be repeated if it smaller than the area
	 *            to be filled.
	 * @param smoothed
	 *            whether the image will be smoothed to improve display quality.
	 * @param uid
	 *            the unique identifier of the object containing the image to be
	 *            displayed. Must be in the range 1..65535.
	 * @param transform
	 *            a CoordTransform object that typically changes the size and
	 *            location and position of the image inside the parent shape.
	 */
	public BitmapFill(final boolean tiled, final boolean smoothed, final int uid, final CoordTransform transform) {
		type = 0x40;
		setTiled(tiled);
		setSmoothed(smoothed);
		setIdentifier(uid);
		setTransform(transform);
	}
	
	//TODO(doc)
	public BitmapFill(BitmapFill object) {
		type = object.type;
		identifier = object.identifier;
		transform = object.transform;
	}
	
	public boolean isTiled() {
		return (type & 0x01) != 0;
	}
	
	public void setTiled(boolean tiled) {
		if (tiled) {
			type &= 0x00FE;
		} else {
			type |= 0x0001;
		}
	}

	public boolean isSmoothed() {
		return (type & 0x02) != 0;
	}
	
	public void setSmoothed(boolean smoothed) {
		if (smoothed) {
			type &= 0x00FD;
		} else {
			type |= 0x0002;
		}
	}

	/**
	 * Returns the unique identifier of the object containing the image to be
	 * displayed.
	 */
	public int getIdentifier() {
		return identifier;
	}

	/**
	 * Sets the unique identifier of the object containing the image to be
	 * displayed.
	 * 
	 * @param uid
	 *            the unique identifier of the object containing the image to be
	 *            displayed which must be in the range 1..65535.
	 */
	public void setIdentifier(final int uid) {
		if ((uid < 1) || (uid > 65535)) {
			throw new IllegalArgumentException(Strings.IDENTIFIER_OUT_OF_RANGE);
		}
		identifier = uid;
	}

	/**
	 * Returns the coordinate transform that will be applied to the image when
	 * it is displayed.
	 */
	public CoordTransform getTransform() {
		return transform;
	}

	/**
	 * Sets the coordinate transform applied to the image to display it inside
	 * the shape. Typically the transform will scale the image by a factor of 20
	 * so that the image is displayed at the correct screen resolution.
	 * 
	 * @param aTransform
	 *            a CoordTransform object that changes the appearance and
	 *            location of the image inside the shape. Must not be null.
	 */
	public void setTransform(final CoordTransform aTransform) {
		if (aTransform == null) {
			throw new IllegalArgumentException(Strings.OBJECT_CANNOT_BE_NULL);
		}
		transform = aTransform;
	}

	public BitmapFill copy() {
		return new BitmapFill(this);
	}

	@Override
	public String toString() {
		return String.format(FORMAT, identifier, transform);
	}

	public int prepareToEncode(final SWFEncoder coder, final Context context) {
		return 3 + transform.prepareToEncode(coder, context);
	}

	public void encode(final SWFEncoder coder, final Context context) throws CoderException {
		coder.writeByte(type);
		coder.writeWord(identifier, 2);
		transform.encode(coder, context);
	}
}
