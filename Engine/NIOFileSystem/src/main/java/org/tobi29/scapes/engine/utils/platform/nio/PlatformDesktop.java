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

package org.tobi29.scapes.engine.utils.platform.nio;

import org.tobi29.scapes.engine.utils.io.filesystem.FileSystem;
import org.tobi29.scapes.engine.utils.io.filesystem.javanio.JavaNIOFileSystem;
import org.tobi29.scapes.engine.utils.platform.Platform;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public abstract class PlatformDesktop extends Platform {
    protected final boolean is64Bit;
    private final String name;
    private final String version;
    private final String arch;

    protected PlatformDesktop() {
        name = System.getProperty("os.name");
        version = System.getProperty("os.version");
        arch = System.getProperty("os.arch");
        is64Bit = arch.contains("64");
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String getArch() {
        return arch;
    }

    @Override
    public boolean is64Bit() {
        return is64Bit;
    }

    @Override
    public FileSystem getFileFileSystem(String id, String root)
            throws IOException {
        return new JavaNIOFileSystem(Paths.get(root), id);
    }

    @Override
    public FileSystem getTempFileFileSystem(String id) throws IOException {
        return new JavaNIOFileSystem(Files.createTempDirectory("ScapesEngine"),
                id);
    }
}
