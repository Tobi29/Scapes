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

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TagStructureBinary {
    // Header
    protected static final byte[] HEADER_MAGIC = {'S', 'T', 'A', 'G'};
    protected static final byte HEADER_VERSION = 0x1;
    // Components
    //  Structure
    protected static final byte ID_STRUCTURE_BEGIN = 0x10;
    protected static final byte ID_STRUCTURE_TERMINATE = 0x11;
    protected static final byte ID_STRUCTURE_EMPTY = 0x12;
    //  List
    protected static final byte ID_LIST_BEGIN = 0x20;
    protected static final byte ID_LIST_TERMINATE = 0x21;
    protected static final byte ID_LIST_EMPTY = 0x22;
    //  Tags
    //   Boolean
    protected static final byte ID_TAG_BOOLEAN = 0x30;
    //   Byte
    protected static final byte ID_TAG_BYTE = 0x40;
    protected static final byte ID_TAG_BYTE_ARRAY = 0x41;
    //   Integer
    protected static final byte ID_TAG_INT_16 = 0x50;
    protected static final byte ID_TAG_INT_32 = 0x51;
    protected static final byte ID_TAG_INT_64 = 0x52;
    //   Float
    protected static final byte ID_TAG_FLOAT_32 = 0x60;
    protected static final byte ID_TAG_FLOAT_64 = 0x61;
    //   String
    protected static final byte ID_TAG_STRING = 0x71;
    // UTF-8
    protected static final Charset CHARSET = StandardCharsets.UTF_8;

    protected static String readKey(DataInputStream streamIn,
            KeyDictionary dictionary) throws IOException {
        byte alias = streamIn.readByte();
        if (alias == -1) {
            return readString(streamIn);
        } else {
            return dictionary.getKey(alias);
        }
    }

    protected static String readString(DataInputStream streamIn)
            throws IOException {
        int length = streamIn.readUnsignedByte();
        if (length == 0xFF) {
            length = streamIn.readInt();
        }
        byte[] array = new byte[length];
        streamIn.readFully(array);
        return new String(array, CHARSET);
    }

    protected static byte[] readArray(DataInputStream streamIn)
            throws IOException {
        int length = streamIn.readUnsignedShort();
        if (length == 0xFFFF) {
            length = streamIn.readInt();
        }
        byte[] array = new byte[length];
        streamIn.readFully(array);
        return array;
    }

    protected static void writeKey(String key, DataOutputStream streamOut,
            KeyDictionary dictionary) throws IOException {
        Byte alias = dictionary.getAlias(key);
        if (alias == null) {
            streamOut.writeByte(0xFF);
            writeString(key, streamOut);
        } else {
            streamOut.writeByte(alias);
        }
    }

    protected static void writeString(String value, DataOutputStream streamOut)
            throws IOException {
        byte[] array = value.getBytes(CHARSET);
        if (array.length >= 0xFF) {
            streamOut.writeByte(0xFF);
            streamOut.writeInt(array.length);
        } else {
            streamOut.writeByte(array.length);
        }
        streamOut.write(array);
    }

    protected static void writeArray(byte[] array, DataOutputStream streamOut)
            throws IOException {
        if (array.length >= 0xFFFF) {
            streamOut.writeShort(0xFFFF);
            streamOut.writeInt(array.length);
        } else {
            streamOut.writeShort(array.length);
        }
        streamOut.write(array);
    }

    public static TagStructure write(TagStructure tagStructure,
            OutputStream streamOut) throws IOException {
        write(tagStructure, streamOut, (byte) -1);
        return tagStructure;
    }

    public static TagStructure write(TagStructure tagStructure,
            OutputStream streamOut, byte compression) throws IOException {
        write(tagStructure, streamOut, compression, true);
        return tagStructure;
    }

    public static TagStructure write(TagStructure tagStructure,
            OutputStream streamOut, byte compression, boolean useDictionary)
            throws IOException {
        tagStructure.write(new TagStructureWriterBinary(streamOut, compression,
                useDictionary));
        return tagStructure;
    }

    public static TagStructure read(InputStream streamIn) throws IOException {
        return read(new TagStructure(), streamIn);
    }

    public static TagStructure read(TagStructure tagStructure,
            InputStream streamIn) throws IOException {
        tagStructure.read(new TagStructureReaderBinary(streamIn));
        return tagStructure;
    }

    protected static class KeyDictionary {
        protected final List<String> keyAliases = new ArrayList<>();
        protected final Map<String, Byte> keyAliasMap =
                new ConcurrentHashMap<>();
        protected final Map<Byte, String> aliasKeyMap =
                new ConcurrentHashMap<>();
        protected byte currentId;

        protected KeyDictionary() {
        }

        protected KeyDictionary(DataInputStream streamIn) throws IOException {
            int length = streamIn.readByte();
            if (length < 0) {
                length += 256;
            }
            while (length-- > 0) {
                addKeyAlias(readString(streamIn));
            }
        }

        protected KeyDictionary(TagStructure tagStructure) {
            Map<String, KeyOccurrence> keys = new ConcurrentHashMap<>();
            analyze(tagStructure, keys);
            if (keys.size() > 255) {
                keys.entrySet().stream().sorted((entry1, entry2) ->
                        entry1.getValue().count == entry2.getValue().count ? 0 :
                                entry1.getValue().count <
                                        entry2.getValue().count ? 1 : -1)
                        .limit(255).map(Map.Entry::getKey)
                        .forEach(this::addKeyAlias);
            } else {
                keys.entrySet().stream().map(Map.Entry::getKey)
                        .forEach(this::addKeyAlias);
            }
        }

        @SuppressWarnings("unchecked")
        private static void analyze(TagStructure tagStructure,
                Map<String, KeyOccurrence> keys) {
            for (Map.Entry<String, Object> entry : tagStructure
                    .getTagEntrySet()) {
                String key = entry.getKey();
                KeyOccurrence occurrence = keys.get(key);
                if (occurrence == null) {
                    keys.put(key, new KeyOccurrence(key));
                } else {
                    occurrence.count += occurrence.length;
                }
                Object value = entry.getValue();
                if (value instanceof TagStructure) {
                    analyze((TagStructure) value, keys);
                } else if (value instanceof List<?>) {
                    ((List<TagStructure>) value).stream()
                            .forEach(child -> analyze(child, keys));
                }
            }
        }

        protected String getKey(byte alias) {
            return aliasKeyMap.get(alias);
        }

        protected void addKeyAlias(String key) {
            keyAliases.add(key);
            keyAliasMap.put(key, currentId);
            aliasKeyMap.put(currentId++, key);
        }

        protected Byte getAlias(String key) {
            return keyAliasMap.get(key);
        }

        protected void write(DataOutputStream streamOut) throws IOException {
            streamOut.writeByte(keyAliases.size());
            for (String key : keyAliases) {
                writeString(key, streamOut);
            }
        }

        private static class KeyOccurrence {
            private final int length;
            private int count;

            private KeyOccurrence(String key) {
                length = key.length();
                count += length;
            }
        }
    }
}
