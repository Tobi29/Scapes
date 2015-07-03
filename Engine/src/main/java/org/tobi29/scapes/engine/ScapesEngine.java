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

package org.tobi29.scapes.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.engine.gui.Gui;
import org.tobi29.scapes.engine.gui.GuiAlignment;
import org.tobi29.scapes.engine.gui.GuiController;
import org.tobi29.scapes.engine.gui.GuiControllerDefault;
import org.tobi29.scapes.engine.gui.debug.GuiDebugLayer;
import org.tobi29.scapes.engine.gui.debug.GuiWidgetDebugValues;
import org.tobi29.scapes.engine.input.ControllerDefault;
import org.tobi29.scapes.engine.input.ControllerKey;
import org.tobi29.scapes.engine.openal.SoundSystem;
import org.tobi29.scapes.engine.opengl.Container;
import org.tobi29.scapes.engine.opengl.GraphicsCheckException;
import org.tobi29.scapes.engine.opengl.GraphicsSystem;
import org.tobi29.scapes.engine.spi.ScapesEngineBackendProvider;
import org.tobi29.scapes.engine.utils.Crashable;
import org.tobi29.scapes.engine.utils.Sync;
import org.tobi29.scapes.engine.utils.io.CrashReportFile;
import org.tobi29.scapes.engine.utils.io.FileCache;
import org.tobi29.scapes.engine.utils.io.FileUtil;
import org.tobi29.scapes.engine.utils.io.filesystem.FileSystemContainer;
import org.tobi29.scapes.engine.utils.io.filesystem.classpath.ClasspathPath;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.io.tag.TagStructureJSON;
import org.tobi29.scapes.engine.utils.task.Joiner;
import org.tobi29.scapes.engine.utils.task.TaskExecutor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

public class ScapesEngine implements Crashable {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ScapesEngine.class);
    private static ScapesEngine instance;
    private final GraphicsSystem graphics;
    private final TaskExecutor taskExecutor;
    private final Runtime runtime;
    private final Game game;
    private final TagStructure tagStructure;
    private final ScapesEngineConfig config;
    private final Path home, temp;
    private final FileCache fileCache;
    private final GuiWidgetDebugValues.Element usedMemoryDebug, maxMemoryDebug;
    private final boolean debug;
    private final Gui globalGui;
    private final GuiDebugLayer debugGui;
    private final FileSystemContainer files;
    private final SoundSystem sounds;
    private final ControllerDefault controller;
    private GuiController guiController;
    private boolean mouseGrabbed;
    private GameState currentState, newState;
    private StateThread stateThread;

    public ScapesEngine(Game game, Path home, boolean debug) {
        this(game, loadBackend(), home, debug);
    }

    public ScapesEngine(Game game, ScapesEngineBackendProvider backend,
            Path home, boolean debug) {
        if (instance != null) {
            throw new ScapesEngineException(
                    "You can only have one engine running at a time!");
        }
        instance = this;
        this.debug = debug;
        this.game = game;
        this.home = home;
        runtime = Runtime.getRuntime();
        game.engine = this;
        Thread.currentThread().setName("Engine-Rendering-Thread");
        LOGGER.info("Starting Scapes-Engine: {} (Game: {})", this, game);
        try {
            files = new FileSystemContainer();
            temp = Files.createTempDirectory("ScapesEngine");
            runtime.addShutdownHook(new Thread(() -> {
                try {
                    FileUtil.deleteDir(temp);
                } catch (IOException e) {
                    LOGGER.warn("Failed to delete temporary directory: {}",
                            e.toString());
                }
            }));
            files.registerFileSystem("Class",
                    new ClasspathPath(getClass().getClassLoader(), ""));
            files.registerFileSystem("Engine",
                    new ClasspathPath(getClass().getClassLoader(),
                            "assets/scapes/tobi29/engine/"));
            fileCache = new FileCache(this.home.resolve("cache"),
                    temp.resolve("cache"));
            fileCache.check();
            Files.createDirectories(this.home.resolve("screenshots"));
        } catch (IOException e) {
            throw new ScapesEngineException(
                    "Failed to create virtual file system: " + e.toString());
        }
        checkSystem();
        taskExecutor = new TaskExecutor(this, "Engine");
        tagStructure = new TagStructure();
        try {
            Path configPath = this.home.resolve("ScapesEngine.json");
            if (Files.exists(configPath)) {
                FileUtil.read(configPath,
                        stream -> TagStructureJSON.read(tagStructure, stream));
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to load config file: {}", e.toString());
        }
        if (tagStructure.has("Engine")) {
            config =
                    new ScapesEngineConfig(tagStructure.getStructure("Engine"));
        } else {
            TagStructure engineTag = tagStructure.getStructure("Engine");
            engineTag.setBoolean("VSync", true);
            engineTag.setDouble("Framerate", 60.0);
            engineTag.setDouble("ResolutionMultiplier", 1.0);
            engineTag.setDouble("MusicVolume", 1.0);
            engineTag.setDouble("SoundVolume", 1.0);
            engineTag.setBoolean("Fullscreen", false);
            config = new ScapesEngineConfig(engineTag);
        }
        globalGui = new Gui(GuiAlignment.STRETCH);
        debugGui = new GuiDebugLayer();
        globalGui.add(debugGui);
        GuiWidgetDebugValues debugValues = debugGui.getDebugValues();
        usedMemoryDebug = debugValues.get("Runtime-Memory-Used");
        maxMemoryDebug = debugValues.get("Runtime-Memory-Max");
        game.init();
        graphics = new GraphicsSystem(this, backend.createContainer(this));
        sounds = new SoundSystem(this, graphics.getContainer().getOpenAL());
        controller = graphics.getContainer().getController();
        guiController = new GuiControllerDefault(this, controller);
    }

    private static ScapesEngineBackendProvider loadBackend() {
        for (ScapesEngineBackendProvider backend : ServiceLoader
                .load(ScapesEngineBackendProvider.class)) {
            try {
                if (backend.available()) {
                    LOGGER.debug("Loaded backend: {}",
                            backend.getClass().getName());
                    return backend;
                }
            } catch (ServiceConfigurationError e) {
                LOGGER.warn("Unable to load backend provider: {}",
                        e.toString());
            }
        }
        throw new ScapesEngineException("No backend found!");
    }

    private void checkSystem() {
        LOGGER.info("Operating system: {} {} {}", System.getProperty("os.name"),
                System.getProperty("os.version"),
                System.getProperty("os.arch"));
        LOGGER.info("Java: {} (MaxMemory: {}, Processors: {})",
                System.getProperty("java.version"),
                runtime.maxMemory() / 1048576, runtime.availableProcessors());
    }

    public GraphicsSystem getGraphics() {
        return graphics;
    }

    public SoundSystem getSounds() {
        return sounds;
    }

    public TaskExecutor getTaskExecutor() {
        return taskExecutor;
    }

    public GameState getNewState() {
        return newState;
    }

    public Game getGame() {
        return game;
    }

    public FileSystemContainer getFiles() {
        return files;
    }

    public Path getHome() {
        return home;
    }

    public Path getTemp() {
        return temp;
    }

    public FileCache getFileCache() {
        return fileCache;
    }

    public ScapesEngineConfig getConfig() {
        return config;
    }

    public TagStructure getTagStructure() {
        return tagStructure;
    }

    public Gui getGlobalGui() {
        return globalGui;
    }

    public GuiWidgetDebugValues getDebugValues() {
        return debugGui.getDebugValues();
    }

    public ControllerDefault getController() {
        return controller;
    }

    public GuiController getGuiController() {
        return guiController;
    }

    public void setGuiController(GuiController guiController) {
        this.guiController = guiController;
    }

    public GameState getState() {
        return currentState;
    }

    public void setState(GameState state) {
        newState = state;
    }

    @SuppressWarnings("OverlyBroadCatchBlock")
    public int run() {
        try {
            graphics.getContainer().run();
        } catch (GraphicsCheckException e) {
            LOGGER.error("Failed to initialize graphics:", e);
            graphics.getContainer()
                    .message(Container.MessageType.ERROR, game.getName(),
                            "Unable to initialize graphics:\n" +
                                    e.getMessage());
            return 1;
        } catch (Throwable e) {
            try {
                graphics.getContainer()
                        .message(Container.MessageType.ERROR, game.getName(),
                                game.getName() + " crashed\n:" + toString());
            } catch (Exception e2) {
                LOGGER.error("Failed to show crash message", e2);
            }
            crash(e);
            return 1;
        }
        return 0;
    }

    public void render() {
        graphics.step();
        sounds.poll(graphics.getSync().getSpeedFactor());
        graphics.render();
    }

    public void dispose() {
        if (stateThread != null) {
            stateThread.joiner.join();
            stateThread = null;
        }
        currentState.getScene().dispose(graphics);
        currentState.disposeState();
        game.dispose();
        sounds.dispose();
        graphics.dispose();
        try {
            FileUtil.write(home.resolve("ScapesEngine.json"),
                    streamOut -> TagStructureJSON
                            .write(tagStructure, streamOut));
        } catch (IOException e) {
            LOGGER.warn("Failed to save config file!");
        }
        taskExecutor.shutdown();
    }

    @Override
    @SuppressWarnings("CallToSystemExit")
    public void crash(Throwable e) {
        LOGGER.error("Scapes engine shutting down because of crash!", e);
        Map<String, String> debugValues = new ConcurrentHashMap<>();
        for (Map.Entry<String, GuiWidgetDebugValues.Element> entry : debugGui
                .getDebugValues().getElements()) {
            debugValues.put(entry.getKey(), entry.getValue().toString());
        }
        try {
            Path crashReportFile = CrashReportFile.getFile(home);
            CrashReportFile.writeCrashReport(e, crashReportFile, "ScapesEngine",
                    debugValues);
            graphics.getContainer().openFile(crashReportFile);
        } catch (IOException e1) {
            LOGGER.error("Failed to write crash report: {}", e.toString());
        }
        System.exit(1);
    }

    public void stop() {
        graphics.getContainer().stop();
    }

    public void step() {
        if (newState != null) {
            if (stateThread != null) {
                stateThread.joiner.join();
                stateThread = null;
            }
            if (currentState == null) {
                currentState = newState;
                game.initLate();
            } else {
                currentState.getScene().dispose(graphics);
                currentState.disposeState();
                currentState = newState;
            }
            newState = null;
            currentState.init();
        }
        if (currentState.isThreaded()) {
            if (stateThread == null) {
                stateThread = new StateThread(currentState);
                stateThread.joiner = taskExecutor.runTask(stateThread, "State",
                        TaskExecutor.Priority.MEDIUM);
            }
        } else if (stateThread != null) {
            stateThread.joiner.join();
            stateThread = null;
        }
        if (stateThread == null) {
            update(graphics.getSync(), currentState);
        }
    }

    private void update(Sync sync, GameState state) {
        taskExecutor.tick();
        boolean mouseGrabbed = currentState.isMouseGrabbed() ||
                guiController.isSoftwareMouse();
        if (this.mouseGrabbed != mouseGrabbed) {
            this.mouseGrabbed = mouseGrabbed;
            graphics.getContainer().setMouseGrabbed(mouseGrabbed);
        }
        controller.poll();
        usedMemoryDebug.setValue(
                (runtime.totalMemory() - runtime.freeMemory()) / 1048576);
        maxMemoryDebug.setValue(runtime.maxMemory() / 1048576);
        if (controller.isPressed(ControllerKey.KEY_F2)) {
            graphics.triggerScreenshot();
        }
        if (debug && controller.isPressed(ControllerKey.KEY_F3)) {
            debugGui.toggleDebugValues();
        }
        state.step(sync);
        globalGui.update(this);
        game.step();
        guiController.update(sync.getSpeedFactor());
    }

    private class StateThread implements TaskExecutor.ASyncTask {
        private final GameState state;
        private Joiner joiner;

        private StateThread(GameState state) {
            this.state = state;
        }

        @Override
        public void run(Joiner joiner) {
            try {
                Sync sync = new Sync(config.getFPS(), 5000000000L, true,
                        "Engine-Update");
                sync.init();
                while (!joiner.marked()) {
                    update(sync, state);
                    sync.capTPS();
                }
            } catch (Throwable e) {
                crash(e);
            }
        }
    }
}
