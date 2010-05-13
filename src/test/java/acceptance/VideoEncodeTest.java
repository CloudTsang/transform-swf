/*
 * VideoEncodeTest.java
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

package acceptance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FilenameFilter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.flagstone.transform.tools.VideoWriter;
import com.flagstone.transform.video.Video;

@RunWith(Parameterized.class)
public final class VideoEncodeTest {

    private static File destDir;

    @Parameters
    public static Collection<Object[]>  files() {

        final File srcDir;

        if (System.getProperty("test.suite") == null) {
            srcDir = new File("test/data/flv/reference");
        } else {
            srcDir = new File(System.getProperty("test.suites"));
        }

        destDir = new File("test/results/acceptance", "VideoEncodeTest");

        if (!destDir.exists() && !destDir.mkdirs()) {
            fail();
        }

        final FilenameFilter filter = new FilenameFilter() {
            public boolean accept(final File directory, final String name) {
                return name.endsWith(".flv");
            }
        };

        String[] files = srcDir.list(filter);
        Object[][] collection = new Object[files.length][2];

        for (int i = 0; i < files.length; i++) {
            collection[i][0] = new File(srcDir, files[i]);
            collection[i][1] = new File(destDir, files[i]);
        }
        return Arrays.asList(collection);
    }

    private final File sourceFile;
    private final File destFile;

    public VideoEncodeTest(final File src, final File dst) {
        sourceFile = src;
        destFile = dst;
    }

    @Test
    public void encode() {

        try {
            final Video source = new Video();
            source.decodeFromFile(sourceFile);
            source.encodeToFile(destFile);

            final Video dest = new Video();
            dest.decodeFromFile(destFile);

            final StringWriter sourceWriter = new StringWriter();

            final VideoWriter writer = new VideoWriter();
            writer.write(source, sourceWriter);

            final StringWriter destWriter = new StringWriter();
            writer.write(dest, destWriter);

            assertEquals(sourceWriter.toString(), destWriter.toString());

        } catch (Exception e) {
            e.printStackTrace();
            fail(sourceFile.getPath());
        }
    }
}
