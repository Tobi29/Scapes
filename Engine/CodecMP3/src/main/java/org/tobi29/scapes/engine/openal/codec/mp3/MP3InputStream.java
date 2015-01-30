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

package org.tobi29.scapes.engine.openal.codec.mp3;

import javazoom.jl.decoder.*;
import org.tobi29.scapes.engine.openal.codec.AudioInputStream;
import org.tobi29.scapes.engine.utils.math.FastMath;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;

public class MP3InputStream extends AudioInputStream {
    private static final boolean BIG_ENDIAN =
            ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN;
    private final Decoder decoder;
    private final Bitstream bitstream;
    private final OutputBuffer outputBuffer;
    private final int channels, rate;
    private boolean eos;
    private int position;

    public MP3InputStream(InputStream streamIn) throws IOException {
        bitstream = new Bitstream(streamIn);
        decoder = new Decoder();
        Header header = readFrame();
        if (eos) {
            throw new IOException("Unable to read first frame");
        } else {
            channels = header.mode() == Header.SINGLE_CHANNEL ? 1 : 2;
            rate = getSampleRate(header.sample_frequency(), header.version());
            outputBuffer = new OutputBuffer(channels);
            decoder.setOutputBuffer(outputBuffer);
            decodeFrame(header);
        }
    }

    private static int getSampleRate(int sampleFrequency, int version) {
        switch (sampleFrequency) {
            case 0:
                if (version == 1) {
                    return 44100;
                } else if (version == 0) {
                    return 22050;
                }
                return 11025;
            case 1:
                if (version == 1) {
                    return 48000;
                } else if (version == 0) {
                    return 24000;
                }
                return 12000;
            case 2:
                if (version == 1) {
                    return 32000;
                } else if (version == 0) {
                    return 16000;
                }
                return 8000;
        }
        return 0;
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
        return outputBuffer.buffer[position++];
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
            byte[] buffer = outputBuffer.buffer;
            int size = FastMath.min(buffer.length - position, len - read);
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
        if (position >= outputBuffer.buffer.length) {
            position = 0;
            Header header = readFrame();
            if (!eos) {
                decodeFrame(header);
            }
        }
    }

    private Header readFrame() throws IOException {
        try {
            Header header = bitstream.readFrame();
            if (header == null) {
                eos = true;
            }
            return header;
        } catch (BitstreamException e) {
            throw new IOException(e);
        }
    }

    private void decodeFrame(Header header) throws IOException {
        try {
            decoder.decodeFrame(header, bitstream);
            bitstream.closeFrame();
        } catch (DecoderException e) {
            throw new IOException(e);
        }
    }

    private static class OutputBuffer extends Obuffer {
        private final byte[] buffer;
        private final int[] index;
        private final int channels;

        public OutputBuffer(int channels) {
            buffer = new byte[OBUFFERSIZE * channels];
            index = new int[MAXCHANNELS];
            this.channels = channels;
            for (int i = 0; i < channels; i++) {
                index[i] = i;
            }
        }

        @Override
        public void append(int channel, short value) {
            if (BIG_ENDIAN) {
                buffer[index[channel] << 1] = (byte) (value >>> 8 & 0xFF);
                buffer[(index[channel] << 1) + 1] = (byte) (value & 0xFF);
            } else {
                buffer[index[channel] << 1] = (byte) (value & 0xFF);
                buffer[(index[channel] << 1) + 1] = (byte) (value >>> 8 & 0xFF);
            }
            index[channel] += channels;
        }

        @Override
        public void appendSamples(int channel, float[] f) {
            for (int j = 0; j < 32; j++) {
                append(channel, (short) (f[j] > 32767.0f ? 32767.0f :
                        f[j] < -32767.0f ? -32767.0f : f[j]));
            }
        }

        @Override
        public void write_buffer(int val) {
        }

        @Override
        public void close() {
        }

        @Override
        public void clear_buffer() {
            for (int i = 0; i < channels; i++) {
                index[i] = i;
            }
        }

        @Override
        public void set_stop_flag() {
        }
    }
}
