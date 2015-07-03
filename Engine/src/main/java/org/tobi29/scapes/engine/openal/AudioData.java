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

package org.tobi29.scapes.engine.openal;

import org.tobi29.scapes.engine.openal.codec.AudioInputStream;
import org.tobi29.scapes.engine.utils.BufferCreatorDirect;
import org.tobi29.scapes.engine.utils.io.ByteBufferStream;
import org.tobi29.scapes.engine.utils.io.ProcessStream;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class AudioData {
    private final int buffer;

    public AudioData(AudioInputStream streamIn, OpenAL openAL)
            throws IOException {
        this(read(streamIn), streamIn.getRate(), streamIn.getChannels(),
                openAL);
    }

    public AudioData(ByteBuffer data, int rate, int channels, OpenAL openAL) {
        buffer = openAL.createBuffer();
        openAL.storeBuffer(buffer,
                channels > 1 ? AudioFormat.STEREO : AudioFormat.MONO, data,
                rate);
    }

    public void dispose(SoundSystem soundSystem, OpenAL openAL) {
        soundSystem.removeBufferFromSources(buffer);
        openAL.deleteBuffer(buffer);
    }

    public int getBuffer() {
        return buffer;
    }

    private static ByteBuffer read(InputStream input) throws IOException {
        ByteBufferStream output = new ByteBufferStream(
                capacity -> BufferCreatorDirect.byteBuffer(capacity + 40960));
        ProcessStream.process(input, output::put);
        output.buffer().flip();
        return output.buffer();
    }
}
