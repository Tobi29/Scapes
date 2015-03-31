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

import org.tobi29.scapes.engine.utils.io.filesystem.FileContents;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class JavaNIOFileContents implements FileContents {
    final Path path;
    FileChannel channel;

    protected JavaNIOFileContents(Path path) {
        this.path = path;
    }

    @Override
    public boolean open() throws IOException {
        boolean existed;
        if (Files.exists(path)) {
            existed = true;
        } else {
            existed = false;
            Files.createFile(path);
        }
        channel = FileChannel
                .open(path, StandardOpenOption.READ, StandardOpenOption.WRITE);
        return existed;
    }

    @Override
    public int read(ByteBuffer buffer, long position) throws IOException {
        return channel.read(buffer, position);
    }

    @Override
    public int write(ByteBuffer buffer, long position) throws IOException {
        return channel.write(buffer, position);
    }

    @Override
    public long getSize() throws IOException {
        return channel.size();
    }

    @Override
    public void truncate(long position, long size) throws IOException {
        channel.position(position);
        channel.truncate(size);
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }
}
