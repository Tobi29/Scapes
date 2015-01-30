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

package org.tobi29.scapes.engine.utils.tests;

import org.junit.Assert;
import org.junit.Test;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.io.tag.TagStructureBinary;
import org.tobi29.scapes.engine.utils.io.tag.TagStructureJSON;
import org.tobi29.scapes.engine.utils.io.tag.TagStructureXML;
import org.tobi29.scapes.engine.utils.tests.util.TagStructureTemplate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class TagStructureTest {
    @Test
    public void testUncompressedBinaryFile() throws IOException {
        TagStructure tagStructure = TagStructureTemplate.createTagStructure();
        ByteArrayOutputStream streamOut = new ByteArrayOutputStream();
        TagStructureBinary.write(tagStructure, streamOut);
        TagStructure read = new TagStructure();
        InputStream streamIn =
                new ByteArrayInputStream(streamOut.toByteArray());
        TagStructureBinary.read(read, streamIn);
        Assert.assertEquals("Read structure doesn't match written one",
                tagStructure, read);
    }

    @Test
    public void testCompressedBinaryFile() throws IOException {
        TagStructure tagStructure = TagStructureTemplate.createTagStructure();
        ByteArrayOutputStream streamOut = new ByteArrayOutputStream();
        TagStructureBinary.write(tagStructure, streamOut, (byte) 1);
        TagStructure read = new TagStructure();
        InputStream streamIn =
                new ByteArrayInputStream(streamOut.toByteArray());
        TagStructureBinary.read(read, streamIn);
        Assert.assertEquals("Read structure doesn't match written one",
                tagStructure, read);
    }

    @Test
    public void testXMLFile() throws IOException {
        TagStructure tagStructure = TagStructureTemplate.createTagStructure();
        ByteArrayOutputStream streamOut = new ByteArrayOutputStream();
        TagStructureXML.write(tagStructure, streamOut);
        TagStructure read = new TagStructure();
        InputStream streamIn =
                new ByteArrayInputStream(streamOut.toByteArray());
        TagStructureXML.read(read, streamIn);
        Assert.assertEquals("Read structure doesn't match written one",
                tagStructure, read);
    }

    @Test
    public void testJSONFile() throws IOException {

        TagStructure tagStructure = TagStructureTemplate.createTagStructure();
        ByteArrayOutputStream streamOut = new ByteArrayOutputStream();
        TagStructureJSON.write(tagStructure, streamOut);
        TagStructure read = new TagStructure();
        InputStream streamIn =
                new ByteArrayInputStream(streamOut.toByteArray());
        streamOut.reset();
        TagStructureJSON.write(read, streamOut);
        TagStructure reread = new TagStructure();
        streamIn = new ByteArrayInputStream(streamOut.toByteArray());
        TagStructureJSON.read(read, streamIn);
        Assert.assertEquals("Read structure doesn't match written one", read,
                reread);
    }
}
