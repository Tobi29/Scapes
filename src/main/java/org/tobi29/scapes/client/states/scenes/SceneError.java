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

package org.tobi29.scapes.client.states.scenes;

import org.tobi29.scapes.engine.opengl.GraphicsSystem;

public class SceneError extends SceneMenu {
    public SceneError() {
        setSpeed(0.0f);
    }

    @Override
    protected void loadTextures() {
        GraphicsSystem graphics = state.getEngine().getGraphics();
        for (int i = 0; i < 6; i++) {
            setBackground(graphics.getTextureManager()
                    .getTexture("Scapes:image/gui/panorama/error/Panorama" + i),
                    i);
        }
    }
}
