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

package org.tobi29.scapes.client.states;

import org.tobi29.scapes.client.gui.GuiDisconnected;
import org.tobi29.scapes.client.states.scenes.SceneError;
import org.tobi29.scapes.engine.GameState;
import org.tobi29.scapes.engine.ScapesEngine;

public class GameStateServerDisconnect extends GameState {
    public GameStateServerDisconnect(String message, ScapesEngine engine) {
        super(engine, new SceneError());
        add(new GuiDisconnected(this, message));
    }

    @Override
    public void dispose() {
    }

    @Override
    public void init() {
    }

    @Override
    public boolean isMouseGrabbed() {
        return false;
    }

    @Override
    public boolean isThreaded() {
        return false;
    }

    @Override
    public boolean forceRender() {
        return false;
    }

    @Override
    public void stepComponent() {
    }
}
