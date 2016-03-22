package org.tobi29.scapes.server;

import java8.util.Optional;
import java8.util.function.DoubleSupplier;
import java8.util.function.Function;
import java8.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.engine.server.ControlPanelProtocol;
import org.tobi29.scapes.engine.utils.CPUUtil;
import org.tobi29.scapes.engine.utils.StringUtil;
import org.tobi29.scapes.server.command.Command;
import org.tobi29.scapes.server.connection.PlayerConnection;

import java.util.List;

public class ControlPanel implements Command.Executor {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ControlPanel.class);
    private static final Function<String, String> ESCAPE = StringUtil
            .replace("&", "&amp;", ">", "&gt;", "<", "&lt;", "'", "&apos;",
                    "\"", "&quot;")::apply;
    private final ControlPanelProtocol connection;
    private final ScapesServer server;

    public ControlPanel(ControlPanelProtocol connection, ScapesServer server) {
        this.connection = connection;
        this.server = server;
        initCommands();
        LOGGER.info("Control panel accepted from {}", connection);
    }

    private void initCommands() {
        connection.addCommand("Ping:Ping", command -> {
            if (command.length >= 1) {
                connection.send("Ping:Pong", command[0]);
            }
        });
        connection.addCommand("Console:Command", command -> {
            if (command.length != 1) {
                return;
            }
            server.commandRegistry().get(command[0], this).execute().forEach(
                    output -> message(output.toString(),
                            MessageLevel.FEEDBACK_ERROR));
        });
        Optional<CPUUtil.Reader> cpuReader = CPUUtil.reader();
        DoubleSupplier cpuSupplier;
        if (cpuReader.isPresent()) {
            CPUUtil.Reader reader = cpuReader.get();
            cpuSupplier = reader::totalCPU;
        } else {
            cpuSupplier = () -> Double.NaN;
        }
        Runtime runtime = Runtime.getRuntime();
        connection.addCommand("Stat:List", command -> {
            double cpu = cpuSupplier.getAsDouble();
            long memory = runtime.totalMemory() - runtime.freeMemory();
            connection.send("Stat:Send", "CPU", String.valueOf(cpu), "Memory",
                    String.valueOf(memory));
        });
        connection.addCommand("Players:List", command -> {
            List<String> players = server.connection().players()
                    .map(PlayerConnection::name)
                    .collect(Collectors.toList());
            connection.send("Players:Send",
                    players.toArray(new String[players.size()]));
        });
    }

    @Override
    public Optional<String> playerName() {
        return Optional.empty();
    }

    @Override
    public String name() {
        return "Control Panel";
    }

    @Override
    public boolean message(String message, MessageLevel level) {
        String style;
        switch (level) {
            case SERVER_ERROR:
            case FEEDBACK_ERROR:
                style = "color: red;";
                break;
            default:
                style = "";
                break;
        }
        message = ESCAPE.apply(message);
        String html = "<span style=\"" + style + "\">" + message + "</span>";
        connection.send("Core:Message", html);
        return true;
    }

    @Override
    public int permissionLevel() {
        return 10;
    }
}
