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

package io.bootique.run;

import io.bootique.cli.Cli;
import io.bootique.command.Command;
import io.bootique.command.CommandManager;
import io.bootique.command.CommandOutcome;
import io.bootique.command.ExecutionPlanBuilder;
import io.bootique.command.ManagedCommand;

public class DefaultRunner implements Runner {

    private Cli cli;
    private CommandManager commandManager;
    private ExecutionPlanBuilder executionPlanBuilder;

    public DefaultRunner(Cli cli, CommandManager commandManager, ExecutionPlanBuilder executionPlanBuilder) {
        this.cli = cli;
        this.commandManager = commandManager;
        this.executionPlanBuilder = executionPlanBuilder;
    }

    @Override
    public CommandOutcome run() {
        return getCommand().run(cli);
    }

    private Command getCommand() {
        return prepareForExecution(bareCommand());
    }

    private Command prepareForExecution(Command bareCommand) {
        return executionPlanBuilder.prepareForExecution(bareCommand);
    }

    private Command bareCommand() {
        if (cli.commandName() != null) {
            ManagedCommand explicitCommand = commandManager.getAllCommands().get(cli.commandName());
            if (explicitCommand == null || explicitCommand.isDefault()) {
                throw new IllegalStateException("Not a valid command: " + cli.commandName());
            }

            return explicitCommand.getCommand();
        }

        // command not found in CLI .. go through defaults

        return commandManager.getPublicDefaultCommand() // 1. runtime default command
                .orElse(commandManager.getPublicHelpCommand() // 2. help command
                        .orElse(cli -> CommandOutcome.succeeded())); // 3. placeholder noop command
    }

}
