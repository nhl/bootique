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

import javax.inject.Inject;

import io.bootique.BQCoreModule;
import io.bootique.cli.Cli;
import io.bootique.command.CommandOutcome;
import io.bootique.command.CommandWithMetadata;
import io.bootique.di.BQModule;
import io.bootique.di.Binder;
import io.bootique.help.HelpCommand;
import io.bootique.log.BootLogger;
import io.bootique.log.DefaultBootLogger;
import io.bootique.meta.application.CommandMetadata;
import io.bootique.unit.BQInternalInMemoryPrintStream;
import io.bootique.unit.BQInternalTestFactory;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DefaultRunnerIT {

    @Rule
    public BQInternalTestFactory testFactory = new BQInternalTestFactory();

    private BQInternalInMemoryPrintStream out;
    private BootLogger logger;

    @Before
    public void before() {
        this.out = new BQInternalInMemoryPrintStream(System.out);
        this.logger = new DefaultBootLogger(true, out, System.err);
    }

    @Test
    public void testRun_Explicit() {

        testFactory.app("-x")
                .module(b -> BQCoreModule.extend(b).addCommand(XCommand.class))
                .bootLogger(logger)
                .createRuntime()
                .run();

        assertTrue(out.toString().contains("x_was_run"));
    }

    @Test
    public void testRun_Implicit_Default() {

        BQInternalInMemoryPrintStream out = new BQInternalInMemoryPrintStream(System.out);
        BootLogger logger = new DefaultBootLogger(false, out, System.err);

        testFactory.app()
                .module(b -> BQCoreModule.extend(b).setDefaultCommand(XCommand.class))
                .bootLogger(logger)
                .createRuntime()
                .run();

        assertTrue(out.toString().contains("x_was_run"));
    }

    @Test
    public void testRun_Implicit_Help() {

        testFactory.app()
                .module(b -> BQCoreModule.extend(b).addCommand(XCommand.class))
                .bootLogger(logger)
                .createRuntime()
                .run();

        String help = out.toString();

        assertTrue(help.contains("-h, --help"));
        assertTrue(help.contains("-x"));
        assertFalse(help.contains("x_was_run"));
    }

    @Test
    @Ignore
    public void testRun_Implicit_NoModuleCommands_NoHelp() {

        testFactory.app()
                .module(b -> BQCoreModule.extend(b).addCommand(XCommand.class))
                .bootLogger(logger)
                .createRuntime()
                .run();

        assertFalse(out.toString().contains("-h, --help"));
    }


    @Test
    @Ignore
    public void testRun_Implicit_NoModuleCommands_HelpAllowed() {

        testFactory.app()
                .module(b -> BQCoreModule.extend(b).addCommand(XCommand.class))
                .module(TestCommandClass1.class)
                .bootLogger(logger)
                .createRuntime()
                .run();

        String help = out.toString();

        assertTrue(help.contains("-h, --help"));
        assertFalse(help.contains("-x"));
        assertTrue(help.contains("-y"));

        assertFalse(help.contains("x_was_run"));
        assertFalse(help.contains("y_was_run"));
    }


    @Test
    public void testRun_Implicit_HelpRedefined() {

        testFactory.app()
                .module(b -> BQCoreModule.extend(b).addCommand(XCommand.class))
                .bootLogger(logger)
                .createRuntime()
                .run();

        String help = out.toString();

        assertTrue(help.contains("-h, --help"));
        assertTrue(help.contains("-x"));
        assertFalse(help.contains("-y"));

        assertFalse(help.contains("x_was_run"));
        assertFalse(help.contains("y_was_run"));
    }

    @Test
    @Ignore
    public void testRun_Implicit_HelpRedefined2() {

        testFactory.app()
                .args("--xhelp")
                .module(b -> BQCoreModule.extend(b).addCommand(XCommand.class))
                .bootLogger(logger)
                .createRuntime()
                .run();

        String help = out.toString();

        assertFalse(help.contains("-h, --help"));
        assertTrue(help.contains("xhelp_was_run"));
    }

    @Test
    @Ignore
    public void testRun_Implicit_Default_NoModuleCommands() {

        testFactory.app()
                .module(b -> BQCoreModule.extend(b).setDefaultCommand(XCommand.class))
                .module(TestCommandClassX1.class)
                .bootLogger(logger)
                .createRuntime()
                .run();

        String help = out.toString();

        assertFalse(help.contains("x_was_run"));
        assertTrue(help.contains("x1_was_run"));
    }

    public static class XHelpCommand extends CommandWithMetadata {

        private BootLogger logger;

        @Inject
        public XHelpCommand(BootLogger logger) {
            // use meta from X
            super(CommandMetadata.builder(HelpCommand.class));
            this.logger = logger;
        }

        @Override
        public CommandOutcome run(Cli cli) {
            logger.stdout("xhelp_was_run");
            return CommandOutcome.succeeded();
        }
    }

    public static class XCommand extends CommandWithMetadata {

        private BootLogger logger;

        @Inject
        public XCommand(BootLogger logger) {
            super(CommandMetadata.builder(XCommand.class));
            this.logger = logger;
        }

        @Override
        public CommandOutcome run(Cli cli) {
            logger.stdout("x_was_run");
            return CommandOutcome.succeeded();
        }
    }

    public static class X1Command extends CommandWithMetadata {

        private BootLogger logger;

        @Inject
        public X1Command(BootLogger logger) {
            // use meta from X
            super(CommandMetadata.builder(XCommand.class));
            this.logger = logger;
        }

        @Override
        public CommandOutcome run(Cli cli) {
            logger.stdout("x1_was_run");
            return CommandOutcome.succeeded();
        }
    }

    public static class YCommand extends CommandWithMetadata {

        private BootLogger logger;

        @Inject
        public YCommand(BootLogger logger) {
            super(CommandMetadata.builder(YCommand.class).alwaysOn());
            this.logger = logger;
        }

        @Override
        public CommandOutcome run(Cli cli) {
            logger.stdout("y_was_run");
            return CommandOutcome.succeeded();
        }
    }

    public static class TestCommandClass1 implements BQModule {

        @Override
        public void configure(Binder binder) {
            BQCoreModule
                    .extend(binder)
                    .addCommand(YCommand.class)
                    .addCommand(HelpCommand.class)
                    .noModuleCommands();
        }
    }

    public static class TestCommandClassX1 implements BQModule {

        @Override
        public void configure(Binder binder) {
            BQCoreModule
                    .extend(binder)
                    .addCommand(X1Command.class)
                    .noModuleCommands();
        }
    }
}
