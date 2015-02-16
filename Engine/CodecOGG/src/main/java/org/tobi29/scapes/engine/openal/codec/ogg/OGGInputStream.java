/*
 * Copyright 2012-2015 Tobi29
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

package org.tobi29.scapes.engine.openal.codec.ogg;

import com.jcraft.jogg.Packet;
import com.jcraft.jogg.Page;
import com.jcraft.jogg.StreamState;
import com.jcraft.jogg.SyncState;
import com.jcraft.jorbis.Block;
import com.jcraft.jorbis.Comment;
import com.jcraft.jorbis.DspState;
import com.jcraft.jorbis.Info;
import org.tobi29.scapes.engine.openal.codec.AudioInputStream;
import org.tobi29.scapes.engine.utils.math.FastMath;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;

public class OGGInputStream extends AudioInputStream {
    private static final boolean BIG_ENDIAN =
            ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN;
    private static final int BUFFER_SIZE = 4096;
    private final InputStream streamIn;
    private final Packet packet = new Packet();
    private final Page page = new Page();
    private final StreamState streamState = new StreamState();
    private final SyncState syncState = new SyncState();
    private final DspState dspState = new DspState();
    private final Block block = new Block(dspState);
    private final Comment comment = new Comment();
    private final Info info = new Info();
    private final int channels, rate;
    private final byte[] buffer;
    private final int[] index;
    private final float[][][] pcm = new float[1][][];
    private boolean eos;
    private int position, limit;

    public OGGInputStream(InputStream streamIn) throws IOException {
        this.streamIn = streamIn;
        syncState.init();
        info.init();
        comment.init();
        readHeader();
        channels = info.channels;
        rate = info.rate;
        buffer = new byte[BUFFER_SIZE * channels << 1];
        index = new int[channels];
    }

    @Override
    public int getChannels() {
        return channels;
    }

    @Override
    public int getRate() {
        return rate;
    }

    @Override
    public int read() throws IOException {
        checkFrame();
        return buffer[position++];
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (eos) {
            return -1;
        }
        int read = 0;
        while (read < len) {
            if (eos) {
                return read;
            }
            checkFrame();
            int size = FastMath.min(limit - position, len - read);
            System.arraycopy(buffer, position, b, read, size);
            position += size;
            read += size;
        }
        return read;
    }

    @Override
    public int available() {
        return eos ? 0 : 1;
    }

    private void readHeader() throws IOException {
        readPage();
        streamState.init(page.serialno());
        if (streamState.pagein(page) == -1) {
            throw new IOException("Error reading first header page");
        }
        if (streamState.packetout(packet) != 1) {
            throw new IOException("Error reading first header packet");
        }
        if (info.synthesis_headerin(comment, packet) < 0) {
            throw new IOException("Error interpreting first header packet");
        }
        for (int i = 0; i < 2; i++) {
            readPacket();
            info.synthesis_headerin(comment, packet);
        }
        dspState.synthesis_init(info);
        block.init(dspState);
    }

    private void checkFrame() throws IOException {
        if (position >= limit) {
            readPacket();
            position = 0;
            limit = decodePacket();
        }
    }

    private void readPacket() throws IOException {
        while (!eos) {
            switch (streamState.packetout(packet)) {
                case -1:
                    throw new IOException("Hole in packet");
                case 1:
                    return;
            }
            readPage();
            streamState.pagein(page);
        }
    }

    private int decodePacket() {
        if (block.synthesis(packet) == 0) {
            dspState.synthesis_blockin(block);
            int offset = 0;
            int samples = dspState.synthesis_pcmout(pcm, index);
            while (samples > 0) {
                float[][] pcmSamples = pcm[0];
                int length = FastMath.min(samples, BUFFER_SIZE);
                for (int i = 0; i < channels; i++) {
                    float[] channel = pcmSamples[i];
                    int position = (i << 1) + offset;
                    int location = index[i];
                    for (int j = 0; j < length; j++) {
                        int sample =
                                (int) (channel[location + j] * Short.MAX_VALUE);
                        short value = (short) FastMath
                                .clamp(sample, Short.MIN_VALUE,
                                        Short.MAX_VALUE);
                        if (BIG_ENDIAN) {
                            buffer[position] = (byte) (value >>> 8 & 0xFF);
                            buffer[position + 1] = (byte) (value & 0xFF);
                        } else {
                            buffer[position] = (byte) (value & 0xFF);
                            buffer[position + 1] = (byte) (value >>> 8 & 0xFF);
                        }
                        position += channels << 1;
                    }
                }
                dspState.synthesis_read(length);
                offset += length * channels << 1;
                samples = dspState.synthesis_pcmout(pcm, index);
            }
            return offset;
        } else {
            eos = true;
        }
        return 0;
    }

    private void readPage() throws IOException {
        while (!eos) {
            switch (syncState.pageout(page)) {
                case -1:
                    throw new IOException("Hole in page");
                case 1:
                    return;
            }
            fillBuffer();
        }
    }

    private void fillBuffer() throws IOException {
        int offset = syncState.buffer(BUFFER_SIZE);
        int read = streamIn.read(syncState.data, offset, BUFFER_SIZE);
        if (read == -1) {
            eos = true;
            return;
        }
        syncState.wrote(read);
    }
}
