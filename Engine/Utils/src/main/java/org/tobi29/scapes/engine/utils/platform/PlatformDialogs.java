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

package org.tobi29.scapes.engine.utils.platform;

import org.tobi29.scapes.engine.utils.DesktopException;

import java.io.File;
import java.util.Optional;

public interface PlatformDialogs {
    File[] openFileDialog(Extension[] extensions, String title,
            boolean multiple);

    Optional<File> saveFileDialog(Extension[] extensions, String title);

    void message(MessageType messageType, String title, String message);

    void openFile(File file);

    void renderTick() throws DesktopException;

    void dispose();

    enum MessageType {
        ERROR,
        INFORMATION,
        WARNING,
        QUESTION,
        PLAIN
    }

    class Extension {
        public final String pattern, name;

        public Extension(String pattern, String name) {
            this.pattern = pattern;
            this.name = name;
        }
    }
}
