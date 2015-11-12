package org.tobi29.scapes.server.extension.base;

import org.tobi29.scapes.engine.utils.ArrayUtil;
import org.tobi29.scapes.server.MessageLevel;
import org.tobi29.scapes.server.ScapesServer;
import org.tobi29.scapes.server.command.Command;
import org.tobi29.scapes.server.command.CommandRegistry;
import org.tobi29.scapes.server.connection.PlayerConnection;
import org.tobi29.scapes.server.connection.ServerConnection;
import org.tobi29.scapes.server.extension.ServerExtension;
import org.tobi29.scapes.server.extension.ServerExtensions;

import java.util.Optional;

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
