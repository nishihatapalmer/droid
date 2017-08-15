/**
 * Copyright (c) 2016, The National Archives <pronom@nationalarchives.gsi.gov.uk>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following
 * conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 *  * Neither the name of the The National Archives nor the
 *    names of its contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package uk.gov.nationalarchives.droid.core.interfaces.resource;

import net.byteseek.io.reader.InputStreamReader;
import net.byteseek.io.reader.cache.AllWindowsCache;
import net.byteseek.io.reader.cache.TempFileCache;
import net.byteseek.io.reader.windows.Window;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Random;

import static org.junit.Assert.assertEquals;


/**
 * @author rflitcroft, boreilly
 * We can delete the entire class once the Byteseek2 test suite has been fully updated.
 * MattP: We can delete this class if you like.  It tests the behaviour of the byteseek InputStreamReader,
 *        which is now fairly well tested - both in the wild and through more unit testing.
 *
 */
public class CachedBinaryTest {

    @Before
    public void setup() {
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testGetInputStreamWithNoBackingFileCache1() throws Exception {

        byte[] rawBytes = new byte[800];
        new Random().nextBytes(rawBytes);

        ByteArrayInputStream in = new ByteArrayInputStream(rawBytes);

        InputStreamReader reader = new InputStreamReader(in, rawBytes.length, new AllWindowsCache());

        // We need to do this first otherwise the next statement always returns -1 due to the reader,
        // retrieving a null window, not clear why...
        reader.readByte(4096);
        //The cast is required to allow for negative numbers (since readByte returns an int)
        int someByte = (byte)reader.readByte(799);

        assertEquals(rawBytes[799], someByte);

        Window window = reader.getWindow(0);

        int byteIn;
        //int count = 0;

        for(int count =0;count<rawBytes.length; count++) {
            byteIn = window.getByte(count);
            assertEquals("Incorrect byte: " + count, rawBytes[count], (byte) byteIn);
        }

        //This should throw the IndexOutOfBoundsException
        byteIn = window.getByte(rawBytes.length);
    }

    @Ignore
    @Test(expected = IndexOutOfBoundsException.class)
    public void testGetInputStreamWithBackingFileCache1() throws Exception {

        byte[] rawBytes = new byte[8500];
        new Random().nextBytes(rawBytes);

        ByteArrayInputStream in = new ByteArrayInputStream(rawBytes);

        final File tempDir = new File("tmp");
        tempDir.mkdir();

        InputStreamReader reader = new InputStreamReader(in, rawBytes.length, new TempFileCache(tempDir));

        // We need to do this first otherwise the next statement always returns -1 due to the reader,
        // retrieving a null window, not clear why...
        reader.readByte(12228);
        //The cast is required to allow for negative numbers (since readByte returns an int)
        int someByte = (byte)reader.readByte(8499);

        assertEquals(rawBytes[8499], someByte);

        Window window = reader.getWindow(0);

        int byteIn;
        //int count = 0;

        for(int count =0;count<rawBytes.length; count++) {
            byteIn = window.getByte(count);
            assertEquals("Incorrect byte: " + count, rawBytes[count], (byte) byteIn);
        }

        //This should throw the IndexOutOfBoundsException
        byteIn = window.getByte(rawBytes.length);
    }
}
