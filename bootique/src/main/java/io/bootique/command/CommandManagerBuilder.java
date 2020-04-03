/*
 * Licensed to ObjectStyle LLC under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ObjectStyle LLC licenses
 * this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.bootique.command;

import io.bootique.BootiqueException;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @since 0.25
 */
public class CommandManagerBuilder<T extends CommandManagerBuilder<T>> {

    protected Collection<Command> commands;
    protected Command helpCommand;
    protected Optional<Command> defaultCommand;
    private boolean hideModuleCommands;

    public CommandManagerBuilder(Collection<Command> commands) {
        this.commands = commands;
    }

    public T helpCommand(Command helpCommand) {
        this.helpCommand = helpCommand;
        return (T) this;
    }

    public T defaultCommand(Optional<Command> defaultCommand) {
        this.defaultCommand = defaultCommand;
        return (T) this;
    }

    public CommandManager build() {
        return new DefaultCommandManager(buildCommandMap());
    }

    public T hideModuleCommands(boolean flag) {
        this.hideModuleCommands = flag;
        return (T) this;
    }

    protected Map<String, ManagedCommand> buildCommandMap() {
        Map<String, ManagedCommand> commandMap = new HashMap<>();

        loadCommands(commandMap);
        loadHelpCommand(commandMap);
        loadDefaultCommand(commandMap);
        return commandMap;
    }

    protected void loadCommands(Map<String, ManagedCommand> commandMap) {
        commands.forEach(c -> {
            if (hideModuleCommands && !c.getMetadata().isAlwaysOn()) {
                ManagedCommand mc = ManagedCommand.builder(c).asHidden().build();
                addCommandNoOverride(commandMap, mc);
            } else {
                ManagedCommand mc = ManagedCommand.builder(c).build();
                addCommandNoOverride(commandMap, mc);
            }
        });
    }

    protected void loadHelpCommand(Map<String, ManagedCommand> commandMap) {

        addCommandNoOverride(commandMap, ManagedCommand.builder(helpCommand).asHelp().build());
    }

    protected void loadDefaultCommand(Map<String, ManagedCommand> commandMap) {
        // as default command can serve as an ad-hoc alias for another command, it is allowed to override other
        // commands with the same name with no complaints
        defaultCommand.ifPresent(c -> addCommand(commandMap, ManagedCommand.builder(c).asDefault().build()));
    }

    protected ManagedCommand addCommand(Map<String, ManagedCommand> commandMap, ManagedCommand managedCommand) {

        Command command = managedCommand.getCommand();
        String name = command.getMetadata().getName();
        return commandMap.put(name, managedCommand);
    }

    protected void addCommandNoOverride(Map<String, ManagedCommand> commandMap, ManagedCommand managedCommand) {

        ManagedCommand existing = addCommand(commandMap, managedCommand);

        // complain on dupes
        if (existing != null && existing.getCommand() != managedCommand.getCommand()) {
            String c1 = existing.getCommand().getClass().getName();
            String c2 = managedCommand.getCommand().getClass().getName();

            String message = String.format("More than one DI command named '%s'. Conflicting types: %s, %s.",
                    managedCommand.getCommand().getMetadata().getName(), c1, c2);
            throw new BootiqueException(1, message);
        }
    }
}
