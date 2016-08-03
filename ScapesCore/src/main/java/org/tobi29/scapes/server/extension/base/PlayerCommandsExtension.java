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

import org.tobi29.scapes.engine.utils.Streams;
import org.tobi29.scapes.engine.utils.StringUtil;
import org.tobi29.scapes.server.MessageLevel;
import org.tobi29.scapes.server.ScapesServer;
import org.tobi29.scapes.server.command.Command;
import org.tobi29.scapes.server.command.CommandRegistry;
import org.tobi29.scapes.server.connection.PlayerConnection;
import org.tobi29.scapes.server.connection.ServerConnection;
import org.tobi29.scapes.server.extension.ServerExtension;
import org.tobi29.scapes.server.extension.ServerExtensions;

import java.util.regex.Pattern;

public class PlayerCommandsExtension extends ServerExtension {
    public PlayerCommandsExtension(ScapesServer server) {
        super(server);
    }

    @Override
    public void init(ServerExtensions.Registrar registrar) {
        CommandRegistry group = server.commandRegistry().group("players");
        ServerConnection connection = server.connection();

        group.register("list [MATCH]", 9, options -> {
        }, (args, executor, commands) -> {
            String exp = args.arg(0).orElse("*");
            Pattern pattern = StringUtil.wildcard(exp);
            commands.add(() -> server.connection().players()
                    .map(PlayerConnection::name)
                    .filter(nickname -> pattern.matcher(nickname).matches())
                    .forEach(message -> executor
                            .message(message, MessageLevel.FEEDBACK_INFO)));
        });

        group.register("add PLAYER-ID...", 9, options -> {
        }, (args, executor, commands) -> {
            String[] ids = args.args();
            Streams.forEach(ids, id -> commands.add(() -> server.add(id)));
        });

        group.register("remove PLAYER-ID...", 9, options -> {
        }, (args, executor, commands) -> {
            String[] ids = args.args();
            Streams.forEach(ids, id -> commands.add(() -> server.remove(id)));
        });

        group.register("kick PLAYER-NAME...", 9, options -> {
        }, (args, executor, commands) -> {
            String[] playerNames = args.args();
            String message = "Kick by an Admin!";
            Streams.forEach(playerNames, playerName -> commands.add(() -> {
                PlayerConnection player =
                        Command.require(connection::playerByName, playerName);
                player.disconnect(message);
            }));
        });

        group.register("op -l LEVEL PLAYER-NAME...", 10, options -> options
                        .add("l", "level", true, "Permission level (0-10)"),
                (args, executor, commands) -> {
                    String[] playerNames = args.args();
                    int permissionLevel =
                            Command.getInt(args.requireOption('l'));
                    Streams.forEach(playerNames,
                            playerName -> commands.add(() -> {
                                PlayerConnection player = Command.require(
                                        connection::playerByName, playerName);
                                player.setPermissionLevel(permissionLevel);
                            }));
                });
    }
}
