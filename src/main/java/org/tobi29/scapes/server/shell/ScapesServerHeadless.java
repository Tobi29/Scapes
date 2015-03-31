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

package org.tobi29.scapes.server.shell;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.engine.utils.SleepUtil;
import org.tobi29.scapes.engine.utils.io.filesystem.FileSystem;
import org.tobi29.scapes.server.ScapesServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Optional;

public class ScapesServerHeadless extends ScapesStandaloneServer {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ScapesServerHeadless.class);

    public ScapesServerHeadless(FileSystem files) throws IOException {
        super(files);
    }

    public void run() throws IOException {
        if (!directory.exists()) {
            throw new IOException("No save found");
        }
        while (true) {
            start();
            try {
                BufferedReader reader =
                        new BufferedReader(new InputStreamReader(System.in));
                while (!server.hasStopped()) {
                    if (reader.ready()) {
                        String line = reader.readLine();
                        if (line != null) {
                            server.getCommandRegistry().get(line, this)
                                    .execute().forEach(output -> System.out
                                    .println(output.toString()));
                        }
                    } else {
                        SleepUtil.sleep(100);
                    }
                }
            } catch (IOException e) {
                LOGGER.error("Error reading console input: {}", e.toString());
                server.stop(ScapesServer.ShutdownReason.ERROR);
            }
            if (stop() != ScapesServer.ShutdownReason.RELOAD) {
                break;
            }
        }
    }

    @Override
    public Optional<String> getPlayerName() {
        return Optional.empty();
    }

    @Override
    public void tell(String message) {
        System.out.println(message);
    }

    @Override
    public int getPermissionLevel() {
        return 10;
    }
}
