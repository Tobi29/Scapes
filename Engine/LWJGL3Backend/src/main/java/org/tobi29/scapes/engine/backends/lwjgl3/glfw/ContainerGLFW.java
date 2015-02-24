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

package org.tobi29.scapes.engine.backends.lwjgl3.glfw;

import org.lwjgl.glfw.*;
import org.lwjgl.opengl.GL11;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.engine.ScapesEngine;
import org.tobi29.scapes.engine.backends.lwjgl3.ContainerLWJGL3;
import org.tobi29.scapes.engine.input.ControllerJoystick;
import org.tobi29.scapes.engine.input.ControllerKey;
import org.tobi29.scapes.engine.opengl.GraphicsException;
import org.tobi29.scapes.engine.utils.BufferCreatorDirect;
import org.tobi29.scapes.engine.utils.MutableSingle;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.platform.Platform;
import org.tobi29.scapes.engine.utils.platform.PlatformDialogs;
import org.tobi29.scapes.engine.utils.task.Joiner;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class ContainerGLFW extends ContainerLWJGL3 {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ContainerGLFW.class);
    private static final double DEADZONES = 0.05, DEADZONES_SCALE = 0.95;
    @SuppressWarnings("FieldCanBeLocal")
    private final GLFWErrorCallback errorFun;
    private final GLFWWindowSizeCallback windowSizeFun;
    private final GLFWWindowCloseCallback windowCloseFun;
    private final GLFWWindowFocusCallback windowFocusFun;
    private final GLFWFramebufferSizeCallback frameBufferSizeFun;
    private final GLFWKeyCallback keyFun;
    private final GLFWCharCallback charFun;
    private final GLFWMouseButtonCallback mouseButtonFun;
    private final GLFWCursorPosCallback cursorPosFun;
    private final GLFWScrollCallback scrollFun;
    private final PlatformDialogs dialogs;
    private final Map<Integer, boolean[]> virtualJoystickStates =
            new ConcurrentHashMap<>();
    private long window;
    private boolean skipMouseCallback, recreateContext, visible;

    public ContainerGLFW(ScapesEngine engine) {
        super(engine);
        errorFun = Callbacks.errorCallbackPrint();
        GLFW.glfwSetErrorCallback(errorFun);
        if (GLFW.glfwInit() != GL11.GL_TRUE) {
            throw new GraphicsException("Unable to initialize GLFW");
        }
        LOGGER.info("GLFW version: {}", GLFW.glfwGetVersionString());
        dialogs = Platform.getPlatform().createDialogHandler();
        windowSizeFun = GLFW.GLFWWindowSizeCallback((window, width, height) -> {
            containerWidth = width;
            containerHeight = height;
            containerResized = true;
        });
        windowCloseFun = GLFW.GLFWWindowCloseCallback(window -> engine.stop());
        windowFocusFun = GLFW.GLFWWindowFocusCallback(
                (window, focused) -> focus = focused == GL11.GL_TRUE);
        frameBufferSizeFun =
                GLFW.GLFWFramebufferSizeCallback((window, width, height) -> {
                    contentWidth = width;
                    contentHeight = height;
                    containerResized = true;
                });
        keyFun = GLFW.GLFWKeyCallback((window, key, scancode, action, mods) -> {
            ControllerKey virtualKey = GLFWKeyMap.getKey(key);
            if (virtualKey != null) {
                if (virtualKey == ControllerKey.KEY_BACKSPACE &&
                        action != GLFW.GLFW_RELEASE) {
                    addTypeEvent((char) 127);
                }
                switch (action) {
                    case GLFW.GLFW_PRESS:
                        addPressEvent(virtualKey, PressState.PRESS);
                        break;
                    case GLFW.GLFW_REPEAT:
                        addPressEvent(virtualKey, PressState.REPEAT);
                        break;
                    case GLFW.GLFW_RELEASE:
                        addPressEvent(virtualKey, PressState.RELEASE);
                        break;
                }
            }
        });
        charFun = GLFW.GLFWCharCallback(
                (window, codepoint) -> addTypeEvent((char) codepoint));
        mouseButtonFun =
                GLFW.GLFWMouseButtonCallback((window, button, action, mods) -> {
                    ControllerKey virtualKey = ControllerKey.getButton(button);
                    if (virtualKey != ControllerKey.UNKNOWN) {
                        switch (action) {
                            case GLFW.GLFW_PRESS:
                                addPressEvent(virtualKey, PressState.PRESS);
                                break;
                            case GLFW.GLFW_RELEASE:
                                addPressEvent(virtualKey, PressState.RELEASE);
                                break;
                        }
                    }
                });
        cursorPosFun = GLFW.GLFWCursorPosCallback((window, xpos, ypos) -> {
            if (skipMouseCallback) {
                skipMouseCallback = false;
                if (mouseGrabbed) {
                    GLFW.glfwSetCursorPos(window, 0.0d, 0.0d);
                }
            } else {
                double dx, dy;
                if (mouseGrabbed) {
                    dx = xpos;
                    dy = ypos;
                    GLFW.glfwSetCursorPos(window, 0.0d, 0.0d);
                } else {
                    dx = xpos - mouseX;
                    dy = ypos - mouseY;
                    mouseX = (int) xpos;
                    mouseY = (int) ypos;
                }
                if (dx != 0.0d || dy != 0.0d) {
                    set(xpos, ypos);
                    addDelta(dx, dy);
                }
            }
        });
        scrollFun = GLFW.GLFWScrollCallback((window, xoffset, yoffset) -> {
            if (xoffset != 0.0d || yoffset != 0.0d) {
                addScroll(xoffset, yoffset);
            }
        });
    }

    private static double deadzones(double value) {
        if (value > DEADZONES) {
            return (value - DEADZONES) / DEADZONES_SCALE;
        } else if (value < -DEADZONES) {
            return (value + DEADZONES) / DEADZONES_SCALE;
        }
        return 0.0f;
    }

    @Override
    public void setMouseGrabbed(boolean value) {
        if (mouseGrabbed != value) {
            if (value) {
                mouseGrabbed = true;
                GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR,
                        GLFW.GLFW_CURSOR_DISABLED);
                GLFW.glfwSetCursorPos(window, 0.0d, 0.0d);
                mouseX = 0.0d;
                mouseY = 0.0d;
                skipMouseCallback = true;
            } else {
                mouseGrabbed = false;
                mouseX = containerWidth * 0.5;
                mouseY = containerHeight * 0.5;
                GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR,
                        GLFW.GLFW_CURSOR_NORMAL);
                GLFW.glfwSetCursorPos(window, mouseX, mouseY);
            }
        }
    }

    @Override
    protected void updateMouseGrabbed(boolean value) {
        if (value) {
            mouseGrabbed = true;
            GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR,
                    GLFW.GLFW_CURSOR_DISABLED);
            GLFW.glfwSetCursorPos(window, 0.0d, 0.0d);
            mouseX = 0.0d;
            mouseY = 0.0d;
            skipMouseCallback = true;
        } else {
            mouseGrabbed = false;
            mouseX = containerWidth * 0.5;
            mouseY = containerHeight * 0.5;
            GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR,
                    GLFW.GLFW_CURSOR_NORMAL);
            GLFW.glfwSetCursorPos(window, mouseX, mouseY);
        }
    }

    @Override
    public void updateVSync(boolean value) {
        GLFW.glfwSwapInterval(value ? 1 : 0);
    }

    @Override
    protected void updateFullscreen(boolean value) {
        recreateContext = true;
    }

    @SuppressWarnings("ForLoopThatDoesntUseLoopVariable")
    @Override
    public void render(boolean active) {
        active |= dialogs.renderTick(active);
        if (active) {
            for (int joystick = GLFW.GLFW_JOYSTICK_1;
                    joystick <= GLFW.GLFW_JOYSTICK_LAST; joystick++) {
                if (GLFW.glfwJoystickPresent(joystick) == GL11.GL_TRUE) {
                    String name = GLFW.glfwGetJoystickName(joystick);
                    FloatBuffer axes = GLFW.glfwGetJoystickAxes(joystick);
                    ByteBuffer buttons = GLFW.glfwGetJoystickButtons(joystick);
                    ControllerJoystick virtualJoystick =
                            virtualJoysticks.get(joystick);
                    boolean[] states = virtualJoystickStates.get(joystick);
                    if (virtualJoystick == null) {
                        assert states == null;
                        virtualJoystick =
                                new ControllerJoystick(name, axes.capacity());
                        states = new boolean[buttons.remaining()];
                        virtualJoysticks.put(joystick, virtualJoystick);
                        virtualJoystickStates.put(joystick, states);
                        joysticksChanged = true;
                    } else {
                        assert states != null;
                        if (!name.equals(virtualJoystick.getName()) ||
                                buttons.remaining() != states.length ||
                                axes.remaining() != virtualJoystick.getAxes()) {
                            virtualJoystick = new ControllerJoystick(name,
                                    axes.capacity());
                            states = new boolean[buttons.remaining()];
                            virtualJoysticks.put(joystick, virtualJoystick);
                            virtualJoystickStates.put(joystick, states);
                            joysticksChanged = true;
                        }
                    }
                    for (int i = 0; axes.hasRemaining(); i++) {
                        virtualJoystick.setAxis(i, deadzones(axes.get()));
                    }
                    for (int i = 0; buttons.hasRemaining(); i++) {
                        boolean value = buttons.get() == 1;
                        if (states[i] != value) {
                            states[i] = value;
                            ControllerKey button = ControllerKey.getButton(i);
                            if (button != ControllerKey.UNKNOWN) {
                                virtualJoystick.addPressEvent(button,
                                        value ? PressState.PRESS :
                                                PressState.RELEASE);
                            }
                        }
                    }
                } else if (virtualJoysticks.containsKey(joystick)) {
                    virtualJoysticks.remove(joystick);
                    virtualJoystickStates.remove(joystick);
                    joysticksChanged = true;
                }
            }
        }
        GLFW.glfwPollEvents();
        if (active) {
            engine.getGraphics().render();
            containerResized = false;
            GLFW.glfwSwapBuffers(window);
        }
        if (!visible) {
            visible = true;
            GLFW.glfwShowWindow(window);
        }
        if (recreateContext) {
            recreateContext = false;
            engine.getGraphics().reset();
            clearStates();
            GLFW.glfwDestroyWindow(window);
            init();
            containerResized = true;
            visible = false;
        }
    }

    @Override
    protected void createWindow(boolean fullscreen, String title) {
        LOGGER.info("Creating GLFW window...");
        long monitor = GLFW.glfwGetPrimaryMonitor();
        ByteBuffer videoMode = GLFW.glfwGetVideoMode(monitor);
        int monitorWidth = GLFWvidmode.width(videoMode);
        int monitorHeight = GLFWvidmode.height(videoMode);
        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GL11.GL_FALSE);
        TagStructure tagStructure = engine.getTagStructure();
        if (!tagStructure.has("Compatibility") ||
                !engine.getTagStructure().getStructure("Compatibility")
                        .getBoolean("ForceLegacyGL")) {
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 2);
            GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE,
                    GLFW.GLFW_OPENGL_CORE_PROFILE);
            GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GL11.GL_TRUE);
        }
        if (fullscreen) {
            window = GLFW.glfwCreateWindow(monitorWidth, monitorHeight, title,
                    monitor, 0L);
        } else {
            int width, height;
            if (monitorWidth > 1280 && monitorHeight > 720) {
                width = 1280;
                height = 720;
            } else {
                width = 960;
                height = 540;
            }
            window = GLFW.glfwCreateWindow(width, height, title, 0L, 0L);
            GLFW.glfwSetWindowPos(window, (monitorWidth - width) / 2,
                    (monitorHeight - height) / 2);
        }
        IntBuffer widthBuffer = BufferCreatorDirect.intBuffer(1);
        IntBuffer heightBuffer = BufferCreatorDirect.intBuffer(1);
        GLFW.glfwGetWindowSize(window, widthBuffer, heightBuffer);
        containerWidth = widthBuffer.get(0);
        containerHeight = heightBuffer.get(0);
        GLFW.glfwGetFramebufferSize(window, widthBuffer, heightBuffer);
        contentWidth = widthBuffer.get(0);
        contentHeight = heightBuffer.get(0);
        GLFW.glfwSetWindowSizeCallback(window, windowSizeFun);
        GLFW.glfwSetWindowCloseCallback(window, windowCloseFun);
        GLFW.glfwSetWindowFocusCallback(window, windowFocusFun);
        GLFW.glfwSetFramebufferSizeCallback(window, frameBufferSizeFun);
        GLFW.glfwSetKeyCallback(window, keyFun);
        GLFW.glfwSetCharCallback(window, charFun);
        GLFW.glfwSetMouseButtonCallback(window, mouseButtonFun);
        GLFW.glfwSetCursorPosCallback(window, cursorPosFun);
        GLFW.glfwSetScrollCallback(window, scrollFun);
        GLFW.glfwMakeContextCurrent(window);
    }

    @Override
    public void dispose() {
        dialogs.dispose();
        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();
        windowSizeFun.release();
        windowCloseFun.release();
        windowFocusFun.release();
        frameBufferSizeFun.release();
        keyFun.release();
        charFun.release();
        mouseButtonFun.release();
        cursorPosFun.release();
        scrollFun.release();
    }

    @Override
    public File[] openFileDialog(PlatformDialogs.Extension[] extensions,
            String title, boolean multiple) {
        Thread thread = Thread.currentThread();
        if (thread == mainThread) {
            return dialogs.openFileDialog(extensions, title, multiple);
        }
        Joiner.Joinable joinable = new Joiner.Joinable();
        MutableSingle<File[]> file = new MutableSingle<>(null);
        tasks.add(() -> {
            file.a = dialogs.openFileDialog(extensions, title, multiple);
            joinable.join();
        });
        joinable.getJoiner().join();
        return file.a;
    }

    @Override
    public Optional<File> saveFileDialog(PlatformDialogs.Extension[] extensions,
            String title) {
        Thread thread = Thread.currentThread();
        if (thread == mainThread) {
            return dialogs.saveFileDialog(extensions, title);
        }
        Joiner.Joinable joinable = new Joiner.Joinable();
        MutableSingle<Optional<File>> file = new MutableSingle<>(null);
        tasks.add(() -> {
            file.a = dialogs.saveFileDialog(extensions, title);
            joinable.join();
        });
        joinable.getJoiner().join();
        return file.a;
    }

    @Override
    public void message(PlatformDialogs.MessageType messageType, String title,
            String message) {
        Thread thread = Thread.currentThread();
        if (thread == mainThread) {
            dialogs.message(messageType, title, message);
            return;
        }
        Joiner.Joinable joinable = new Joiner.Joinable();
        tasks.add(() -> {
            dialogs.message(messageType, title, message);
            joinable.join();
        });
        joinable.getJoiner().join();
    }

    @Override
    public void openFile(File file) {
        Thread thread = Thread.currentThread();
        if (thread == mainThread) {
            dialogs.openFile(file);
            return;
        }
        Joiner.Joinable joinable = new Joiner.Joinable();
        tasks.add(() -> {
            dialogs.openFile(file);
            joinable.join();
        });
        joinable.getJoiner().join();
    }

    @Override
    public void clipboardCopy(String value) {
        GLFW.glfwSetClipboardString(window, value);
    }

    @Override
    public String clipboardPaste() {
        return GLFW.glfwGetClipboardString(window);
    }
}
