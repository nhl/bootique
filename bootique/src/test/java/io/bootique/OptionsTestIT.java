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

package io.bootique;

import io.bootique.cli.Cli;
import io.bootique.command.CommandOutcome;
import io.bootique.command.CommandWithMetadata;
import io.bootique.di.BQModule;
import io.bootique.di.Binder;
import io.bootique.di.DIRuntimeException;
import io.bootique.log.BootLogger;
import io.bootique.log.DefaultBootLogger;
import io.bootique.meta.application.ApplicationMetadata;
import io.bootique.meta.application.CommandMetadata;
import io.bootique.meta.application.OptionMetadata;
import io.bootique.unit.BQInternalInMemoryPrintStream;
import io.bootique.unit.BQInternalTestFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class OptionsTestIT {

    private BQInternalInMemoryPrintStream out;
    private BootLogger logger;

    @Rule
    public BQInternalTestFactory testFactory = new BQInternalTestFactory();

    private String[] args;

    @Before
    public void before() {
        this.out = new BQInternalInMemoryPrintStream(System.out);
        this.logger = new DefaultBootLogger(true, out, System.err);
        args = new String[]{"a", "b", "c"};
    }

    @Test
    public void testSystemOutModuleOptions() {

        BQRuntime runtime = testFactory
                .app(args)
                .module(TestCommandClass2.class)
                .bootLogger(logger)
                .createRuntime();

        ApplicationMetadata applicationMetadata = runtime.getInstance(ApplicationMetadata.class);
        runtime.run();

        String outString = out.toString();

        assertTrue(outString.contains("test-command-class"));
        assertTrue(outString.contains("classOption"));
        assertEquals(0, applicationMetadata.getOptions().size());
    }

    @Test(expected = DIRuntimeException.class)
    public void testCallHideModuleOptions() {
        BQRuntime runtime = testFactory
                .app("--noOption1")
                .module(TestNoOptionOptionClass.class)
                .createRuntime();

        ApplicationMetadata applicationMetadata = runtime.getInstance(ApplicationMetadata.class);
        assertEquals(0, applicationMetadata.getOptions().size());

        runtime.run();
    }

    @Test
    public void testAddModuleOptions() {

        BQRuntime runtime = testFactory
                .app(args)
                .module(TestOptionClass.class)
                .createRuntime();

        ApplicationMetadata applicationMetadata = runtime.getInstance(ApplicationMetadata.class);

        assertEquals(3, applicationMetadata.getOptions().size());
        assertTrue(applicationMetadata.getOptions().stream().anyMatch(o -> o.getName().equals(TestOptionClass.option1().getName())));
        assertTrue(applicationMetadata.getOptions().stream().anyMatch(o -> o.getName().equals(TestOptionClass.option2().getName())));
        assertTrue(applicationMetadata.getOptions().stream().anyMatch(o -> o.getName().equals("config")));
    }

    @Test
    public void testAddAlwaysOnModuleOptions() {

        BQRuntime runtime = testFactory
                .app(args)
                .module(TestAlwaysOnOptionClass.class)
                .createRuntime();

        ApplicationMetadata applicationMetadata = runtime.getInstance(ApplicationMetadata.class);

        assertEquals(2, applicationMetadata.getOptions().size());
        assertTrue(applicationMetadata.getOptions().stream().anyMatch(o -> o.getName().equals(TestAlwaysOnOptionClass.option1().getName())));
        assertTrue(applicationMetadata.getOptions().stream().anyMatch(o -> o.getName().equals("config")));
    }

    @Test
    public void testAddAllOptions() {

        BQRuntime runtime = testFactory
                .app(args)
                .module(TestAlwaysOnOptionClass.class)
                .module(TestOptionClass.class)
                .createRuntime();

        ApplicationMetadata applicationMetadata = runtime.getInstance(ApplicationMetadata.class);

        assertEquals(4, applicationMetadata.getOptions().size());
        assertTrue(applicationMetadata.getOptions().stream().anyMatch(o -> o.getName().equals(TestAlwaysOnOptionClass.option1().getName())));
        assertTrue(applicationMetadata.getOptions().stream().anyMatch(o -> o.getName().equals(TestOptionClass.option1().getName())));
        assertTrue(applicationMetadata.getOptions().stream().anyMatch(o -> o.getName().equals(TestOptionClass.option2().getName())));
        assertTrue(applicationMetadata.getOptions().stream().anyMatch(o -> o.getName().equals("config")));
    }

    @Test
    public void testRemoveOnlyModuleOptions() {

        BQRuntime runtime = testFactory
                .app(args)
                .module(TestNoOptionOptionClass.class)
                .createRuntime();

        ApplicationMetadata applicationMetadata = runtime.getInstance(ApplicationMetadata.class);
        assertEquals(0, applicationMetadata.getOptions().size());
    }

    @Test
    public void testRemoveAlwaysOnModuleOptions() {

        BQRuntime runtime = testFactory
                .app(args)
                .module(TestNoOptionAlwaysOnOptionClass.class)
                .createRuntime();

        ApplicationMetadata applicationMetadata = runtime.getInstance(ApplicationMetadata.class);
        assertEquals(1, applicationMetadata.getOptions().size());
        assertTrue(applicationMetadata
                .getOptions()
                .stream()
                .anyMatch(o -> o
                        .getName()
                        .equals(TestNoOptionAlwaysOnOptionClass
                                .option1()
                                .getName())));
    }

    @Test
    public void testRemoveAllOptions() {

        BQRuntime runtime = testFactory
                .app(args)
                .module(TestNoOptionAlwaysOnOptionClass.class)
                .module(TestNoOptionOptionClass.class)
                .createRuntime();

        ApplicationMetadata applicationMetadata = runtime.getInstance(ApplicationMetadata.class);
        assertEquals(1, applicationMetadata.getOptions().size());
        assertTrue(applicationMetadata
                .getOptions()
                .stream()
                .anyMatch(o -> o
                        .getName()
                        .equals(TestNoOptionAlwaysOnOptionClass
                                .option1()
                                .getName())));
    }

    @Test
    public void testRemoveAlwaysOnModuleOptionsAndOptions() {

        BQRuntime runtime = testFactory
                .app(args)
                .module(TestNoOptionAlwaysOnOptionClass.class)
                .module(TestOptionClass.class)
                .createRuntime();

        ApplicationMetadata applicationMetadata = runtime.getInstance(ApplicationMetadata.class);
        assertEquals(1, applicationMetadata.getOptions().size());
        assertTrue(applicationMetadata
                .getOptions()
                .stream()
                .anyMatch(o -> o
                        .getName()
                        .equals(TestNoOptionAlwaysOnOptionClass
                                .option1()
                                .getName())));
    }

    @Test(expected = DIRuntimeException.class)
    public void testDuplicateException() {

        testFactory
                .app(args)
                .module(TestOptionClass.class)
                .module(TestOptionClass.class)
                .createRuntime()
                .run();
    }

    static class TestOptionClass implements BQModule {

        public static OptionMetadata option1() {
            return OptionMetadata
                    .builder("option1")
                    .description("option1 description")
                    .build();
        }

        public static OptionMetadata option2() {
            return OptionMetadata
                    .builder("option2")
                    .description("option2 description")
                    .build();
        }

        @Override
        public void configure(Binder binder) {
            BQCoreModule.extend(binder)
                    .addOption(option1())
                    .addOption(option2());
        }
    }

    static class TestNoOptionOptionClass implements BQModule {

        public static OptionMetadata option1() {
            return OptionMetadata
                    .builder("noOption1")
                    .description("no option1 description")
                    .build();
        }

        public static OptionMetadata option2() {
            return OptionMetadata
                    .builder("noOption2")
                    .description("no option2 description")
                    .build();
        }

        @Override
        public void configure(Binder binder) {
            BQCoreModule.extend(binder)
                    .addOption(option1())
                    .addOption(option2())
                    .noModuleOptions();
        }
    }

    static class TestAlwaysOnOptionClass implements BQModule {

        public static OptionMetadata option1() {
            return OptionMetadata
                    .builder("option1AlwaysOn")
                    .description("option1 always on description")
                    .alwaysOn()
                    .build();
        }

        @Override
        public void configure(Binder binder) {
            BQCoreModule.extend(binder)
                    .addOption(option1());
        }
    }

    static class TestNoOptionAlwaysOnOptionClass implements BQModule {

        public static OptionMetadata option1() {
            return OptionMetadata
                    .builder("option1AlwaysOn")
                    .description("option1 always on description")
                    .alwaysOn()
                    .build();
        }

        @Override
        public void configure(Binder binder) {
            BQCoreModule.extend(binder)
                    .addOption(option1())
                    .noModuleOptions();
        }
    }

    static class TestCommandClass extends CommandWithMetadata {

        private static OptionMetadata classOption() {
            return OptionMetadata.builder("classOption")
                    .description("Class option description")
                    .build();
        }

        public TestCommandClass() {
            super(CommandMetadata.builder(TestCommandClass.class).addOption(classOption()).build());
        }

        @Override
        public CommandOutcome run(Cli cli) {
            return CommandOutcome.succeeded();
        }
    }

    static class TestCommandClass2 implements BQModule {

        @Override
        public void configure(Binder binder) {
            BQCoreModule
                    .extend(binder)
                    .addCommand(TestCommandClass.class)
                    .noModuleOptions();
        }
    }

}