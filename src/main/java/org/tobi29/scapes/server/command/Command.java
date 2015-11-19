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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.tobi29.scapes.engine.utils.ArrayUtil;
import org.tobi29.scapes.engine.utils.math.vector.Vector3d;
import org.tobi29.scapes.server.MessageLevel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * <table summary="Standard return codes">
 * <thead>
 * <tr><th>Return Code</th><th>Description</th></tr>
 * </thead>
 * <tbody>
 * <tr><td>0</td><td>Successfully executed</td></tr>
 * <tr><td>245</td><td>Missing permissions</td></tr>
 * <tr><td>253</td><td>Execution error</td></tr>
 * <tr><td>254</td><td>Parsing error</td></tr>
 * <tr><td>255</td><td>Fatal error</td></tr>
 * </tbody>
 * </table>
 */
@FunctionalInterface
public interface Command {
    static int getInt(String value) throws CommandException {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new CommandException(253, "Unable to parse int: " + value);
        }
    }

    static long getLong(String value) throws CommandException {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new CommandException(253, "Unable to parse long: " + value);
        }
    }

    static float getFloat(String value) throws CommandException {
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            throw new CommandException(253, "Unable to parse float: " + value);
        }
    }

    static double getDouble(String value) throws CommandException {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new CommandException(253, "Unable to parse double: " + value);
        }
    }

    static Vector3d getVector3d(String[] values) throws CommandException {
        if (values.length != 3) {
            throw new CommandException(253,
                    "Unable to parse vector3d: " + ArrayUtil.join(values, " "));
        }
        try {
            return new Vector3d(Double.parseDouble(values[0]),
                    Double.parseDouble(values[1]),
                    Double.parseDouble(values[2]));
        } catch (NumberFormatException e) {
            throw new CommandException(253,
                    "Unable to parse vector3d: " + ArrayUtil.join(values, " "));
        }
    }

    static void require(Arguments args, char name) throws CommandException {
        if (!args.hasOption(name)) {
            throw new CommandException(253, "Missing argument: " + name);
        }
    }

    static <T> T require(Optional<T> object, char name)
            throws CommandException {
        if (!object.isPresent()) {
            throw new CommandException(253, "Missing argument: " + name);
        }
        return object.get();
    }

    static <O, T> T require(Function<O, Optional<T>> supplier, O option)
            throws CommandException {
        Optional<T> object = supplier.apply(option);
        if (!object.isPresent()) {
            throw new CommandException(253, "Missing argument: " + option);
        }
        return object.get();
    }

    static <O, T> T requireNullable(Function<O, T> supplier, O option)
            throws CommandException {
        T object = supplier.apply(option);
        if (object == null) {
            throw new CommandException(253, "Missing argument: " + option);
        }
        return object;
    }

    static <T> T require(Optional<T> object, String name)
            throws CommandException {
        if (!object.isPresent()) {
            throw new CommandException(253, "Not found: " + name);
        }
        return object.get();
    }

    static void error(String msg) throws CommandException {
        throw new CommandException(253, msg);
    }

    static void requirePermission(Executor executor, int level)
            throws CommandException {
        if (executor.permissionLevel() < level) {
            throw new CommandException(243, "Missing permissions");
        }
    }

    static void requirePermission(Executor executor, int level, char object)
            throws CommandException {
        if (executor.permissionLevel() < level) {
            throw new CommandException(243,
                    "Missing permissions for: " + object);
        }
    }

    void execute() throws CommandException;

    @FunctionalInterface
    interface OptionSupplier {
        void options(CommandOptions options);
    }

    @FunctionalInterface
    interface Compiler {
        void compile(Arguments args, Executor executor,
                Collection<Command> commands) throws CommandException;
    }

    interface Executor {
        Optional<String> playerName();

        String name();

        boolean message(String message, MessageLevel level);

        int permissionLevel();
    }

    class CommandOptions {
        private final Options options;

        public CommandOptions(Options options) {
            this.options = options;
        }

        public void add(String opt, String longOpt, boolean hasArg,
                String description) {
            options.addOption(opt, longOpt, hasArg, description);
        }

        public void add(String opt, String longOpt, int arguments,
                String description) {
            Option option = new Option(opt, longOpt, true, description);
            option.setArgs(arguments);
            options.addOption(option);
        }
    }

    class Arguments {
        private final CommandLine commandLine;

        public Arguments(CommandLine commandLine) {
            this.commandLine = commandLine;
        }

        public boolean hasOption(char option) {
            return commandLine.hasOption(option);
        }

        public String requireOption(char option) throws CommandException {
            return require(option(option), option);
        }

        public Optional<String> option(char option) {
            return Optional.ofNullable(commandLine.getOptionValue(option));
        }

        public String[] requireOptionArray(char option)
                throws CommandException {
            return require(optionArray(option), option);
        }

        public Optional<String[]> optionArray(char option) {
            return Optional.ofNullable(commandLine.getOptionValues(option));
        }

        public String requireOption(char option, Optional<String> def)
                throws CommandException {
            return require(option(option, def), option);
        }

        public Optional<String> option(char option, Optional<String> def) {
            return Optional.ofNullable(
                    commandLine.getOptionValue(option, def.orElse(null)));
        }

        public String option(char option, String def) {
            return commandLine.getOptionValue(option, def);
        }

        public String[] args() {
            return commandLine.getArgs();
        }

        public Optional<String> arg(int i) {
            String[] args = commandLine.getArgs();
            if (i < 0 || i >= args.length) {
                return Optional.empty();
            }
            return Optional.of(args[i]);
        }
    }

    class Compiled {
        private final Collection<Command> commands;

        public Compiled(Collection<Command> commands) {
            this.commands = commands;
        }

        public Stream<Output> execute() {
            Collection<Output> outputs = new ArrayList<>(commands.size());
            commands.forEach(command -> {
                try {
                    command.execute();
                } catch (CommandException e) {
                    outputs.add(new Output(252, e.getMessage()));
                }
            });
            return outputs.stream();
        }
    }

    class Output {
        private final int returnCode;
        private final String out;

        public Output(int returnCode, String out) {
            if (returnCode < 0 || returnCode > 255) {
                throw new IllegalArgumentException(
                        "Return code must be in range of 0 to 255");
            }
            this.returnCode = returnCode;
            this.out = out;
        }

        public int getReturnCode() {
            return returnCode;
        }

        public String getOut() {
            return out;
        }

        @Override
        public String toString() {
            return out + " (" + returnCode + ')';
        }
    }

    class Null extends Compiled {
        public Null(Output output) {
            super(Collections.singletonList(() -> {
                throw new CommandException(output);
            }));
        }
    }

    class CommandException extends Exception {
        private final int returnCode;

        public CommandException(Output output) {
            this(output.returnCode, output.out);
        }

        public CommandException(int returnCode, String message) {
            super(message);
            this.returnCode = returnCode;
        }

        public int getReturnCode() {
            return returnCode;
        }
    }
}
