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

package org.tobi29.scapes.engine.utils.io.filesystem;

import org.tobi29.scapes.engine.utils.platform.PlatformDialogs;

import java.io.IOException;
import java.util.List;

public interface Directory extends Path {
    String getName();

    boolean exists();

    void make() throws IOException;

    void move(Directory directory) throws IOException;

    void copy(Directory directory) throws IOException;

    void delete() throws IOException;

    FileAttributes getAttributes();

    @Override
    Directory get(String path) throws IOException;

    @Override
    File getResource(String path) throws IOException;

    List<File> listFiles(FileFilter filter) throws IOException;

    default List<File> listFiles() throws IOException {
        return listFiles(file -> true);
    }

    List<Directory> listDirectories(DirectoryFilter filter) throws IOException;

    default List<Directory> listDirectories() throws IOException {
        return listDirectories(directory -> true);
    }

    default List<File> listFilesRecursive(FileFilter filter,
            DirectoryFilter directoryFilter) throws IOException {
        List<File> files = listFiles(filter);
        for (Directory directory : listDirectories(directoryFilter)) {
            files.addAll(directory.listFilesRecursive(filter, directoryFilter));
        }
        return files;
    }

    default List<File> listFilesRecursive(FileFilter filter)
            throws IOException {
        return listFilesRecursive(filter, directory -> true);
    }

    default List<File> listFilesRecursive() throws IOException {
        return listFilesRecursive(file -> true);
    }

    boolean importFromUser(PlatformDialogs.Extension[] extensions, String title,
            boolean multiple, PlatformDialogs dialogs) throws IOException;

    java.io.File getFile();

    @FunctionalInterface
    interface FileFilter {
        boolean accept(File file) throws IOException;
    }

    @FunctionalInterface
    interface DirectoryFilter {
        boolean accept(Directory directory) throws IOException;
    }
}
