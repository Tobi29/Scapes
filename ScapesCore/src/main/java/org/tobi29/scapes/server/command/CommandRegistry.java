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
import org.tobi29.scapes.engine.utils.Pair;

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
            Pattern.compile("[ ]+(?=([^\"]*\"[^\"]*\")*[^\"]*$)"), SPLIT =
            Pattern.compile(" ");
    private final Map<String, Compiler> commands = new ConcurrentHashMap<>();
    private final String prefix;

    public CommandRegistry() {
        this("");
    }

    public CommandRegistry(String prefix) {
        this.prefix = prefix;
    }

    public void register(String usage, int level,
            Command.OptionSupplier optionSupplier, Command.Compiler compiler) {
        String[] split = SPLIT.split(usage, 2);
        String name = split[0];
        usage = prefix + usage;
        commands.put(name, compiler(usage, level, optionSupplier, compiler));
    }

    public CommandRegistry group(String name) {
        CommandRegistry registry = new CommandRegistry(prefix + name + ' ');
        commands.put(name,
                (args, executor) -> registry.get(args, name + ' ', executor));
        return registry;
    }

    public Command.Compiled get(String line, Command.Executor executor) {
        String[] split = PATTERN.split(line);
        return get(split, "", executor);
    }

    private Command.Compiled get(String[] split, String prefix,
            Command.Executor executor) {
        Pair<String, String[]> pair = command(split);
        Compiler compiler = commands.get(pair.a);
        if (compiler == null) {
            return new Command.Null(new Command.Output(255,
                    "Unknown command: " + prefix + pair.a));
        }
        return compiler.compile(pair.b, executor);
    }

    private Pair<String, String[]> command(String[] split) {
        if (split.length == 0) {
            return new Pair<>("", EMPTY_STRING);
        }
        String name = split[0];
        String[] args;
        if (split.length == 1) {
            args = EMPTY_STRING;
        } else {
            args = new String[split.length - 1];
            System.arraycopy(split, 1, args, 0, args.length);
        }
        return new Pair<>(name, args);
    }

    private Compiler compiler(String usage, int level,
            Command.OptionSupplier optionSupplier, Command.Compiler compiler) {
        return (args, executor) -> {
            DefaultParser parser = new DefaultParser();
            try {
                Command.requirePermission(executor, level);
                Options options = new Options();
                options.addOption("h", "help", false, "Display this help");
                optionSupplier.options(new Command.CommandOptions(options));
                CommandLine commandLine = parser.parse(options, args);
                if (commandLine.hasOption('h')) {
                    HelpFormatter helpFormatter = new HelpFormatter();
                    StringWriter writer = new StringWriter();
                    PrintWriter printWriter = new PrintWriter(writer);
                    helpFormatter
                            .printHelp(printWriter, 74, usage, null, options, 1,
                                    3, null, false);
                    String help = writer.toString();
                    return new Command.Null(new Command.Output(1, help));
                } else {
                    Collection<Command> commands = new ArrayList<>();
                    compiler.compile(new Command.Arguments(commandLine),
                            executor, commands);
                    return new Command.Compiled(commands);
                }
            } catch (ParseException e) {
                return new Command.Null(new Command.Output(254,
                        e.getClass().getSimpleName() + ": " + e.getMessage()));
            } catch (Command.CommandException e) {
                return new Command.Null(
                        new Command.Output(253, e.getMessage()));
            }
        };
    }

    private interface Compiler {
        Command.Compiled compile(String[] args, Command.Executor executor);
    }
}
