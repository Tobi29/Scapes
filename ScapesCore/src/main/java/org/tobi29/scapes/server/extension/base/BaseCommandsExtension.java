/*
 * Copyright 2012-2016 Tobi29
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

package org.tobi29.scapes.server.extension.base;

import java8.util.Optional;
import org.tobi29.scapes.engine.utils.ArrayUtil;
import org.tobi29.scapes.server.MessageLevel;
import org.tobi29.scapes.server.ScapesServer;
import org.tobi29.scapes.server.command.Command;
import org.tobi29.scapes.server.command.CommandRegistry;
import org.tobi29.scapes.server.connection.PlayerConnection;
import org.tobi29.scapes.server.connection.ServerConnection;
import org.tobi29.scapes.server.extension.ServerExtension;
import org.tobi29.scapes.server.extension.ServerExtensions;

public class BaseCommandsExtension extends ServerExtension {
    public BaseCommandsExtension(ScapesServer server) {
        super(server);
    }

    @Override
    public void init(ServerExtensions.Registrar registrar) {
        ServerConnection connection = server.connection();
        CommandRegistry registry = server.commandRegistry();
        CommandRegistry serverGroup = registry.group("server");

        registry.register("say MESSAGE...", 0, options -> {
            options.add("n", "name", true, "Name used for prefix");
            options.add("r", "raw", false, "Disable prefix");
        }, (args, executor, commands) -> {
            String message;
            if (args.hasOption('r')) {
                Command.requirePermission(executor, 8, 'r');
                message = ArrayUtil.join(args.args(), " ");
            } else {
                String name;
                Optional<String> nameOption = args.option('n');
                if (nameOption.isPresent()) {
                    Command.requirePermission(executor, 8, 'n');
                    name = nameOption.get();
                } else {
                    name = executor.name();
                }
                message = '<' + name + "> " + ArrayUtil.join(args.args(), " ");
            }
            commands.add(() -> server.connection()
                    .message(message, MessageLevel.CHAT));
        });

        registry.register("tell MESSAGE...", 0, options -> {
            options.add("t", "target", true, "Target player");
            options.add("n", "name", true, "Name used for prefix");
            options.add("r", "raw", false, "Disable prefix");
        }, (args, executor, commands) -> {
            String targetName = args.requireOption('t', executor.playerName());
            String message;
            if (args.hasOption('r')) {
                Command.requirePermission(executor, 8, 'r');
                message = ArrayUtil.join(args.args(), " ");
            } else {
                String name;
                Optional<String> nameOption = args.option('n');
                if (nameOption.isPresent()) {
                    Command.requirePermission(executor, 8, 'n');
                    name = nameOption.get();
                } else {
                    name = executor.name();
                }
                message = '[' + name + "] " + ArrayUtil.join(args.args(), " ");
            }
            commands.add(() -> {
                PlayerConnection target =
                        Command.require(connection::playerByName, targetName);
                target.message(message, MessageLevel.CHAT);
            });
        });

        serverGroup.register("stop", 10, options -> {
        }, (args, executor, commands) -> server
                .scheduleStop(ScapesServer.ShutdownReason.STOP));

        serverGroup.register("reload", 10, options -> {
        }, (args, executor, commands) -> server
                .scheduleStop(ScapesServer.ShutdownReason.RELOAD));
    }
}
