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

package org.tobi29.scapes.engine.utils.platform.swt;

import org.eclipse.swt.widgets.Display;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.engine.utils.io.ProcessStream;
import org.tobi29.scapes.engine.utils.io.ReadSource;
import org.tobi29.scapes.engine.utils.platform.Platform;
import org.tobi29.scapes.engine.utils.platform.PlatformDialogs;
import org.tobi29.scapes.engine.utils.ui.font.GlyphRenderer;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

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
    public PlatformDialogs createDialogHandler(String id) {
        return new PlatformDialogsSWT(id);
    }

    @Override
    public GlyphRenderer getGlyphRenderer(String font, int size) {
        return new SWTGlyphRenderer(font, size);
    }

    @Override
    public boolean loadFont(ReadSource font) {
        Logger logger = LoggerFactory.getLogger(PlatformDesktop.class);
        String fontPath = null;
        try {
            Path fontFile = Files.createTempFile("Scapes", ".ttf");
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    Files.delete(fontFile);
                } catch (IOException e) {
                    logger.warn("Failed to delete temporary font file: {}",
                            e.toString());
                }
            }));
            try (OutputStream streamOut = Files.newOutputStream(fontFile)) {
                ProcessStream.processSource(font, streamOut::write);
            }
            fontPath = fontFile.toAbsolutePath().toString();
        } catch (IOException e) {
            logger.warn("Failed to store font file: {}", e.toString());
            return false;
        }
        if (Display.getDefault().loadFont(fontPath)) {
            return true;
        }
        logger.warn("Failed to load font: {}", font);
        return false;
    }
}
