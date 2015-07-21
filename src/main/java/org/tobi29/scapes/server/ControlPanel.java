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

package org.tobi29.scapes.server;

import org.tobi29.scapes.server.command.Command;

import java.util.Map;
import java.util.Optional;

public interface ControlPanel extends Command.Executor {
    @Override
    default Optional<String> playerName() {
        return Optional.empty();
    }

    @Override
    default void tell(String message) {
        appendLog(message);
    }

    @Override
    default int permissionLevel() {
        return 10;
    }

    String id();

    void updatePlayers(String[] players);

    void updateWorlds(String[] worlds);

    void appendLog(String line);

    void sendProfilerResults(long ram, Map<String, Double> tps);

    void replaced();

    boolean isClosed();
}
