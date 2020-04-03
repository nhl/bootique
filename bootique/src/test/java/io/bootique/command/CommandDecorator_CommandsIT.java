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

import io.bootique.BQCoreModule;
import io.bootique.BQRuntime;
import io.bootique.cli.Cli;
import io.bootique.di.BQModule;
import io.bootique.di.Binder;
import io.bootique.meta.application.CommandMetadata;
import io.bootique.unit.BQInternalTestFactory;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class CommandDecorator_CommandsIT {

    @Rule
    public BQInternalTestFactory testFactory = new BQInternalTestFactory();

    @Test
    public void testAlsoRun_DecorateWithPrivate() {
        BQRuntime runtime = createRuntime();
        CommandOutcome outcome = runtime.run();

        waitForAllToFinish();

        assertTrue(outcome.isSuccess());
        assertTrue(getCommand(runtime, MainCommand.class).isExecuted());
        assertTrue(getCommand(runtime, SuccessfulCommand.class).isExecuted());
    }

    @Test
    public void testBeforeRun_DecorateWithPrivate() {
        BQRuntime runtime = createRuntime();
        CommandOutcome outcome = runtime.run();

        waitForAllToFinish();

        assertTrue(outcome.isSuccess());
        assertTrue(getCommand(runtime, MainCommand.class).isExecuted());
        assertTrue(getCommand(runtime, SuccessfulCommand.class).isExecuted());
    }

    public static class TestCommandClass extends CommandWithMetadata implements BQModule {

        CommandDecorator decorator = CommandDecorator.beforeRun("s");

        public TestCommandClass() {
            super(CommandMetadata.builder(MainCommand.class).alwaysOn().build());
        }

        @Override
        public CommandOutcome run(Cli cli) {
            return CommandOutcome.succeeded();
        }

        @Override
        public void configure(Binder binder) {
            BQCoreModule.extend(binder)
                    .addCommand(TestCommandClass.class)
                    .addCommand(SuccessfulCommand.class)
                    .addCommand(MainCommand.class)
                    .decorateCommand(MainCommand.class, decorator)
                    .noModuleCommands();
        }
    }

    private BQRuntime createRuntime() {
        return testFactory
                .app("--a")
                .module(TestCommandClass.class)
                .createRuntime();
    }

    private <T extends Command> T getCommand(BQRuntime runtime, Class<T> type) {
        return (T) runtime.getInstance(CommandManager.class).lookupByType(type).getCommand();
    }

    private void waitForAllToFinish() {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    private static class MainCommand extends CommandDecoratorIT.ExecutableOnceCommand {

        private static final String NAME = "a";

        MainCommand() {
            super(NAME);
        }
    }

    private static class SuccessfulCommand extends CommandDecoratorIT.ExecutableOnceCommand {

        private static final String NAME = "s";

        SuccessfulCommand() {
            super(NAME);
        }
    }
}
