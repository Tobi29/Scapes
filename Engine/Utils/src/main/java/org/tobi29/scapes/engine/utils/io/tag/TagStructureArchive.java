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

package org.tobi29.scapes.engine.utils.io.tag;

import org.tobi29.scapes.engine.utils.Pair;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class TagStructureArchive {
    private static final byte HEADER_VERSION = 1;
    private static final byte[] HEADER_MAGIC = {'S', 'T', 'A', 'R'};
    private static final Charset CHARSET = StandardCharsets.UTF_8;
    private final Map<String, byte[]> tagStructures = new ConcurrentHashMap<>();
    private final ByteArrayOutputStream byteStreamOut;

    public TagStructureArchive() {
        this(new ByteArrayOutputStream());
    }

    public TagStructureArchive(ByteArrayOutputStream byteStreamOut) {
        this.byteStreamOut = byteStreamOut;
    }

    public static Optional<TagStructure> extract(String name, InputStream streamIn)
            throws IOException {
        List<Entry> entries = readHeader(streamIn);
        int offset = 0;
        for (Entry entry : entries) {
            if (entry.name.equals(name)) {
                new DataInputStream(streamIn).skipBytes(offset);
                TagStructure tagStructure = new TagStructure();
                tagStructure.read(new TagStructureReaderBinary(streamIn));
                return Optional.of(tagStructure);
            }
            offset += entry.length;
        }
        return Optional.empty();
    }

    public static List<Entry> readHeader(InputStream streamIn)
            throws IOException {
        DataInputStream dataStreamIn = new DataInputStream(streamIn);
        byte[] magic = new byte[HEADER_MAGIC.length];
        dataStreamIn.readFully(magic);
        if (!Arrays.equals(magic, magic)) {
            throw new IOException("Not in tag-archive format! (Magic-Header: " +
                    Arrays.toString(magic) +
                    ')');
        }
        byte version = dataStreamIn.readByte();
        if (version > HEADER_VERSION) {
            throw new IOException(
                    "Unsupported version or not in tag-container format! (Version: " +
                            version + ')');
        }
        List<Entry> entries = new ArrayList<>();
        while (true) {
            int length = dataStreamIn.readByte();
            if (length < 0) {
                length += 256;
            }
            if (length == 255) {
                break;
            } else if (length == 254) {
                length = dataStreamIn.readInt();
            }
            byte[] array = new byte[length];
            dataStreamIn.readFully(array);
            String name = new String(array, CHARSET);
            length = dataStreamIn.readInt();
            entries.add(new Entry(name, length));
        }
        return entries;
    }

    public void setTagStructure(String key, TagStructure tagStructure)
            throws IOException {
        setTagStructure(key, tagStructure, (byte) 1);
    }

    public synchronized void setTagStructure(String key,
            TagStructure tagStructure, byte compression) throws IOException {
        TagStructureBinary.write(tagStructure, byteStreamOut, compression);
        byte[] array = byteStreamOut.toByteArray();
        byteStreamOut.reset();
        tagStructures.put(key, array);
    }

    public synchronized TagStructure getTagStructure(String key)
            throws IOException {
        byte[] array = tagStructures.get(key);
        if (array == null) {
            return null;
        }
        TagStructure tagStructure = new TagStructure();
        try (ByteArrayInputStream streamIn = new ByteArrayInputStream(array)) {
            TagStructureBinary.read(tagStructure, streamIn);
        }
        return tagStructure;
    }

    public synchronized void removeTagStructure(String key) {
        tagStructures.remove(key);
    }

    public boolean hasTagStructure(String key) {
        return tagStructures.containsKey(key);
    }

    public Collection<String> getKeys() {
        return tagStructures.keySet();
    }

    @SuppressWarnings("AccessToStaticFieldLockedOnInstance")
    public synchronized void write(OutputStream streamOut) throws IOException {
        List<Pair<String, byte[]>> entries = tagStructures.entrySet().stream()
                .map(entry -> new Pair<>(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
        DataOutputStream dataStreamOut = new DataOutputStream(streamOut);
        dataStreamOut.write(HEADER_MAGIC);
        dataStreamOut.writeByte(HEADER_VERSION);
        for (Pair<String, byte[]> entry : entries) {
            byte[] array = entry.a.getBytes(CHARSET);
            if (array.length >= 254) {
                dataStreamOut.writeByte(-2);
                dataStreamOut.writeInt(array.length);
            } else {
                dataStreamOut.writeByte((byte) array.length);
            }
            dataStreamOut.write(array);
            dataStreamOut.writeInt(entry.b.length);
        }
        dataStreamOut.writeByte(-1);
        for (Pair<String, byte[]> entry : entries) {
            dataStreamOut.write(entry.b);
        }
    }

    public synchronized void read(InputStream streamIn) throws IOException {
        List<Entry> entries = readHeader(streamIn);
        DataInputStream dataStreamIn = new DataInputStream(streamIn);
        for (Entry entry : entries) {
            byte[] array = new byte[entry.length];
            dataStreamIn.readFully(array);
            tagStructures.put(entry.name, array);
        }
    }

    public static class Entry {
        private final String name;
        private final int length;

        private Entry(String name, int length) {
            this.name = name;
            this.length = length;
        }

        public String getName() {
            return name;
        }
    }
}
