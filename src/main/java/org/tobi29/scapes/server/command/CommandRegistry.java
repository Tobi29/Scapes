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
package org.tobi29.scapes.server.command;

import org.apache.commons.cli.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class CommandRegistry {
    private static final String[] EMPTY_STRING = new String[0];
    private static final Pattern PATTERN =
            Pattern.compile("[ ]+(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
    private final Map<String, CommandParser> commands =
            new ConcurrentHashMap<>();

    public void register(String name, int level, Command.OptionSupplier options,
            Command.Compiler command) {
        commands.put(name, new CommandParser(command, options, level));
    }

    public Command.Compiled get(String line, Command.Executor executor) {
        String[] split = PATTERN.split(line, 2);
        String commandName = split[0];
        String[] commandArgs;
        if (split.length == 1) {
            commandArgs = EMPTY_STRING;
        } else {
            commandArgs = PATTERN.split(split[1]);
        }
        CommandParser command = commands.get(commandName);
        if (command == null) {
            return new Command.Null(
                    new Command.Output(255, "Unknown command: " + commandName));
        }
        DefaultParser parser = new DefaultParser();
        try {
            Command.requirePermission(executor, command.level);
            Options options = new Options();
            options.addOption("h", "help", false, "Display this help");
            command.options.options(new Command.CommandOptions(options));
            CommandLine args = parser.parse(options, commandArgs);
            if (args.hasOption('h')) {
                HelpFormatter helpFormatter = new HelpFormatter();
                StringWriter writer = new StringWriter();
                PrintWriter printWriter = new PrintWriter(writer);
                helpFormatter
                        .printHelp(printWriter, 74, commandName, null, options,
                                1, 3, null, false);
                String help = writer.toString();
                return new Command.Null(new Command.Output(1, help));
            } else {
                Collection<Command> commands = new ArrayList<>();
                command.command.compile(new Command.Arguments(args), executor,
                        commands);
                return new Command.Compiled(commands);
            }
        } catch (ParseException e) {
            return new Command.Null(new Command.Output(254,
                    e.getClass().getSimpleName() + ": " + e.getMessage()));
        } catch (Command.CommandException e) {
            return new Command.Null(new Command.Output(253, e.getMessage()));
        }
    }

    private static class CommandParser {
        private final Command.Compiler command;
        private final Command.OptionSupplier options;
        private final int level;

        private CommandParser(Command.Compiler command,
                Command.OptionSupplier options, int level) {
            this.command = command;
            this.options = options;
            this.level = level;
        }
    }
}
