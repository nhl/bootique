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
import io.bootique.di.DIRuntimeException;
import io.bootique.meta.application.CommandMetadata;
import io.bootique.unit.BQInternalTestFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.HashSet;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Arrays.sort;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CommandsIT {

    @Rule
    public BQInternalTestFactory testFactory = new BQInternalTestFactory();

    private String[] args;

    private static void assertCommandKeys(Map<String, ManagedCommand> commands, String... expectedCommands) {
        assertEquals(commands.size(), expectedCommands.length);
        assertEquals(new HashSet<>(asList(expectedCommands)), commands.keySet());
    }

    @Before
    public void before() {
        args = new String[]{"a", "b", "c"};
    }

    @Test
    public void testModuleCommands() {

        BQRuntime runtime = testFactory.app(args).createRuntime();
        CommandManager commandManager = runtime.getInstance(CommandManager.class);

        Map<String, ManagedCommand> commands = commandManager.getAllCommands();
        assertCommandKeys(commands, "help", "help-config");

        assertFalse(commands.get("help").isHidden());
        assertFalse(commands.get("help").isDefault());
        assertTrue(commands.get("help").isHelp());

        assertFalse(commands.get("help-config").isHidden());
        assertFalse(commands.get("help-config").isDefault());
    }

    @Test
    public void testNoModuleCommands() {
        BQRuntime runtime = testFactory.app(args).module(b -> BQCoreModule.extend(b).noModuleCommands()).createRuntime();
        CommandManager commandManager = runtime.getInstance(CommandManager.class);

        Map<String, ManagedCommand> commands = commandManager.getAllCommands();
        assertCommandKeys(commands, "help", "help-config");

        assertFalse(commands.get("help").isHidden());
        assertFalse(commands.get("help").isDefault());
        assertTrue(commands.get("help").isHelp());

        assertTrue(commands.get("help-config").isHidden());
        assertFalse(commands.get("help-config").isDefault());
        assertFalse(commands.get("help-config").isHelp());
    }

    @Test
    public void testModule_ExtraCommandAsType() {
        BQRuntime runtime = testFactory.app(args).module(b -> BQCoreModule.extend(b).addCommand(C1.class)).createRuntime();
        CommandManager commandManager = runtime.getInstance(CommandManager.class);

        Map<String, ManagedCommand> commands = commandManager.getAllCommands();
        assertCommandKeys(commands, "c1", "help", "help-config");

        assertFalse(commands.get("help").isHidden());
        assertFalse(commands.get("help").isDefault());
        assertTrue(commands.get("help").isHelp());

        assertFalse(commands.get("help-config").isHidden());
        assertFalse(commands.get("help-config").isDefault());

        assertTrue(commands.containsKey("c1"));
        assertFalse(commands.get("c1").isDefault());
    }

    @Test
    public void testModule_ExtraCommandAsInstance() {
        BQRuntime runtime = testFactory.app(args).module(TestCommandClassC1.class).createRuntime();
        CommandManager commandManager = runtime.getInstance(CommandManager.class);

        Map<String, ManagedCommand> commands = commandManager.getAllCommands();
        assertCommandKeys(commands, "c1", "help", "help-config");
    }

    //Exception when override default help command because help command is always on.
    @Test(expected = DIRuntimeException.class)
    public void testModule_ExtraCommandOverride() {
        BQRuntime runtime = testFactory.app(args).module(TestCommandClassC2Help.class).createRuntime();
        CommandManager commandManager = runtime.getInstance(CommandManager.class);

        Map<String, ManagedCommand> commands = commandManager.getAllCommands();
        assertCommandKeys(commands, "help", "help-config");
    }

    public static class C1 implements Command {
        @Override
        public CommandOutcome run(Cli cli) {
            return CommandOutcome.succeeded();
        }
    }

    public static class C2_Help implements Command {
        @Override
        public CommandOutcome run(Cli cli) {
            return CommandOutcome.succeeded();
        }

        @Override
        public CommandMetadata getMetadata() {
            return CommandMetadata.builder("help").build();
        }
    }

    public static class TestCommandClassC1 implements BQModule {

        @Override
        public void configure(Binder binder) {
            BQCoreModule
                    .extend(binder)
                    .addCommand(C1.class);
        }
    }

    public static class TestCommandClassC2Help implements BQModule {

        @Override
        public void configure(Binder binder) {
            BQCoreModule
                    .extend(binder)
                    .addCommand(C2_Help.class);
        }
    }
}
