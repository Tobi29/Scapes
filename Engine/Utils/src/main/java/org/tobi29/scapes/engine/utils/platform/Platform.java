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

import org.tobi29.scapes.engine.utils.UnsupportedJVMException;
import org.tobi29.scapes.engine.utils.io.ReadSource;
import org.tobi29.scapes.engine.utils.platform.spi.PlatformProvider;
import org.tobi29.scapes.engine.utils.ui.font.GlyphRenderer;

import java.util.ServiceLoader;

public abstract class Platform {
    private static final Platform PLATFORM;

    static {
        PLATFORM = loadPlatform().getPlatform();
    }

    private static PlatformProvider loadPlatform() {
        for (PlatformProvider platform : ServiceLoader
                .load(PlatformProvider.class)) {
            if (platform.available()) {
                return platform;
            }
        }
        throw new UnsupportedJVMException("Platform not supported!");
    }

    public static Platform getPlatform() {
        return PLATFORM;
    }

    public abstract String getAppData(String name);

    public abstract String getID();

    public abstract String getName();

    public abstract String getVersion();

    public abstract String getArch();

    public abstract boolean is64Bit();

    public abstract PlatformDialogs createDialogHandler();

    public abstract GlyphRenderer getGlyphRenderer(String font, int size);

    public abstract boolean loadFont(ReadSource font);
}
