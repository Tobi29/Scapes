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

package org.tobi29.scapes.engine.openal.codec.wav;

import org.tobi29.scapes.engine.openal.codec.AudioInputStream;
import org.tobi29.scapes.engine.utils.math.FastMath;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;

public class WAVInputStream extends AudioInputStream {
    private static final boolean BIG_ENDIAN =
            ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN;
    private static final int BUFFER_SIZE = 4096;
    private final javax.sound.sampled.AudioInputStream streamIn;
    private final int channels, rate, readSize;
    private final byte[] buffer;
    private final boolean is16Bit;
    private boolean eos;
    private int position, limit;

    public WAVInputStream(InputStream streamIn) throws IOException {
        try {
            this.streamIn = AudioSystem
                    .getAudioInputStream(new BufferedInputStream(streamIn));
        } catch (UnsupportedAudioFileException e) {
            throw new IOException(e);
        }
        AudioFormat audioFormat = this.streamIn.getFormat();
        channels = audioFormat.getChannels();
        rate = (int) audioFormat.getSampleRate();
        is16Bit = audioFormat.getSampleSizeInBits() == 16;
        buffer = new byte[BUFFER_SIZE * channels << 1];
        readSize = is16Bit ? buffer.length : buffer.length >> 1;
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

    private void checkFrame() throws IOException {
        if (position >= limit) {
            position = 0;
            if (is16Bit) {
                int read = streamIn.read(buffer, 0, readSize);
                if (read < 0) {
                    eos = true;
                    return;
                }
                limit = read;
            } else {
                int read = streamIn.read(buffer, 0, readSize);
                if (read < 0) {
                    eos = true;
                    return;
                }
                for (int i = read - 1; i >= 0; i--) {
                    int sample = buffer[i];
                    if (sample < 0) {
                        sample += 256;
                    }
                    sample -= 128;
                    int value = sample << 8;
                    int position = i << 1;
                    if (BIG_ENDIAN) {
                        buffer[position] = (byte) (value >>> 8 & 0xFF);
                        buffer[position + 1] = (byte) (value & 0xFF);
                    } else {
                        buffer[position] = (byte) (value & 0xFF);
                        buffer[position + 1] = (byte) (value >>> 8 & 0xFF);
                    }
                }
                limit = read << 1;
            }
        }
    }
}
