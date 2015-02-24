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

package org.tobi29.scapes.engine.openal.codec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.engine.openal.codec.spi.AudioInputStreamProvider;
import org.tobi29.scapes.engine.utils.BufferCreatorDirect;
import org.tobi29.scapes.engine.utils.io.ProcessStream;
import org.tobi29.scapes.engine.utils.io.filesystem.Resource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AudioInputStream extends InputStream {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(AudioInputStream.class);
    private static final Map<String, AudioInputStreamProvider> CODECS =
            new ConcurrentHashMap<>();

    public static AudioInputStream create(Resource resource)
            throws IOException {
        String mime = resource.getAttributes().getMIMEType();
        Optional<AudioInputStreamProvider> codec = get(mime);
        if (codec.isPresent()) {
            return codec.get().get(resource.read());
        }
        throw new IOException("No compatible decoder found for type: " + mime);
    }

    public static boolean playable(Resource resource) throws IOException {
        return playable(resource.getAttributes().getMIMEType());
    }

    public static boolean playable(String mime) {
        return get(mime) != null;
    }

    private static Optional<AudioInputStreamProvider> get(String mime) {
        Optional<AudioInputStreamProvider> codec =
                Optional.ofNullable(CODECS.get(mime));
        if (!codec.isPresent()) {
            codec = loadService(mime);
            if (codec.isPresent()) {
                CODECS.put(mime, codec.get());
            }
        }
        return codec;
    }

    private static Optional<AudioInputStreamProvider> loadService(String mime) {
        for (AudioInputStreamProvider codec : ServiceLoader
                .load(AudioInputStreamProvider.class)) {
            try {
                if (codec.accepts(mime)) {
                    LOGGER.debug("Loaded audio codec ({}): {}", mime,
                            codec.getClass().getName());
                    return Optional.of(codec);
                }
            } catch (ServiceConfigurationError e) {
                LOGGER.warn("Unable to load codec provider: {}", e.toString());
            }
        }
        return Optional.empty();
    }

    public abstract int getChannels();

    public abstract int getRate();

    public ByteBuffer readNow() throws IOException {
        ByteArrayOutputStream streamOut = new ByteArrayOutputStream();
        ProcessStream.process(this, streamOut::write);
        byte[] bytes = streamOut.toByteArray();
        streamOut.close();
        ByteBuffer buffer = BufferCreatorDirect.byteBuffer(bytes.length);
        buffer.put(bytes);
        buffer.rewind();
        return buffer;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }
}
