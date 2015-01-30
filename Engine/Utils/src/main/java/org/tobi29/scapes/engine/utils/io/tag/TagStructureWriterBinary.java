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

import org.tobi29.scapes.engine.utils.io.MarkedDeflaterOutputStream;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

public class TagStructureWriterBinary extends TagStructureBinary
        implements TagStructureWriter {
    private final OutputStream streamOut;
    private final byte compression;
    private final boolean useDictionary;
    private KeyDictionary dictionary;
    private DataOutputStream structureStreamOut;
    private Deflater deflater;
    private DeflaterOutputStream deflaterStreamOut;

    public TagStructureWriterBinary(OutputStream streamOut, byte compression) {
        this(streamOut, compression, true);
    }

    public TagStructureWriterBinary(OutputStream streamOut, byte compression,
            boolean useDictionary) {
        this.streamOut = streamOut;
        this.compression = compression;
        this.useDictionary = useDictionary;
    }

    @Override
    public void begin(TagStructure root) throws IOException {
        DataOutputStream dataStreamOut = new DataOutputStream(streamOut);
        dataStreamOut.write(HEADER_MAGIC);
        dataStreamOut.writeByte(HEADER_VERSION);
        dataStreamOut.writeByte(compression);
        if (compression >= 0) {
            deflater = new Deflater(compression);
            deflaterStreamOut =
                    new MarkedDeflaterOutputStream(streamOut, deflater, 1024);
            structureStreamOut = new DataOutputStream(
                    new BufferedOutputStream(deflaterStreamOut, 1024));
        } else {
            structureStreamOut = dataStreamOut;
        }
        if (useDictionary) {
            dictionary = new KeyDictionary(root);
        } else {
            dictionary = new KeyDictionary();
        }
        dictionary.write(structureStreamOut);
    }

    @Override
    public void end() throws IOException {
        if (compression >= 0) {
            structureStreamOut.flush();
            deflaterStreamOut.flush();
            deflaterStreamOut.write(ID_STRUCTURE_TERMINATE);
            deflaterStreamOut.finish();
            deflaterStreamOut.flush();
            deflater.end();
        } else {
            structureStreamOut.writeByte(ID_STRUCTURE_TERMINATE);
            structureStreamOut.flush();
        }
    }

    @Override
    public void beginStructure() throws IOException {
    }

    @Override
    public void beginStructure(String key) throws IOException {
        structureStreamOut.writeByte(ID_STRUCTURE_BEGIN);
        writeKey(key, structureStreamOut, dictionary);
    }

    @Override
    public void endStructure() throws IOException {
        structureStreamOut.writeByte(ID_STRUCTURE_TERMINATE);
    }

    @Override
    public void structureEmpty() throws IOException {
        structureStreamOut.writeByte(ID_STRUCTURE_TERMINATE);
    }

    @Override
    public void structureEmpty(String key) throws IOException {
        structureStreamOut.writeByte(ID_STRUCTURE_EMPTY);
        writeKey(key, structureStreamOut, dictionary);
    }

    @Override
    public void beginList(String key) throws IOException {
        structureStreamOut.writeByte(ID_LIST_BEGIN);
        writeKey(key, structureStreamOut, dictionary);
    }

    @Override
    public void endListWidthTerminate() throws IOException {
        structureStreamOut.writeByte(ID_LIST_TERMINATE);
    }

    @Override
    public void endListWithEmpty() throws IOException {
        structureStreamOut.writeByte(ID_LIST_TERMINATE);
    }

    @Override
    public void listEmpty(String key) throws IOException {
        structureStreamOut.writeByte(ID_LIST_EMPTY);
        writeKey(key, structureStreamOut, dictionary);
    }

    @Override
    public void writeTag(String key, Object tag) throws IOException {
        if (tag instanceof Boolean) {
            structureStreamOut.writeByte(ID_TAG_BOOLEAN);
            writeKey(key, structureStreamOut, dictionary);
            structureStreamOut.writeBoolean((boolean) tag);
        } else if (tag instanceof Byte) {
            structureStreamOut.writeByte(ID_TAG_BYTE);
            writeKey(key, structureStreamOut, dictionary);
            structureStreamOut.writeByte((byte) tag);
        } else if (tag instanceof byte[]) {
            structureStreamOut.writeByte(ID_TAG_BYTE_ARRAY);
            writeKey(key, structureStreamOut, dictionary);
            writeArray((byte[]) tag, structureStreamOut);
        } else if (tag instanceof Short) {
            structureStreamOut.writeByte(ID_TAG_INT_16);
            writeKey(key, structureStreamOut, dictionary);
            structureStreamOut.writeShort((short) tag);
        } else if (tag instanceof Integer) {
            structureStreamOut.writeByte(ID_TAG_INT_32);
            writeKey(key, structureStreamOut, dictionary);
            structureStreamOut.writeInt((int) tag);
        } else if (tag instanceof Long) {
            structureStreamOut.writeByte(ID_TAG_INT_64);
            writeKey(key, structureStreamOut, dictionary);
            structureStreamOut.writeLong((long) tag);
        } else if (tag instanceof Float) {
            structureStreamOut.writeByte(ID_TAG_FLOAT_32);
            writeKey(key, structureStreamOut, dictionary);
            structureStreamOut.writeFloat((float) tag);
        } else if (tag instanceof Double) {
            structureStreamOut.writeByte(ID_TAG_FLOAT_64);
            writeKey(key, structureStreamOut, dictionary);
            structureStreamOut.writeDouble((double) tag);
        } else if (tag instanceof String) {
            structureStreamOut.writeByte(ID_TAG_STRING);
            writeKey(key, structureStreamOut, dictionary);
            writeString((String) tag, structureStreamOut);
        } else {
            throw new IOException("Invalid type: " + tag.getClass());
        }
    }
}
