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
package org.tobi29.scapes.plugins;

import org.tobi29.scapes.engine.utils.Checksum;
import org.tobi29.scapes.engine.utils.VersionUtil;
import org.tobi29.scapes.engine.utils.io.BufferedReadChannelStream;
import org.tobi29.scapes.engine.utils.io.ChecksumUtil;
import org.tobi29.scapes.engine.utils.io.filesystem.FileUtil;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.io.tag.TagStructureJSON;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.channels.Channels;
import java.nio.file.Path;
import java.util.zip.ZipFile;

public class PluginFile {
    private final Path path;
    private final Checksum checksum;
    private final String name, parent, mainClass;
    private final VersionUtil.Version version, scapesVersion;

    public PluginFile(Path path) throws IOException {
        this.path = path;
        checksum = FileUtil.readReturn(path, ChecksumUtil::checksum);
        try (ZipFile zip = new ZipFile(path.toFile())) {
            TagStructure tagStructure = TagStructureJSON
                    .read(new BufferedReadChannelStream(Channels.newChannel(
                            zip.getInputStream(zip.getEntry("Plugin.json")))));
            name = tagStructure.getString("Name");
            parent = tagStructure.getString("Parent");
            version = VersionUtil.get(tagStructure.getString("Version"));
            scapesVersion =
                    VersionUtil.get(tagStructure.getString("ScapesVersion"));
            mainClass = tagStructure.getString("MainClass");
        } catch (VersionUtil.VersionException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    public VersionUtil.Version version() {
        return version;
    }

    public VersionUtil.Version scapesVersion() {
        return scapesVersion;
    }

    public Path file() {
        return path;
    }

    public String name() {
        return name;
    }

    public String parent() {
        return parent;
    }

    public Checksum checksum() {
        return checksum;
    }

    public Plugin plugin(ClassLoader classLoader) throws IOException {
        try {
            return (Plugin) classLoader.loadClass(mainClass).getConstructor()
                    .newInstance();
        } catch (ClassNotFoundException | InvocationTargetException | InstantiationException | NoSuchMethodException | IllegalAccessException e) {
            throw new IOException(e);
        }
    }
}
