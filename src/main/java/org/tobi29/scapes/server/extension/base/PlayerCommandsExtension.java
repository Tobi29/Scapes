package org.tobi29.scapes.server.extension.base;

import org.tobi29.scapes.engine.utils.StringUtil;
import org.tobi29.scapes.server.MessageLevel;
import org.tobi29.scapes.server.ScapesServer;
import org.tobi29.scapes.server.command.Command;
import org.tobi29.scapes.server.command.CommandRegistry;
import org.tobi29.scapes.server.connection.PlayerConnection;
import org.tobi29.scapes.server.connection.ServerConnection;
import org.tobi29.scapes.server.extension.ServerExtension;
import org.tobi29.scapes.server.extension.ServerExtensions;

import java.util.Arrays;
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
                    .map(PlayerConnection::nickname)
                    .filter(nickname -> pattern.matcher(nickname).matches())
                    .forEach(message -> executor
                            .message(message, MessageLevel.FEEDBACK_INFO)));
        });

        group.register("add PLAYER-ID...", 9, options -> {
        }, (args, executor, commands) -> {
            String[] ids = args.args();
            Arrays.stream(ids)
                    .forEach(id -> commands.add(() -> server.add(id)));
        });

        group.register("remove PLAYER-ID...", 9, options -> {
        }, (args, executor, commands) -> {
            String[] ids = args.args();
            Arrays.stream(ids)
                    .forEach(id -> commands.add(() -> server.remove(id)));
        });

        group.register("kick PLAYER-NAME...", 9, options -> {
        }, (args, executor, commands) -> {
            String[] playerNames = args.args();
            String message = "Kick by an Admin!";
            Arrays.stream(playerNames)
                    .forEach(playerName -> commands.add(() -> {
                        PlayerConnection player =
                                Command.require(connection::playerByName,
                                        playerName);
                        player.disconnect(message);
                    }));
        });

        group.register("op -l LEVEL PLAYER-NAME...", 10, options -> options
                        .add("l", "level", true, "Permission level (0-10)"),
                (args, executor, commands) -> {
                    String[] playerNames = args.args();
                    int permissionLevel =
                            Command.getInt(args.requireOption('l'));
                    Arrays.stream(playerNames)
                            .forEach(playerName -> commands.add(() -> {
                                PlayerConnection player = Command.require(
                                        connection::playerByName, playerName);
                                player.setPermissionLevel(permissionLevel);
                            }));
                });
    }
}
