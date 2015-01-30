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

package org.tobi29.scapes.engine.utils.io.filesystem.javanio;

import org.tobi29.scapes.engine.utils.io.filesystem.FileSystem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class JavaNIOFileSystem extends JavaNIODirectory implements FileSystem {
    public JavaNIOFileSystem(Path path, String id) throws IOException {
        super(path, id);
        Files.createDirectories(path);
    }
}
