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

package org.tobi29.scapes;

import org.apache.commons.cli.*;
import org.tobi29.scapes.client.ScapesClient;
import org.tobi29.scapes.client.states.GameStateMenu;
import org.tobi29.scapes.engine.GameStateStartup;
import org.tobi29.scapes.engine.ScapesEngine;
import org.tobi29.scapes.engine.opengl.scenes.SceneImage;
import org.tobi29.scapes.engine.swt.SWTLoader;
import org.tobi29.scapes.engine.utils.VersionUtil;
import org.tobi29.scapes.engine.utils.platform.Platform;
import org.tobi29.scapes.plugins.PluginClassLoader;
import org.tobi29.scapes.server.shell.ScapesServerHeadless;
import org.tobi29.scapes.server.shell.ScapesServerShell;

import java.io.IOException;
import java.security.*;
import java.util.Enumeration;

public class Scapes {
    public static final VersionUtil.Version VERSION =
            new VersionUtil.Version(0, 0, 0, 1);
    public static String directory;
    public static boolean debug;
    public static boolean gui;
    public static boolean skipIntro;

    @SuppressWarnings("CallToSystemExit")
    public static void main(String[] args) throws IOException {
        Options options = new Options();
        options.addOption("h", "help", false, "Print this text and exit");
        options.addOption("v", "version", false, "Print version and exit");
        options.addOption("m", "mode", true, "Specify which mode to run");
        options.addOption("d", "debug", false, "Run in debug mode");
        options.addOption("c", "console", false, "Run server without gui");
        options.addOption("s", "skipintro", false, "Skip client intro");
        Parser parser = new PosixParser();
        CommandLine commandLine;
        try {
            commandLine = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println(e.getMessage());
            System.exit(255);
            return;
        }
        if (commandLine.hasOption('h')) {
            HelpFormatter helpFormatter = new HelpFormatter();
            helpFormatter.printHelp("scapes", options);
            System.exit(0);
        }
        if (commandLine.hasOption('v')) {
            System.out.println("Scapes " + VERSION);
            System.exit(0);
        }
        Policy.setPolicy(new Policy() {
            @Override
            public Permissions getPermissions(CodeSource codesource) {
                Permissions permissions = new Permissions();
                permissions.add(new AllPermission());
                return permissions;
            }

            @Override
            public Permissions getPermissions(ProtectionDomain domain) {
                Permissions permissions;
                if (domain.getClassLoader() instanceof PluginClassLoader) {
                    permissions = new Permissions();
                } else {
                    permissions = getPermissions(domain.getCodeSource());
                }
                PermissionCollection domainPermissions =
                        domain.getPermissions();
                synchronized (domainPermissions) {
                    Enumeration<Permission> domainPermission =
                            domainPermissions.elements();
                    while (domainPermission.hasMoreElements()) {
                        permissions.add(domainPermission.nextElement());
                    }
                }
                return permissions;
            }
        });
        System.setSecurityManager(new SecurityManager());
        gui = !commandLine.hasOption('c');
        debug = commandLine.hasOption('d');
        skipIntro = commandLine.hasOption('s');
        String[] cmdArgs = commandLine.getArgs();
        String mode = commandLine.getOptionValue('m', "client");
        switch (mode) {
            case "client":
                if (cmdArgs.length > 0) {
                    directory = cmdArgs[0];
                } else {
                    directory = Platform.getPlatform().getAppData("Scapes");
                }
                System.setProperty("scapes.home", directory);
                ScapesEngine engine =
                        new ScapesEngine(new ScapesClient(), directory, debug);
                if (skipIntro) {
                    engine.setState(new GameStateMenu(engine));
                } else {
                    engine.setState(
                            new GameStateStartup(new GameStateMenu(engine),
                                    new SceneImage(engine.getGraphics()
                                            .getTextureManager()
                                            .getTexture("Engine:image/Logo"),
                                            0.5d), engine));
                }
                System.exit(engine.run());
                break;
            case "server":
                if (cmdArgs.length > 0) {
                    directory = cmdArgs[0];
                } else {
                    directory = System.getProperty("user.dir");
                }
                System.setProperty("scapes.home", directory);
                if (gui) {
                    SWTLoader.loadSWT();
                    ScapesServerShell server = new ScapesServerShell();
                    server.run();
                } else {
                    ScapesServerHeadless server = new ScapesServerHeadless();
                    server.run();
                }
                break;
            default:
                System.err.println("Unknown mode: " + mode);
                System.exit(254);
        }
    }
}
