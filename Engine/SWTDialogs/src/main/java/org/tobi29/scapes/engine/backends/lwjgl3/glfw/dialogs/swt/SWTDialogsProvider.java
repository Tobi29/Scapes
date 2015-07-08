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

package org.tobi29.scapes.engine.backends.lwjgl3.glfw.dialogs.swt;

import org.eclipse.swt.widgets.Display;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.engine.ScapesEngine;
import org.tobi29.scapes.engine.backends.lwjgl3.glfw.PlatformDialogs;
import org.tobi29.scapes.engine.backends.lwjgl3.glfw.spi.GLFWDialogsProvider;
import org.tobi29.scapes.engine.gui.GlyphRenderer;
import org.tobi29.scapes.engine.utils.io.FileUtil;
import org.tobi29.scapes.engine.utils.io.ProcessStream;
import org.tobi29.scapes.engine.utils.io.ReadSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class SWTDialogsProvider implements GLFWDialogsProvider {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(SWTDialogsProvider.class);

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public PlatformDialogs createDialogs(ScapesEngine engine) {
        return new PlatformDialogsSWT(engine.getGame().getName());
    }

    @Override
    public boolean loadFont(ReadSource font) {
        String fontPath = null;
        try {
            Path fontFile = Files.createTempFile("Scapes", ".ttf");
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    Files.delete(fontFile);
                } catch (IOException e) {
                    LOGGER.warn("Failed to delete temporary font file: {}",
                            e.toString());
                }
            }));
            FileUtil.write(fontFile,
                    stream -> ProcessStream.processSource(font, stream::put));
            fontPath = fontFile.toAbsolutePath().toString();
        } catch (IOException e) {
            LOGGER.warn("Failed to store font file: {}", e.toString());
            return false;
        }
        if (Display.getDefault().loadFont(fontPath)) {
            return true;
        }
        LOGGER.warn("Failed to load font: {}", font);
        return false;
    }

    @Override
    public GlyphRenderer createGlyphRenderer(String fontName, int size) {
        return new SWTGlyphRenderer(fontName, size);
    }
}
