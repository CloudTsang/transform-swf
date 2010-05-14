/*
 * PNGDecoder.java
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

package com.flagstone.transform.util.image;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import com.flagstone.transform.coder.FLVDecoder;
import com.flagstone.transform.coder.ImageTag;
import com.flagstone.transform.image.DefineImage;
import com.flagstone.transform.image.DefineImage2;
import com.flagstone.transform.image.ImageFormat;

/**
 * PNGDecoder decodes Portable Network Graphics (PNG) format images so they can
 * be used in a Flash file.
 */
//TODO(class)
@SuppressWarnings("PMD.TooManyMethods")
public final class PNGDecoder implements ImageProvider, ImageDecoder {

    /** Alpha channel value for opaque colors. */
    private static final int OPAQUE = 255;
    /** Mask for reading unsigned 8-bit values. */
    private static final int UNSIGNED_BYTE = 255;

    private static final String BAD_FORMAT = "Unsupported format";

    // Tables mapping grey scale values onto 8-bit colour channels
    private static final int[] MONOCHROME = {0, 255};
    private static final int[] GREYCSALE2 = {0, 85, 170, 255};
    private static final int[] GREYCSALE4 = {0, 17, 34, 51, 68, 85, 102, 119,
            136, 153, 170, 187, 204, 221, 238, 255};

    // Constants used for PNG images

    private static final int[] SIGNATURE = {137, 80, 78, 71, 13, 10, 26, 10};

    // private static final int CRITICAL_CHUNK = 0x20000000;

    private static final int IHDR = 0x49484452;
    private static final int PLTE = 0x504c5445;
    private static final int IDAT = 0x49444154;
    private static final int IEND = 0x49454e44;
    private static final int TRNS = 0x74524e53;
    /*
     * private static final int BKGD = 0x624b4744; private static final int CHRM
     * = 0x6348524d; private static final int FRAC = 0x66524163; private static
     * final int GAMA = 0x67414d41; private static final int GIFG = 0x67494667;
     * private static final int GIFT = 0x67494674; private static final int GIFX
     * = 0x67494678; private static final int HIST = 0x68495354; private static
     * final int ICCP = 0x69434350; private static final int ITXT = 0x69545874;
     * private static final int OFFS = 0x6f464673; private static final int PCAL
     * = 0x7043414c; private static final int PHYS = 0x70485973; private static
     * final int SBIT = 0x73424954; private static final int SCAL = 0x7343414c;
     * private static final int SPLT = 0x73504c54; private static final int SRGB
     * = 0x73524742; private static final int TEXT = 0x74455874; private static
     * final int TIME = 0x74494d45; private static final int ZTXT = 0x7a545874;
     */
    private static final int GREYSCALE = 0;
    private static final int TRUE_COLOUR = 2;
    private static final int INDEXED_COLOUR = 3;
    private static final int ALPHA_GREYSCALE = 4;
    private static final int ALPHA_TRUECOLOUR = 6;

    private static final int NO_FILTER = 0;
    private static final int SUB_FILTER = 1;
    private static final int UP_FILTER = 2;
    private static final int AVG_FILTER = 3;
    private static final int PAETH_FILTER = 4;

    private static final int[] START_ROW = {0, 0, 4, 0, 2, 0, 1};
    private static final int[] START_COLUMN = {0, 4, 0, 2, 0, 1, 0};
    private static final int[] ROW_STEP = {8, 8, 8, 4, 4, 2, 2};
    private static final int[] COLUMN_STEP = {8, 8, 4, 4, 2, 2, 1};

    private static final int BIT_DEPTH = 0;
    private static final int COLOUR_COMPONENTS = 1;
    private static final int COMPRESSION = 2;

    private static final int COLOUR_TYPE = 4;
    private static final int FILTER_METHOD = 5;
    private static final int INTERLACE_METHOD = 6;
    private static final int TRANSPARENT_GREY = 7;
    private static final int TRANSPARENT_RED = 8;
    private static final int TRANSPARENT_GREEN = 9;
    private static final int TRANSPARENT_BLUE = 10;

    private final transient int[] attributes = new int[16];
    private transient byte[] chunkData = new byte[0];

    private transient ImageFormat format;
    private transient int width;
    private transient int height;
    private transient byte[] table;
    private transient byte[] image;


    public ImageDecoder newDecoder() {
        return new PNGDecoder();
    }


    public void read(final File file) throws IOException, DataFormatException {
        final ImageInfo info = new ImageInfo();
        info.setInput(new RandomAccessFile(file, "r"));
        info.setDetermineImageNumber(true);

        if (!info.check()) {
            throw new DataFormatException(BAD_FORMAT);
        }

        read(new FileInputStream(file), (int) file.length());
    }


    public void read(final URL url) throws IOException, DataFormatException {
        final URLConnection connection = url.openConnection();
        final int length = connection.getContentLength();

        if (length < 0) {
            throw new FileNotFoundException(url.getFile());
        }

        read(url.openStream(), length);
    }


    public ImageTag defineImage(final int identifier) {
        ImageTag object = null;

        switch (format) {
        case IDX8:
            object = new DefineImage(identifier, width, height,
                    table.length / 4,
                    zip(merge(adjustScan(width, height, image), table)));
            break;
        case IDXA:
            object = new DefineImage2(identifier, width, height,
                    table.length / 4,
                    zip(mergeAlpha(adjustScan(width, height, image), table)));
            break;
        case RGB5:
            object = new DefineImage(identifier, width, height,
                    zip(packColours(width, height, image)), 16);
            break;
        case RGB8:
            orderAlpha(image);
            object = new DefineImage(identifier, width, height, zip(image), 24);
            break;
        case RGBA:
            applyAlpha(image);
            object = new DefineImage2(identifier, width, height, zip(image));
            break;
        default:
            throw new AssertionError(BAD_FORMAT);
        }
        return object;
    }


    public void read(final InputStream stream, final int size)
            throws DataFormatException, IOException {

        final byte[] bytes = new byte[size];
        final BufferedInputStream buffer = new BufferedInputStream(stream);

        buffer.read(bytes);
        buffer.close();

        final FLVDecoder coder = new FLVDecoder(bytes);

        int length = 0;
        int chunkType = 0;
        boolean moreChunks = true;

        attributes[BIT_DEPTH] = 0;
        attributes[COLOUR_COMPONENTS] = 0;
        attributes[COLOUR_TYPE] = 0;
        attributes[FILTER_METHOD] = 0;
        attributes[INTERLACE_METHOD] = 0;
        chunkData = new byte[0];

        attributes[TRANSPARENT_GREY] = -1;
        attributes[TRANSPARENT_RED] = -1;
        attributes[TRANSPARENT_GREEN] = -1;
        attributes[TRANSPARENT_BLUE] = -1;

        for (int i = 0; i < 8; i++) {
            if (coder.readByte() != SIGNATURE[i]) {
                throw new DataFormatException(BAD_FORMAT);
            }
        }

        while (moreChunks) {
            length = coder.readUI32();
            chunkType = coder.readUI32();

            final int current = coder.getPointer();
            final int next = current + ((length + 4) << 3);

            switch (chunkType) {
            case IHDR:
                decodeIHDR(coder);
                break;
            case PLTE:
                decodePLTE(coder, length);
                break;
            case TRNS:
                decodeTRNS(coder, length);
                break;
            case IDAT:
                decodeIDAT(coder, length);
                break;
            case IEND:
                moreChunks = false;
                coder.adjustPointer(32);
                break;
            default:
                coder.adjustPointer((length + 4) << 3);
                break;
            }
            length += 4; // include CRC at end of chunk
            coder.setPointer(next);

            if (coder.eof()) {
                moreChunks = false;
            }
        }
        decodeImage();
    }

    private void decodeIHDR(final FLVDecoder coder) throws DataFormatException {
        width = coder.readUI32();
        height = coder.readUI32();
        attributes[BIT_DEPTH] = coder.readByte();
        attributes[COLOUR_TYPE] = coder.readByte();
        attributes[COMPRESSION] = coder.readByte();
        attributes[FILTER_METHOD] = coder.readByte();
        attributes[INTERLACE_METHOD] = coder.readByte();

        coder.readUI32(); // crc

        switch (attributes[COLOUR_TYPE]) {
        case GREYSCALE:
            format = (attributes[TRANSPARENT_GREY] == -1) ? ImageFormat.RGB8
                    : ImageFormat.RGBA;
            attributes[COLOUR_COMPONENTS] = 1;
            break;
        case TRUE_COLOUR:
            format = (attributes[TRANSPARENT_RED] == -1) ? ImageFormat.RGB8
                    : ImageFormat.RGBA;
            attributes[COLOUR_COMPONENTS] = 3;
            break;
        case INDEXED_COLOUR:
            format = ImageFormat.IDX8;
            attributes[COLOUR_COMPONENTS] = 1;
            break;
        case ALPHA_GREYSCALE:
            format = ImageFormat.RGBA;
            attributes[COLOUR_COMPONENTS] = 2;
            break;
        case ALPHA_TRUECOLOUR:
            format = ImageFormat.RGBA;
            attributes[COLOUR_COMPONENTS] = 4;
            break;
        default:
            throw new DataFormatException(BAD_FORMAT);
        }
    }

    private void decodePLTE(final FLVDecoder coder, final int length) {
        if (attributes[COLOUR_TYPE] == 3) {
            final int paletteSize = length / 3;
            int index = 0;

            table = new byte[paletteSize * 4];

            for (int i = 0; i < paletteSize; i++, index += 4) {
                table[index + 3] = (byte) OPAQUE;
                table[index + 2] = (byte) coder.readByte();
                table[index + 1] = (byte) coder.readByte();
                table[index] = (byte) coder.readByte();
            }
        } else {
            coder.adjustPointer(length << 3);
        }
        coder.readUI32(); // crc
    }

    private void decodeTRNS(final FLVDecoder coder, final int length) {
        int index = 0;

        switch (attributes[COLOUR_TYPE]) {
        case GREYSCALE:
            attributes[TRANSPARENT_GREY] = coder.readUI16();
            break;
        case TRUE_COLOUR:
            attributes[TRANSPARENT_RED] = coder.readUI16();
            attributes[TRANSPARENT_GREEN] = coder.readUI16();
            attributes[TRANSPARENT_BLUE] = coder.readUI16();
            break;
        case INDEXED_COLOUR:
            format = ImageFormat.IDXA;
            for (int i = 0; i < length; i++, index += 4) {
                table[index + 3] = (byte) coder.readByte();

                if (table[index + 3] == 0) {
                    table[index] = 0;
                    table[index + 1] = 0;
                    table[index + 2] = 0;
                }
            }
            break;
        default:
            break;
        }
        coder.readUI32(); // crc
    }

    private void decodeIDAT(final FLVDecoder coder, final int length) {
        final int currentLength = chunkData.length;
        final int newLength = currentLength + length;

        final byte[] data = new byte[newLength];

        System.arraycopy(chunkData, 0, data, 0, currentLength);

        for (int i = currentLength; i < newLength; i++) {
            data[i] = (byte) coder.readByte();
        }

        chunkData = data;

        coder.readUI32(); // crc
    }

    private void decodeImage() throws DataFormatException {
        if ((format == ImageFormat.RGB8) && (attributes[BIT_DEPTH] <= 5)) {
            format = ImageFormat.RGB5;
        }

        if ((format == ImageFormat.RGB5) || (format == ImageFormat.RGB8)
                || (format == ImageFormat.RGBA)) {
            image = new byte[height * width * 4];
        }

        if ((format == ImageFormat.IDX8) || (format == ImageFormat.IDXA)) {
            image = new byte[height * width];
        }

        final byte[] encodedImage = unzip(chunkData);

        final int bitsPerPixel = attributes[BIT_DEPTH]
                * attributes[COLOUR_COMPONENTS];
        final int bitsPerRow = width * bitsPerPixel;
        final int rowWidth = (bitsPerRow % 8 > 0) ? (bitsPerRow / 8) + 1
                : (bitsPerRow / 8);
        final int bytesPerPixel = (bitsPerPixel < 8) ? 1 : bitsPerPixel / 8;

        final byte[] current = new byte[rowWidth];
        final byte[] previous = new byte[rowWidth];

        for (int i = 0; i < rowWidth; i++) {
            previous[i] = (byte) 0;
        }

        int rowStart = 0;
        int rowInc = 0;
        int colStart = 0;
        int colInc = 0;

        int imageIndex = 0;
//        int pixelCount = 0;

        int row = 0;
        int col = 0;
        int filter = 0;

        int scanBits = 0;
        int scanLength = 0;

        final int numberOfPasses = (attributes[INTERLACE_METHOD] == 1) ? 7 : 1;

        int cindex = 0;
        int pindex = 0;

        for (int pass = 0; pass < numberOfPasses; pass++) {
            rowStart = (attributes[INTERLACE_METHOD] == 1)
            ? START_ROW[pass] : 0;
            rowInc = (attributes[INTERLACE_METHOD] == 1) ? ROW_STEP[pass]
                    : 1;

            colStart = (attributes[INTERLACE_METHOD] == 1) ? START_COLUMN[pass]
                    : 0;
            colInc = (attributes[INTERLACE_METHOD] == 1) ? COLUMN_STEP[pass]
                    : 1;

            for (row = rowStart; (row < height)
                    && (imageIndex < encodedImage.length); row += rowInc) {
                for (col = colStart, /* pixelCount = 0, */ scanBits = 0; col
                        < width; col += colInc) {
                    /* pixelCount++; */
                    scanBits += bitsPerPixel;
                }

                scanLength = (scanBits % 8 > 0) ? (scanBits / 8) + 1
                        : (scanBits / 8);

                filter = encodedImage[imageIndex++];

                for (int i = 0; i < scanLength; i++, imageIndex++) {
                    current[i] = (imageIndex < encodedImage.length)
                            ? encodedImage[imageIndex]
                            : previous[i];
                }

                switch (filter) {
                case NO_FILTER:
                    break;
                case SUB_FILTER:
                    for (cindex = bytesPerPixel, pindex = 0;
                            cindex < scanLength; cindex++, pindex++) {
                        current[cindex] =
                            (byte) (current[cindex] + current[pindex]);
                    }
                    break;
                case UP_FILTER:
                    for (cindex = 0; cindex < scanLength; cindex++) {
                        current[cindex] =
                                (byte) (current[cindex] + previous[cindex]);
                    }
                    break;
                case AVG_FILTER:
                    for (cindex = 0; cindex < bytesPerPixel; cindex++) {
                        current[cindex] = (byte) (current[cindex]
                                        + (0 + (UNSIGNED_BYTE
                                                & previous[cindex])) / 2);
                    }

                    for (cindex = bytesPerPixel, pindex = 0;
                    cindex < scanLength; cindex++, pindex++) {
                        current[cindex] = (byte) (current[cindex]
                            + ((UNSIGNED_BYTE & current[pindex])
                                    + (UNSIGNED_BYTE & previous[cindex])) / 2);
                    }
                    break;
                case PAETH_FILTER:
                    for (cindex = 0; cindex < bytesPerPixel; cindex++) {
                        current[cindex] = (byte) (current[cindex]
                                     + paeth((byte) 0,
                                previous[cindex], (byte) 0));
                    }

                    for (cindex = bytesPerPixel, pindex = 0;
                    cindex < scanLength; cindex++, pindex++) {
                        current[cindex] = (byte) (current[cindex]
                                                      + paeth(current[pindex],
                                previous[cindex], previous[pindex]));
                    }
                    break;
                default:
                    throw new DataFormatException(BAD_FORMAT);
                }

                System.arraycopy(current, 0, previous, 0, scanLength);

                final FLVDecoder coder = new FLVDecoder(current);

                for (col = colStart; col < width; col += colInc) {
                    switch (attributes[COLOUR_TYPE]) {
                    case GREYSCALE:
                        decodeGreyscale(coder, row, col);
                        break;
                    case TRUE_COLOUR:
                        decodeTrueColour(coder, row, col);
                        break;
                    case INDEXED_COLOUR:
                        decodeIndexedColour(coder, row, col);
                        break;
                    case ALPHA_GREYSCALE:
                        decodeAlphaGreyscale(coder, row, col);
                        break;
                    case ALPHA_TRUECOLOUR:
                        decodeAlphaTrueColour(coder, row, col);
                        break;
                    default:
                        throw new DataFormatException(BAD_FORMAT);
                    }
                }
            }
        }
    }

    private int paeth(final byte lower, final byte upper, final byte next) {
        final int left = UNSIGNED_BYTE & lower;
        final int above = UNSIGNED_BYTE & upper;
        final int upperLeft = UNSIGNED_BYTE & next;
        final int estimate = left + above - upperLeft;
        int distLeft = estimate - left;

        if (distLeft < 0) {
            distLeft = -distLeft;
        }

        int distAbove = estimate - above;

        if (distAbove < 0) {
            distAbove = -distAbove;
        }

        int distUpperLeft = estimate - upperLeft;

        if (distUpperLeft < 0) {
            distUpperLeft = -distUpperLeft;
        }

        final int value;

        if ((distLeft <= distAbove) && (distLeft <= distUpperLeft)) {
            value = left;
        } else if (distAbove <= distUpperLeft) {
            value = above;
        } else {
            value = upperLeft;
        }

        return value;
    }

    private void decodeGreyscale(final FLVDecoder coder, final int row,
            final int col) throws DataFormatException {
        int pixel = 0;
        byte colour = 0;

        switch (attributes[BIT_DEPTH]) {
        case 1:
            pixel = coder.readBits(1, false);
            colour = (byte) MONOCHROME[pixel];
            break;
        case 2:
            pixel = coder.readBits(2, false);
            colour = (byte) GREYCSALE2[pixel];
            break;
        case 4:
            pixel = coder.readBits(4, false);
            colour = (byte) GREYCSALE4[pixel];
            break;
        case 8:
            pixel = coder.readByte();
            colour = (byte) pixel;
            break;
        case 16:
            pixel = coder.readUI16();
            colour = (byte) (pixel >> 8);
            break;
        default:
            throw new DataFormatException(BAD_FORMAT);
        }

        int index = row * (width << 2) + (col << 2);

        image[index++] = colour;
        image[index++] = colour;
        image[index++] = colour;
        image[index++] = (byte) attributes[TRANSPARENT_GREY];
    }

    private void decodeTrueColour(final FLVDecoder coder, final int row,
            final int col) throws DataFormatException {
        int pixel = 0;
        byte colour = 0;

        int index = row * (width << 2) + (col << 2);

        for (int i = 0; i < attributes[COLOUR_COMPONENTS]; i++) {
            if (attributes[BIT_DEPTH] == 8) {
                pixel = coder.readByte();
                colour = (byte) pixel;
            } else if (attributes[BIT_DEPTH] == 16) {
                pixel = coder.readUI16();
                colour = (byte) (pixel >> 8);
            } else {
                throw new DataFormatException(BAD_FORMAT);
            }

            image[index + i] = colour;
        }
        image[index + 3] = (byte) attributes[TRANSPARENT_RED];
    }

    private void decodeIndexedColour(final FLVDecoder coder, final int row,
            final int col) throws DataFormatException {
        int index = 0;

        switch (attributes[BIT_DEPTH]) {
        case 1:
            index = coder.readBits(1, false);
            break;
        case 2:
            index = coder.readBits(2, false);
            break;
        case 4:
            index = coder.readBits(4, false);
            break;
        case 8:
            index = coder.readByte();
            break;
        case 16:
            index = coder.readUI16();
            break;
        default:
            throw new DataFormatException(BAD_FORMAT);
        }
        image[row * width + col] = (byte) index;
    }

    private void decodeAlphaGreyscale(final FLVDecoder coder, final int row,
            final int col) throws DataFormatException {
        int pixel = 0;
        byte colour = 0;
        int alpha = 0;

        switch (attributes[BIT_DEPTH]) {
        case 1:
            pixel = coder.readBits(1, false);
            colour = (byte) MONOCHROME[pixel];
            alpha = coder.readBits(1, false);
            break;
        case 2:
            pixel = coder.readBits(2, false);
            colour = (byte) GREYCSALE2[pixel];
            alpha = coder.readBits(2, false);
            break;
        case 4:
            pixel = coder.readBits(4, false);
            colour = (byte) GREYCSALE4[pixel];
            alpha = coder.readBits(4, false);
            break;
        case 8:
            pixel = coder.readByte();
            colour = (byte) pixel;
            alpha = coder.readByte();
            break;
        case 16:
            pixel = coder.readUI16();
            colour = (byte) (pixel >> 8);
            alpha = coder.readUI16() >> 8;
            break;
        default:
            throw new DataFormatException(BAD_FORMAT);
        }

        int index = row * (width << 2) + (col << 2);

        image[index++] = colour;
        image[index++] = colour;
        image[index++] = colour;
        image[index] = (byte) alpha;
    }

    private void decodeAlphaTrueColour(final FLVDecoder coder, final int row,
            final int col) throws DataFormatException {
        int pixel = 0;
        byte colour = 0;

        int index = row * (width << 2) + (col << 2);

        for (int i = 0; i < attributes[COLOUR_COMPONENTS]; i++) {
            if (attributes[BIT_DEPTH] == 8) {
                pixel = coder.readByte();
                colour = (byte) pixel;
            } else if (attributes[BIT_DEPTH] == 16) {
                pixel = coder.readUI16();
                colour = (byte) (pixel >> 8);
            } else {
                throw new DataFormatException(BAD_FORMAT);
            }

            image[index + i] = colour;
        }
    }

    private byte[] unzip(final byte[] bytes) throws DataFormatException {
        final byte[] data = new byte[width * height * 8];
        int count = 0;

        final Inflater inflater = new Inflater();
        inflater.setInput(bytes);
        count = inflater.inflate(data);

        final byte[] uncompressedData = new byte[count];

        System.arraycopy(data, 0, uncompressedData, 0, count);

        return uncompressedData;
    }

    private void orderAlpha(final byte[] img) {
        byte alpha;

        for (int i = 0; i < img.length; i += 4) {
            alpha = img[i + 3];

            img[i + 3] = img[i + 2];
            img[i + 2] = img[i + 1];
            img[i + 1] = img[i];
            img[i] = alpha;
        }
    }

    private void applyAlpha(final byte[] img) {
        int alpha;

        for (int i = 0; i < img.length; i += 4) {
            alpha = img[i + 3] & UNSIGNED_BYTE;

            img[i + 3] = (byte) (((img[i + 2] & UNSIGNED_BYTE) * alpha)
                    / OPAQUE);
            img[i + 2] = (byte) (((img[i + 1] & UNSIGNED_BYTE) * alpha)
                    / OPAQUE);
            img[i + 1] = (byte) (((img[i] & UNSIGNED_BYTE) * alpha) / OPAQUE);
            img[i] = (byte) alpha;
       }
    }

    private byte[] merge(final byte[] img, final byte[] colors) {
        final byte[] merged = new byte[(colors.length / 4) * 3 + img.length];
        int dst = 0;

        for (int i = 0; i < colors.length; i += 4) {
            merged[dst++] = colors[i + 1]; // G
            merged[dst++] = colors[i]; // R
        }

        for (final byte element : img) {
            merged[dst++] = element;
        }

        return merged;
    }

    private byte[] mergeAlpha(final byte[] img, final byte[] colors) {
        final byte[] merged = new byte[colors.length + img.length];
        int dst = 0;

        for (final byte element : colors) {
            merged[dst++] = element;
        }

        for (final byte element : img) {
            merged[dst++] = element;
        }
        return merged;
    }

    private byte[] zip(final byte[] img) {
        final Deflater deflater = new Deflater();
        deflater.setInput(img);
        deflater.finish();

        final byte[] compressedData = new byte[img.length * 2];
        final int bytesCompressed = deflater.deflate(compressedData);
        final byte[] newData = Arrays.copyOf(compressedData, bytesCompressed);

        return newData;
    }

    private byte[] adjustScan(final int imgWidth, final int imgHeight,
            final byte[] img) {
        int src = 0;
        int dst = 0;
        int row;
        int col;

        int scan = 0;
        byte[] formattedImage = null;

        scan = (imgWidth + 3) & ~3;
        formattedImage = new byte[scan * imgHeight];

        for (row = 0; row < imgHeight; row++) {
            for (col = 0; col < imgWidth; col++) {
                formattedImage[dst++] = img[src++];
            }

            while (col++ < scan) {
                formattedImage[dst++] = 0;
            }
        }

        return formattedImage;
    }

    private byte[] packColours(final int imgWidth, final int imgHeight,
            final byte[] img) {
        int src = 0;
        int dst = 0;
        int row;
        int col;

        final int scan = imgWidth + (imgWidth & 1);
        final byte[] formattedImage = new byte[scan * imgHeight * 2];

        for (row = 0; row < imgHeight; row++) {
            for (col = 0; col < imgWidth; col++, src++) {
                final int red = (img[src++] & 0xF8) << 7;
                final int green = (img[src++] & 0xF8) << 2;
                final int blue = (img[src++] & 0xF8) >> 3;
                final int colour = (red | green | blue) & 0x7FFF;

                formattedImage[dst++] = (byte) (colour >> 8);
                formattedImage[dst++] = (byte) colour;
            }

            while (col < scan) {
                formattedImage[dst++] = 0;
                formattedImage[dst++] = 0;
                col++;
            }
        }
        return formattedImage;
    }


    public int getWidth() {
        return width;
    }


    public int getHeight() {
        return height;
    }


    public byte[] getImage() {
        return Arrays.copyOf(image, image.length);
    }
}
