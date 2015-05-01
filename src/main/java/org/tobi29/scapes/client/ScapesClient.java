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

package org.tobi29.scapes.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.Scapes;
import org.tobi29.scapes.client.input.InputMode;
import org.tobi29.scapes.client.input.gamepad.InputModeGamepad;
import org.tobi29.scapes.client.input.keyboard.InputModeKeyboard;
import org.tobi29.scapes.client.input.spi.InputModeProvider;
import org.tobi29.scapes.engine.Game;
import org.tobi29.scapes.engine.ScapesEngine;
import org.tobi29.scapes.engine.gui.GuiAlignment;
import org.tobi29.scapes.engine.gui.GuiComponentIcon;
import org.tobi29.scapes.engine.gui.GuiComponentText;
import org.tobi29.scapes.engine.gui.GuiMessage;
import org.tobi29.scapes.engine.input.Controller;
import org.tobi29.scapes.engine.input.ControllerDefault;
import org.tobi29.scapes.engine.input.ControllerJoystick;
import org.tobi29.scapes.engine.input.InputException;
import org.tobi29.scapes.engine.utils.VersionUtil;
import org.tobi29.scapes.engine.utils.io.filesystem.FileSystemContainer;
import org.tobi29.scapes.engine.utils.io.filesystem.classpath.ClasspathPathRoot;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;

import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

public class ScapesClient extends Game {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ScapesClient.class);
    private final List<InputMode> inputModes = new ArrayList<>();
    private InputMode inputMode;
    private boolean freezeInputMode;

    private static Optional<InputMode> loadService(ScapesEngine engine,
            Controller controller, TagStructure tagStructure) {
        for (InputModeProvider provider : ServiceLoader
                .load(InputModeProvider.class)) {
            try {
                InputMode inputMode =
                        provider.get(engine, controller, tagStructure);
                if (inputMode != null) {
                    LOGGER.debug("Loaded input mode: {}",
                            provider.getClass().getName());
                    return Optional.of(inputMode);
                }
            } catch (ServiceConfigurationError e) {
                LOGGER.warn("Unable to load input mode provider: {}",
                        e.toString());
            }
        }
        if (controller instanceof ControllerDefault) {
            return Optional.of(new InputModeKeyboard(engine,
                    (ControllerDefault) controller, tagStructure));
        } else if (controller instanceof ControllerJoystick) {
            return Optional.of(new InputModeGamepad(engine,
                    (ControllerJoystick) controller, tagStructure));
        }
        return Optional.empty();
    }

    public InputMode getInputMode() {
        return inputMode;
    }

    @Override
    public String getName() {
        return "Scapes";
    }

    @Override
    public String getID() {
        return "Scapes";
    }

    @Override
    public VersionUtil.Version getVersion() {
        return Scapes.VERSION;
    }

    @Override
    public void init() {
        try {
            FileSystemContainer files = engine.getFiles();
            files.registerFileSystem("Scapes", "assets/scapes/tobi29/",
                    ClasspathPathRoot.make(getClass().getClassLoader()));
            files.getDirectory("File:playlists/day").make();
            files.getDirectory("File:playlists/night").make();
            files.getDirectory("File:playlists/battle").make();
            files.getDirectory("File:plugins").make();
            files.getDirectory("File:screenshots").make();
            files.getDirectory("File:saves").make();
        } catch (IOException e) {
            engine.crash(e);
        }
        TagStructure tagStructure = engine.getTagStructure();
        if (!tagStructure.has("Scapes")) {
            TagStructure scapesTag = tagStructure.getStructure("Scapes");
            scapesTag.setFloat("AnimationDistance", 0.15f);
            scapesTag.setBoolean("FXAA", true);
            scapesTag.setBoolean("Bloom", true);
            scapesTag.setDouble("RenderDistance", 128.0);
            scapesTag.setBoolean("KeepInvisibleChunkVbos", true);
            TagStructure integratedServerTag =
                    scapesTag.getStructure("IntegratedServer");
            TagStructure serverTag = integratedServerTag.getStructure("Server");
            serverTag.setInteger("MaxLoadingRadius", 256);
            TagStructure socketTag = serverTag.getStructure("Socket");
            socketTag.setInteger("MaxPlayers", 5);
            socketTag.setInteger("WorkerCount", 1);
            socketTag.setInteger("RSASize", 1024);
        }
        reloadInput();
    }

    @Override
    public void step() {
        if (engine.getGraphics().getContainer().joysticksChanged()) {
            reloadInput();
        }
        InputMode newInputMode = null;
        for (InputMode inputMode : inputModes) {
            if (inputMode.poll()) {
                newInputMode = inputMode;
            }
        }
        if (newInputMode != null && inputMode != newInputMode &&
                !freezeInputMode) {
            LOGGER.info("Setting input mode to {}", newInputMode);
            inputMode = newInputMode;
            engine.setGuiController(inputMode.getGuiController());
            GuiMessage message =
                    new GuiMessage(500, 0, 290, 60, GuiAlignment.RIGHT, 3.0);
            message.add(new GuiComponentIcon(10, 10, 40, 40,
                    engine.getGraphics().getTextureManager()
                            .getTexture("Scapes:image/gui/input/Default")));
            message.add(new GuiComponentText(60, 25, 420, 10,
                    inputMode.toString()));
            engine.getGlobalGui().add(message);
        }
    }

    @Override
    public void dispose() {
    }

    public void reloadInput() {
        LOGGER.info("Loading input");
        TagStructure tagStructure =
                engine.getTagStructure().getStructure("Scapes")
                        .getStructure("Input");
        Optional<InputMode> inputModeDefault =
                loadService(engine, engine.getController(), tagStructure);
        if (!inputModeDefault.isPresent()) {
            throw new InputException("No keyboard controller installed");
        }
        inputModes.clear();
        inputModeDefault.ifPresent(inputModes::add);
        for (ControllerJoystick joystick : engine.getGraphics().getContainer()
                .getJoysticks()) {
            loadService(engine, joystick, tagStructure)
                    .ifPresent(inputModes::add);
        }
        inputMode = inputModes.get(0);
        engine.setGuiController(inputMode.getGuiController());
    }

    public void setFreezeInputMode(boolean freezeInputMode) {
        this.freezeInputMode = freezeInputMode;
    }

    public Stream<InputMode> getInputModes() {
        return inputModes.stream();
    }
}
