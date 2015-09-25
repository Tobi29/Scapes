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
import org.tobi29.scapes.server.MessageLevel;
import org.tobi29.scapes.server.ScapesServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;

public class ScapesServerHeadless extends ScapesStandaloneServer {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ScapesServerHeadless.class);

    public ScapesServerHeadless(Path path) {
        super(path);
    }

    @Override
    protected Runnable loop() {
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(System.in));
        return () -> {
            try {
                while (reader.ready()) {
                    String line = reader.readLine();
                    if (line != null) {
                        server.commandRegistry().get(line, this).execute()
                                .forEach(output -> message(output.toString(),
                                        MessageLevel.FEEDBACK_ERROR));
                    }
                }
            } catch (IOException e) {
                LOGGER.error("Error reading console input: {}", e.toString());
                server.scheduleStop(ScapesServer.ShutdownReason.ERROR);
            }
        };
    }

    @Override
    public boolean message(String message, MessageLevel level) {
        switch (level) {
            case SERVER_ERROR:
            case FEEDBACK_ERROR: {
                LOGGER.error(message);
                break;
            }
            default: {
                LOGGER.info(message);
                break;
            }
        }
        return true;
    }
}
