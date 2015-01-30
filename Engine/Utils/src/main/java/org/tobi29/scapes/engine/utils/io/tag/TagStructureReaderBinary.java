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
import org.tobi29.scapes.engine.utils.io.MarkedInflaterInputStream;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

public class TagStructureReaderBinary extends TagStructureBinary
        implements TagStructureReader {
    private final InputStream streamIn;
    private byte compression;
    private KeyDictionary dictionary;
    private DataInputStream structureStreamIn;
    private Inflater inflater;

    public TagStructureReaderBinary(InputStream streamIn) {
        this.streamIn = streamIn;
    }

    @Override
    public void begin() throws IOException {
        DataInputStream dataStreamIn = new DataInputStream(streamIn);
        byte[] magic = new byte[HEADER_MAGIC.length];
        dataStreamIn.readFully(magic);
        if (!Arrays.equals(magic, magic)) {
            throw new IOException("Not in tag format! (Magic-Header: " +
                    Arrays.toString(magic) +
                    ')');
        }
        byte version = dataStreamIn.readByte();
        if (version > HEADER_VERSION) {
            throw new IOException(
                    "Unsupported version or not in tag format! (Version: " +
                            version + ')');
        }
        compression = dataStreamIn.readByte();
        if (compression >= 0) {
            inflater = new Inflater();
            InflaterInputStream inflaterStreamIn =
                    new MarkedInflaterInputStream(streamIn, inflater, 1024);
            structureStreamIn = new DataInputStream(inflaterStreamIn);
        } else {
            structureStreamIn = dataStreamIn;
        }
        dictionary = new KeyDictionary(structureStreamIn);
    }

    @Override
    public void end() throws IOException {
        if (compression >= 0) {
            inflater.end();
        }
    }

    @Override
    public Pair<String, Object> next() throws IOException {
        byte componentId = structureStreamIn.readByte();
        if (componentId == ID_STRUCTURE_TERMINATE) {
            return new Pair<>(null, SpecialNext.STRUCTURE_TERMINATE);
        } else if (componentId == ID_LIST_TERMINATE) {
            return new Pair<>(null, SpecialNext.LIST_TERMINATE);
        }
        String key = readKey(structureStreamIn, dictionary);
        Object tag;
        switch (componentId) {
            case ID_STRUCTURE_BEGIN:
                tag = SpecialNext.STRUCTURE;
                break;
            case ID_STRUCTURE_EMPTY:
                tag = SpecialNext.STRUCTURE_EMPTY;
                break;
            case ID_LIST_BEGIN:
                tag = SpecialNext.LIST;
                break;
            case ID_LIST_EMPTY:
                tag = SpecialNext.LIST_EMPTY;
                break;
            case ID_TAG_BOOLEAN:
                tag = structureStreamIn.readBoolean();
                break;
            case ID_TAG_BYTE:
                tag = structureStreamIn.readByte();
                break;
            case ID_TAG_BYTE_ARRAY:
                tag = readArray(structureStreamIn);
                break;
            case ID_TAG_INT_16:
                tag = structureStreamIn.readShort();
                break;
            case ID_TAG_INT_32:
                tag = structureStreamIn.readInt();
                break;
            case ID_TAG_INT_64:
                tag = structureStreamIn.readLong();
                break;
            case ID_TAG_FLOAT_32:
                tag = structureStreamIn.readFloat();
                break;
            case ID_TAG_FLOAT_64:
                tag = structureStreamIn.readDouble();
                break;
            case ID_TAG_STRING:
                tag = readString(structureStreamIn);
                break;
            default:
                throw new IOException(
                        "Not in tag format! (Invalid component-id: " +
                                componentId + ')');
        }
        return new Pair<>(key, tag);
    }

    @Override
    public void beginStructure() throws IOException {
    }

    @Override
    public void endStructure() throws IOException {
    }

    @Override
    public void beginList() throws IOException {
    }

    @Override
    public void endList() throws IOException {
    }
}
