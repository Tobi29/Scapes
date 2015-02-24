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

package org.tobi29.scapes.engine.gui;

import org.tobi29.scapes.engine.ScapesEngine;
import org.tobi29.scapes.engine.opengl.FontRenderer;
import org.tobi29.scapes.engine.opengl.GraphicsSystem;
import org.tobi29.scapes.engine.opengl.matrix.Matrix;
import org.tobi29.scapes.engine.opengl.matrix.MatrixStack;
import org.tobi29.scapes.engine.opengl.shader.Shader;
import org.tobi29.scapes.engine.utils.Pair;

import java.util.Collections;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicLong;

public abstract class GuiComponent
        implements GuiComponentEventListenerContainer,
        Comparable<GuiComponent> {
    private static final AtomicLong UID_COUNTER =
            new AtomicLong(Long.MIN_VALUE);
    protected final Queue<Pair<Boolean, GuiComponent>> changeComponents =
            new ConcurrentLinkedQueue<>();
    protected final Set<GuiComponent> components =
            new ConcurrentSkipListSet<>();
    protected final Set<GuiComponentEventListener> events =
            Collections.newSetFromMap(new ConcurrentHashMap<>());
    protected final Set<GuiComponentHoverListener> hovers =
            Collections.newSetFromMap(new ConcurrentHashMap<>());
    protected final Set<GuiComponentEventListener> rightEvents =
            Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final long uid = UID_COUNTER.getAndIncrement();
    protected GuiComponent parent;
    protected boolean visible = true;
    protected int x, y, width, height;

    protected GuiComponent(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void add(GuiComponent add) {
        changeComponents.add(new Pair<>(true, add));
    }

    public void remove(GuiComponent remove) {
        changeComponents.add(new Pair<>(false, remove));
    }

    public void removeAll() {
        components.stream().map(component -> new Pair<>(false, component))
                .forEach(changeComponents::add);
    }

    @Override
    public void addLeftClick(GuiComponentEventListener add) {
        events.add(add);
    }

    @Override
    public void addRightClick(GuiComponentEventListener add) {
        rightEvents.add(add);
    }

    @Override
    public void addHover(GuiComponentHoverListener add) {
        hovers.add(add);
    }

    @Override
    public void removeLeftClick(GuiComponentEventListener remove) {
        events.remove(remove);
    }

    @Override
    public void removeRightClock(GuiComponentEventListener remove) {
        rightEvents.remove(remove);
    }

    @Override
    public void removeHover(GuiComponentHoverListener remove) {
        hovers.remove(remove);
    }

    @Override
    public void clickLeft(GuiComponentEvent event, ScapesEngine engine) {
        for (GuiComponentEventListener event1 : events) {
            event1.click(event);
        }
        getGui().ifPresent(gui -> gui.setLastClicked(this));
    }

    @Override
    public void clickRight(GuiComponentEvent event, ScapesEngine engine) {
        for (GuiComponentEventListener rightEvent : rightEvents) {
            rightEvent.click(event);
        }
        getGui().ifPresent(gui -> gui.setLastClicked(this));
    }

    @Override
    public void hover(GuiComponentEvent event) {
        for (GuiComponentHoverListener hover : hovers) {
            hover.hover(event);
        }
    }

    public void removed() {
        components.forEach(GuiComponent::removed);
    }

    public boolean checkInside(double x, double y) {
        return x >= this.x && y >= this.y && x < this.x + width &&
                y < this.y + height;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public Optional<Gui> getGui() {
        GuiComponent other = this;
        while (true) {
            if (other.parent != null) {
                other = other.parent;
                continue;
            }
            if (other instanceof Gui) {
                return Optional.of((Gui) other);
            }
            return Optional.empty();
        }
    }

    public void render(GraphicsSystem graphics, Shader shader,
            FontRenderer font, double delta) {
        if (visible) {
            MatrixStack matrixStack = graphics.getMatrixStack();
            Matrix matrix = matrixStack.push();
            transform(matrix);
            renderComponent(graphics, shader, font, delta);
            components.stream().forEach(component -> component
                    .render(graphics, shader, font, delta));
            renderOverlay(graphics, shader, font);
            matrixStack.pop();
        }
    }

    public void renderComponent(GraphicsSystem graphics, Shader shader,
            FontRenderer font, double delta) {
    }

    public void renderOverlay(GraphicsSystem graphics, Shader shader,
            FontRenderer font) {
    }

    public void setHover(boolean hover, ScapesEngine engine) {
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public void update(ScapesEngine engine) {
        GuiController guiController = engine.getGuiController();
        update(guiController.getGuiCursorX(), guiController.getGuiCursorY(),
                true, engine);
    }

    public void update(double mouseX, double mouseY, boolean mouseInside,
            ScapesEngine engine) {
        if (visible) {
            updateComponent();
            boolean inside = mouseInside && checkInside(mouseX, mouseY);
            if (inside) {
                GuiController guiController = engine.getGuiController();
                if (guiController.getLeftClick()) {
                    clickLeft(new GuiComponentEvent(mouseX, mouseY), engine);
                }
                if (guiController.getRightClick()) {
                    clickRight(new GuiComponentEvent(mouseX, mouseY), engine);
                }
                setHover(true, engine);
                hover(new GuiComponentEvent(mouseX, mouseY));
            } else {
                resetHover(engine);
            }
            while (!changeComponents.isEmpty()) {
                Pair<Boolean, GuiComponent> component = changeComponents.poll();
                if (component.a) {
                    components.add(component.b);
                    component.b.parent = this;
                } else {
                    components.remove(component.b);
                    component.b.removed();
                    component.b.parent = null;
                }
            }
            double mouseXX = mouseX - x;
            double mouseYY = mouseY - y;
            components.forEach(
                    component -> updateChild(component, mouseXX, mouseYY,
                            inside, engine));
        }
    }

    protected void resetHover(ScapesEngine engine) {
        setHover(false, engine);
        for (GuiComponent component : components) {
            component.resetHover(engine);
        }
    }

    public void updateComponent() {
    }

    protected void updateChild(GuiComponent component, double mouseX,
            double mouseY, boolean inside, ScapesEngine engine) {
        component.update(mouseX, mouseY, inside, engine);
    }

    protected void transform(Matrix matrix) {
        matrix.translate(x, y, 0);
    }

    @Override
    public int hashCode() {
        return (int) uid;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof GuiComponent && uid == ((GuiComponent) obj).uid;
    }

    @Override
    public int compareTo(GuiComponent o) {
        long id = uid - o.uid;
        if (id > 0) {
            return 1;
        } else if (id < 0) {
            return -1;
        }
        return 0;
    }
}
