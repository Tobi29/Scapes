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

import java8.util.Optional;
import java8.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.Version;
import org.tobi29.scapes.client.input.InputMode;
import org.tobi29.scapes.client.input.gamepad.InputModeGamepad;
import org.tobi29.scapes.client.input.keyboard.InputModeKeyboard;
import org.tobi29.scapes.client.input.spi.InputModeProvider;
import org.tobi29.scapes.client.input.touch.InputModeTouch;
import org.tobi29.scapes.client.states.GameStateMenu;
import org.tobi29.scapes.engine.Container;
import org.tobi29.scapes.engine.Game;
import org.tobi29.scapes.engine.GameStateStartup;
import org.tobi29.scapes.engine.ScapesEngine;
import org.tobi29.scapes.engine.gui.GuiNotificationSimple;
import org.tobi29.scapes.engine.input.*;
import org.tobi29.scapes.engine.opengl.scenes.SceneEmpty;
import org.tobi29.scapes.engine.utils.Streams;
import org.tobi29.scapes.engine.utils.VersionUtil;
import org.tobi29.scapes.engine.utils.io.IOFunction;
import org.tobi29.scapes.engine.utils.io.filesystem.FilePath;
import org.tobi29.scapes.engine.utils.io.filesystem.FileSystemContainer;
import org.tobi29.scapes.engine.utils.io.filesystem.FileUtil;
import org.tobi29.scapes.engine.utils.io.filesystem.classpath.ClasspathPath;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

public class ScapesClient extends Game {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ScapesClient.class);
    private final IOFunction<ScapesClient, SaveStorage> savesSupplier;
    private final List<InputMode> inputModes = new ArrayList<>();
    private final boolean skipIntro;
    private SaveStorage saves;
    private InputMode inputMode;
    private boolean freezeInputMode;

    public ScapesClient(IOFunction<ScapesClient, SaveStorage> saves) {
        this(false, saves);
    }

    public ScapesClient(boolean skipIntro,
            IOFunction<ScapesClient, SaveStorage> saves) {
        this.skipIntro = skipIntro;
        savesSupplier = saves;
    }

    private static Optional<InputMode> loadService(ScapesEngine engine,
            Controller controller, TagStructure tagStructure) {
        for (InputModeProvider provider : ServiceLoader
                .load(InputModeProvider.class)) {
            try {
                Optional<InputMode> inputMode =
                        provider.get(engine, controller, tagStructure);
                if (inputMode.isPresent()) {
                    LOGGER.debug("Loaded input mode: {}",
                            provider.getClass().getName());
                    return inputMode;
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
        } else if (controller instanceof ControllerTouch) {
            return Optional.of(new InputModeTouch(engine,
                    (ControllerTouch) controller));
        }
        return Optional.empty();
    }

    public SaveStorage saves() {
        return saves;
    }

    public InputMode inputMode() {
        return inputMode;
    }

    @Override
    public String name() {
        return "Scapes";
    }

    @Override
    public String id() {
        return "Scapes";
    }

    @Override
    public VersionUtil.Version version() {
        return Version.VERSION;
    }

    @Override
    public void initEarly() {
        try {
            FilePath path = engine.home();
            FilePath playlistsPath = path.resolve("playlists");
            FileUtil.createDirectories(playlistsPath.resolve("day"));
            FileUtil.createDirectories(playlistsPath.resolve("night"));
            FileUtil.createDirectories(playlistsPath.resolve("battle"));
            FileUtil.createDirectories(path.resolve("plugins"));
            FileSystemContainer files = engine.files();
            files.registerFileSystem("Scapes",
                    new ClasspathPath(getClass().getClassLoader(),
                            "assets/scapes/tobi29/"));
            saves = savesSupplier.apply(this);
        } catch (IOException e) {
            engine.crash(e);
        }
        if (skipIntro) {
            engine.setState(new GameStateMenu(engine));
        } else {
            engine.setState(new GameStateStartup(new GameStateMenu(engine),
                    "Engine:image/Logo", 0.5, new SceneEmpty(), engine));
        }
    }

    @Override
    public void init() {
        TagStructure tagStructure = engine.tagStructure();
        if (!tagStructure.has("Scapes")) {
            boolean lightDefaults = engine.container().formFactor() ==
                    Container.FormFactor.PHONE;
            TagStructure scapesTag = tagStructure.getStructure("Scapes");
            if (lightDefaults) {
                scapesTag.setFloat("AnimationDistance", 0.0f);
                scapesTag.setBoolean("Bloom", false);
                scapesTag.setBoolean("AutoExposure", false);
                scapesTag.setBoolean("FXAA", false);
                scapesTag.setDouble("RenderDistance", 64.0);
            } else {
                scapesTag.setFloat("AnimationDistance", 0.15f);
                scapesTag.setBoolean("Bloom", true);
                scapesTag.setBoolean("AutoExposure", true);
                scapesTag.setBoolean("FXAA", true);
                scapesTag.setDouble("RenderDistance", 128.0);
            }
            TagStructure integratedServerTag =
                    scapesTag.getStructure("IntegratedServer");
            TagStructure serverTag = integratedServerTag.getStructure("Server");
            serverTag.setInteger("MaxLoadingRadius", 288);
            TagStructure socketTag = serverTag.getStructure("Socket");
            socketTag.setInteger("MaxPlayers", 5);
            socketTag.setInteger("WorkerCount", 1);
            socketTag.setInteger("RSASize", 1024);
        }
        loadInput();
    }

    @Override
    public void step() {
        if (engine.container().joysticksChanged()) {
            loadInput();
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
            engine.setGUIController(inputMode.guiController());
            engine.notifications().add(p -> new GuiNotificationSimple(p,
                    engine.graphics().textures()
                            .get("Scapes:image/gui/Playlist"),
                    inputMode.toString()));
        }
    }

    @Override
    public void dispose() {
    }

    public void loadInput() {
        LOGGER.info("Loading input");
        TagStructure tagStructure = engine.tagStructure().getStructure("Scapes")
                .getStructure("Input");
        inputModes.clear();
        Optional<ControllerDefault> controller =
                engine.container().controller();
        if (controller.isPresent()) {
            loadService(engine, controller.get(), tagStructure)
                    .ifPresent(inputModes::add);
        }
        Optional<ControllerTouch> touch = engine.container().touch();
        if (touch.isPresent()) {
            loadService(engine, touch.get(), tagStructure)
                    .ifPresent(inputModes::add);
        }
        for (ControllerJoystick joystick : engine.container().joysticks()) {
            loadService(engine, joystick, tagStructure)
                    .ifPresent(inputModes::add);
        }
        if (inputModes.isEmpty()) {
            throw new InputException("No input mode available");
        }
        inputMode = inputModes.get(0);
        engine.setGUIController(inputMode.guiController());
    }

    public void setFreezeInputMode(boolean freezeInputMode) {
        this.freezeInputMode = freezeInputMode;
    }

    public Stream<InputMode> inputModes() {
        return Streams.of(inputModes);
    }
}
