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

package org.tobi29.scapes.engine.backends.lwjgl3;

import org.lwjgl.LWJGLUtil;
import org.lwjgl.Sys;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.engine.ScapesEngine;
import org.tobi29.scapes.engine.input.ControllerDefault;
import org.tobi29.scapes.engine.input.ControllerJoystick;
import org.tobi29.scapes.engine.input.ControllerKey;
import org.tobi29.scapes.engine.openal.OpenAL;
import org.tobi29.scapes.engine.opengl.Container;
import org.tobi29.scapes.engine.opengl.GraphicsCheckException;
import org.tobi29.scapes.engine.opengl.OpenGL;
import org.tobi29.scapes.engine.utils.DesktopException;
import org.tobi29.scapes.engine.utils.MutableSingle;
import org.tobi29.scapes.engine.utils.task.Joiner;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

public abstract class ContainerLWJGL3 extends ControllerDefault
        implements Container {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ContainerLWJGL3.class);
    protected final Map<Integer, ControllerJoystick> virtualJoysticks =
            new ConcurrentHashMap<>();
    protected final Queue<Runnable> tasks = new ConcurrentLinkedQueue<>();
    protected final ScapesEngine engine;
    protected final Thread mainThread;
    protected final OpenGL openGL;
    protected final OpenAL openAL;
    protected final boolean superModifier;
    protected GLContext context;
    protected boolean focus = true, valid, visible, containerResized = true;
    protected int containerWidth, containerHeight, contentWidth, contentHeight;
    protected double mouseX, mouseY;
    protected volatile boolean joysticksChanged;

    protected ContainerLWJGL3(ScapesEngine engine) {
        this.engine = engine;
        mainThread = Thread.currentThread();
        String natives = LWJGLNatives.extract(engine.getFiles());
        System.setProperty("org.lwjgl.librarypath", natives);
        LOGGER.info("LWJGL version: {}", Sys.getVersion());
        openGL = new LWJGL3OpenGL(engine);
        openAL = new LWJGL3OpenAL();
        superModifier = LWJGLUtil.getPlatform() == LWJGLUtil.Platform.MACOSX;
    }

    private static Optional<String> checkContext(GLContext context) {
        LOGGER.info("OpenGL: {} (Vendor: {}, Renderer: {})",
                GL11.glGetString(GL11.GL_VERSION),
                GL11.glGetString(GL11.GL_VENDOR),
                GL11.glGetString(GL11.GL_RENDERER));
        if (!context.getCapabilities().OpenGL11) {
            return Optional.of("Your graphics card has no OpenGL 1.1 support!");
        }
        if (!context.getCapabilities().OpenGL12) {
            return Optional.of("Your graphics card has no OpenGL 1.2 support!");
        }
        if (!context.getCapabilities().OpenGL13) {
            return Optional.of("Your graphics card has no OpenGL 1.3 support!");
        }
        if (!context.getCapabilities().OpenGL14) {
            return Optional.of("Your graphics card has no OpenGL 1.4 support!");
        }
        if (!context.getCapabilities().OpenGL15) {
            return Optional.of("Your graphics card has no OpenGL 1.5 support!");
        }
        if (!context.getCapabilities().OpenGL20) {
            return Optional.of("Your graphics card has no OpenGL 2.0 support!");
        }
        if (!context.getCapabilities().OpenGL21) {
            return Optional.of("Your graphics card has no OpenGL 2.1 support!");
        }
        if (!context.getCapabilities().OpenGL30) {
            return Optional.of("Your graphics card has no OpenGL 3.0 support!");
        }
        if (!context.getCapabilities().OpenGL31) {
            return Optional.of("Your graphics card has no OpenGL 3.1 support!");
        }
        if (!context.getCapabilities().OpenGL32) {
            return Optional.of("Your graphics card has no OpenGL 3.2 support!");
        }
        return Optional.empty();
    }

    @Override
    public void setVSync(boolean value) {
        valid = false;
    }

    @Override
    public void setFullscreen(boolean value) {
        valid = false;
    }

    @Override
    public int getContainerWidth() {
        return containerWidth;
    }

    @Override
    public int getContainerHeight() {
        return containerHeight;
    }

    @Override
    public int getContentWidth() {
        return contentWidth;
    }

    @Override
    public int getContentHeight() {
        return contentHeight;
    }

    @Override
    public boolean getContainerResized() {
        return containerResized;
    }

    @Override
    public void renderTick() throws DesktopException {
        while (!tasks.isEmpty()) {
            tasks.poll().run();
        }
        if (!valid) {
            if (context != null) {
                engine.getGraphics().reset();
                cleanWindow();
            }
            initWindow(engine.getConfig().isFullscreen(),
                    engine.getConfig().getVSync());
            context = GLContext.createFromCurrent();
            Optional<String> check = checkContext(context);
            if (check.isPresent()) {
                throw new GraphicsCheckException(check.get());
            }
            valid = true;
            containerResized = true;
        }
        render();
        containerResized = false;
        if (!visible) {
            showWindow();
        }
    }

    @Override
    public OpenGL getOpenGL() {
        return openGL;
    }

    @Override
    public OpenAL getOpenAL() {
        return openAL;
    }

    @Override
    public ControllerDefault getController() {
        return this;
    }

    @Override
    public Collection<ControllerJoystick> getJoysticks() {
        joysticksChanged = false;
        Collection<ControllerJoystick> collection =
                new ArrayList<>(virtualJoysticks.size());
        collection.addAll(virtualJoysticks.values());
        return collection;
    }

    @Override
    public boolean joysticksChanged() {
        return joysticksChanged;
    }

    protected abstract void render();

    protected abstract void initWindow(boolean fullscreen, boolean vSync);

    protected abstract void showWindow();

    protected abstract void cleanWindow();

    @Override
    public boolean isModifierDown() {
        if (superModifier) {
            return isDown(ControllerKey.KEY_LEFT_SUPER) ||
                    isDown(ControllerKey.KEY_RIGHT_SUPER);
        } else {
            return isDown(ControllerKey.KEY_LEFT_CONTROL) ||
                    isDown(ControllerKey.KEY_RIGHT_CONTROL);
        }
    }

    protected void exec(Runnable runnable) {
        Thread thread = Thread.currentThread();
        if (thread == mainThread) {
            runnable.run();
            return;
        }
        Joiner.Joinable joinable = new Joiner.Joinable();
        tasks.add(() -> {
            runnable.run();
            joinable.join();
        });
        joinable.getJoiner().join();
    }

    protected <R> R exec(Supplier<R> runnable) {
        Thread thread = Thread.currentThread();
        if (thread == mainThread) {
            return runnable.get();
        }
        Joiner.Joinable joinable = new Joiner.Joinable();
        MutableSingle<R> output = new MutableSingle<>(null);
        tasks.add(() -> {
            output.a = runnable.get();
            joinable.join();
        });
        joinable.getJoiner().join();
        return output.a;
    }

    protected <R> R execIO(IOSupplier<R> runnable) throws IOException {
        Thread thread = Thread.currentThread();
        if (thread == mainThread) {
            return runnable.get();
        }
        Joiner.Joinable joinable = new Joiner.Joinable();
        MutableSingle<R> output = new MutableSingle<>(null);
        MutableSingle<IOException> exception = new MutableSingle<>(null);
        tasks.add(() -> {
            try {
                output.a = runnable.get();
            } catch (IOException e) {
                exception.a = e;
            }
            joinable.join();
        });
        joinable.getJoiner().join();
        if (exception.a != null) {
            throw new IOException(exception.a);
        }
        return output.a;
    }

    @FunctionalInterface
    protected interface IOSupplier<R> {
        R get() throws IOException;
    }
}
