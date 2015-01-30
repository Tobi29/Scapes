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

package org.tobi29.scapes.engine.opengl.scenes;

import org.tobi29.scapes.engine.GameState;
import org.tobi29.scapes.engine.ScapesEngine;
import org.tobi29.scapes.engine.gui.GuiComponent;
import org.tobi29.scapes.engine.opengl.FBO;
import org.tobi29.scapes.engine.opengl.GraphicsSystem;
import org.tobi29.scapes.engine.opengl.shader.Shader;
import org.tobi29.scapes.engine.utils.Pair;

import java.util.Collections;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public abstract class Scene {
    protected final Set<GuiComponent> guis =
            Collections.newSetFromMap(new ConcurrentHashMap<>());
    protected final Queue<Pair<Boolean, GuiComponent>> changeGuis =
            new ConcurrentLinkedQueue<>();
    protected GameState state;

    public void addGui(GuiComponent add) {
        changeGuis.add(new Pair<>(true, add));
    }

    public void removeGui(GuiComponent remove) {
        changeGuis.add(new Pair<>(false, remove));
    }

    public void removeAllGui() {
        guis.stream().map(component -> new Pair<>(false, component))
                .forEach(changeGuis::add);
    }

    public void stepGui(ScapesEngine engine) {
        while (!changeGuis.isEmpty()) {
            Pair<Boolean, GuiComponent> component = changeGuis.poll();
            if (component.a) {
                guis.add(component.b);
            } else {
                guis.remove(component.b);
                component.b.removed();
            }
        }
        guis.forEach(gui -> gui.update(engine));
    }

    public void renderGui(GraphicsSystem graphics, Shader shader) {
        for (GuiComponent gui : guis) {
            gui.render(graphics, shader, state.getFont());
        }
    }

    public GameState getState() {
        return state;
    }

    public void setState(GameState state) {
        this.state = state;
    }

    public abstract void init(GraphicsSystem graphics);

    public abstract void renderScene(GraphicsSystem graphics);

    public Shader postProcessing(GraphicsSystem graphics, int pass) {
        return graphics.getShaderManager()
                .getShader("Engine:shader/Textured", graphics);
    }

    public int getWidth(int width) {
        return width;
    }

    public int getHeight(int height) {
        return height;
    }

    public int getRenderPasses() {
        return 1;
    }

    public int getColorAttachments() {
        return 1;
    }

    public void initFBO(int i, FBO fbo) {
    }

    public abstract void dispose(GraphicsSystem graphics);
}
