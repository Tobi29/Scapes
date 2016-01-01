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
package org.tobi29.scapes.desktop;

import org.apache.commons.cli.*;
import org.tobi29.scapes.Debug;
import org.tobi29.scapes.Version;
import org.tobi29.scapes.client.BasicSaveStorage;
import org.tobi29.scapes.client.SaveStorage;
import org.tobi29.scapes.client.ScapesClient;
import org.tobi29.scapes.engine.ScapesEngine;
import org.tobi29.scapes.engine.utils.io.IOFunction;
import org.tobi29.scapes.engine.utils.io.filesystem.FilePath;
import org.tobi29.scapes.engine.utils.io.filesystem.FileUtil;
import org.tobi29.scapes.plugins.Sandbox;
import org.tobi29.scapes.server.format.basic.BasicWorldSource;
import org.tobi29.scapes.server.shell.ScapesServerHeadless;
import org.tobi29.scapes.server.shell.ScapesStandaloneServer;

import java.io.IOException;
import java.util.regex.Pattern;

public class Scapes {
    private static final Pattern HOME_PATH = Pattern.compile("\\$HOME");

    @SuppressWarnings({"CallToSystemExit", "CallToPrintStackTrace"})
    public static void main(String[] args) {
        Options options = new Options();
        options.addOption("h", "help", false, "Print this text and exit");
        options.addOption("v", "version", false, "Print version and exit");
        options.addOption("m", "mode", true, "Specify which mode to run");
        options.addOption("d", "debug", false, "Run in debug mode");
        options.addOption("r", "socketsp", false,
                "Use network socket for singleplayer");
        options.addOption("s", "skipintro", false, "Skip client intro");
        options.addOption("c", "config", true, "Config directory for server");
        DefaultParser parser = new DefaultParser();
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
            return;
        }
        if (commandLine.hasOption('v')) {
            System.out.println("Scapes " + Version.VERSION);
            System.exit(0);
            return;
        }
        Sandbox.sandbox();
        if (commandLine.hasOption('d')) {
            Debug.enable();
        }
        if (commandLine.hasOption('r')) {
            Debug.socketSingleplayer(true);
        }
        String[] cmdArgs = commandLine.getArgs();
        String mode = commandLine.getOptionValue('m', "client");
        FilePath home;
        if (cmdArgs.length > 0) {
            home = FileUtil.path(HOME_PATH.matcher(cmdArgs[0])
                    .replaceAll(System.getProperty("user.home")))
                    .toAbsolutePath();
            System.setProperty("user.dir", home.toAbsolutePath().toString());
        } else {
            home = FileUtil.path(System.getProperty("user.dir"))
                    .toAbsolutePath();
        }
        switch (mode) {
            case "client":
                IOFunction<ScapesClient, SaveStorage> saves =
                        scapes -> new BasicSaveStorage(home.resolve("saves"));
                ScapesEngine engine = new ScapesEngine(
                        new ScapesClient(commandLine.hasOption('s'), saves),
                        home, Debug.enabled());
                System.exit(engine.run());
                break;
            case "server":
                FilePath config;
                if (commandLine.hasOption('c')) {
                    config = home.resolve(
                            FileUtil.path(commandLine.getOptionValue('c')))
                            .toAbsolutePath();
                } else {
                    config = home;
                }
                try {
                    ScapesStandaloneServer server =
                            new ScapesServerHeadless(config);
                    server.run(() -> new BasicWorldSource(home));
                } catch (IOException e) {
                    e.printStackTrace();
                    System.exit(200);
                    return;
                }
                break;
            default:
                System.err.println("Unknown mode: " + mode);
                System.exit(254);
        }
    }
}
