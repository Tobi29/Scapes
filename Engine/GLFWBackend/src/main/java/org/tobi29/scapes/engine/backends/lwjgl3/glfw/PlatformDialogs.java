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

package org.tobi29.scapes.engine.backends.lwjgl3.glfw;

import org.tobi29.scapes.engine.opengl.Container;
import org.tobi29.scapes.engine.utils.Pair;
import org.tobi29.scapes.engine.utils.io.filesystem.Directory;
import org.tobi29.scapes.engine.utils.io.filesystem.File;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;

public interface PlatformDialogs {
    URI[] openFileDialog(Pair<String, String>[] extensions,
            String title, boolean multiple);

    Optional<URI> saveFileDialog(Pair<String, String>[] extensions,
            String title);

    boolean exportToUser(File file, Pair<String, String>[] extensions,
            String title) throws IOException;

    boolean importFromUser(File file, Pair<String, String>[] extensions,
            String title) throws IOException;

    boolean importFromUser(Directory directory,
            Pair<String, String>[] extensions, String title, boolean multiple)
            throws IOException;

    void message(Container.MessageType messageType, String title,
            String message);

    void openFile(URI file);

    void renderTick();

    void dispose();
}