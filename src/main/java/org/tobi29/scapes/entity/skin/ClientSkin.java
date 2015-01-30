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

package org.tobi29.scapes.entity.skin;

import org.tobi29.scapes.engine.opengl.GraphicsSystem;
import org.tobi29.scapes.engine.opengl.OpenGLFunction;
import org.tobi29.scapes.engine.opengl.texture.TextureCustom;

import java.nio.ByteBuffer;

public class ClientSkin extends TextureCustom {
    private final String checksum;
    private int unusedTicks;

    public ClientSkin(ByteBuffer buffer, String checksum) {
        super(64, 64, buffer);
        this.checksum = checksum;
    }

    public void setImage(ByteBuffer buffer) {
        this.buffer = buffer;
        markDisposed();
    }

    @OpenGLFunction
    @Override
    public void bind(GraphicsSystem graphics) {
        unusedTicks = 0;
        super.bind(graphics);
    }

    public String getChecksum() {
        return checksum;
    }

    protected int increaseTicks() {
        return unusedTicks++;
    }
}
