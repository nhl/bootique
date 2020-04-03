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

import io.bootique.annotation.Args;
import io.bootique.annotation.DIConfigs;
import io.bootique.annotation.DefaultCommand;
import io.bootique.annotation.EnvironmentProperties;
import io.bootique.annotation.EnvironmentVariables;
import io.bootique.cli.Cli;
import io.bootique.cli.CliFactory;
import io.bootique.command.Command;
import io.bootique.command.CommandDecorator;
import io.bootique.command.CommandDispatchThreadFactory;
import io.bootique.command.CommandManager;
import io.bootique.command.CommandManagerBuilder;
//import io.bootique.command.CommandManagerWithOverridesBuilder;
import io.bootique.command.CommandRefDecorated;
import io.bootique.command.ExecutionPlanBuilder;
import io.bootique.config.CliConfigurationSource;
import io.bootique.config.ConfigurationFactory;
import io.bootique.config.ConfigurationSource;
import io.bootique.config.PolymorphicConfiguration;
import io.bootique.config.TypesFactory;
import io.bootique.di.Binder;
import io.bootique.di.Injector;
import io.bootique.di.Key;
import io.bootique.di.BQModule;
import io.bootique.di.Provides;
import io.bootique.env.DeclaredVariable;
import io.bootique.env.DefaultEnvironment;
import io.bootique.env.Environment;
import io.bootique.help.DefaultHelpGenerator;
import io.bootique.help.HelpCommand;
import io.bootique.help.HelpGenerator;
import io.bootique.help.ValueObjectDescriptor;
import io.bootique.help.config.ConfigHelpGenerator;
import io.bootique.help.config.DefaultConfigHelpGenerator;
import io.bootique.help.config.HelpConfigCommand;
import io.bootique.jackson.DefaultJacksonService;
import io.bootique.jackson.JacksonService;
import io.bootique.jopt.JoptCliFactory;
import io.bootique.log.BootLogger;
import io.bootique.meta.application.ApplicationMetadata;
import io.bootique.meta.application.OptionMetadata;
import io.bootique.meta.config.ConfigHierarchyResolver;
import io.bootique.meta.config.ConfigMetadataCompiler;
import io.bootique.meta.module.ModulesMetadata;
import io.bootique.meta.module.ModulesMetadataCompiler;
import io.bootique.run.DefaultRunner;
import io.bootique.run.Runner;
import io.bootique.shutdown.ShutdownManager;
import io.bootique.terminal.FixedWidthTerminal;
import io.bootique.terminal.SttyTerminal;
import io.bootique.terminal.Terminal;
import io.bootique.value.Bytes;
import io.bootique.value.Duration;
import io.bootique.value.Percent;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

/**
 * The main {@link BQModule} of Bootique DI runtime. Declares a minimal set of
 * services needed for a Bootique app to start: services for parsing command
 * line, reading configuration, selectign and running a Command.
 */
public class BQCoreModule implements BQModule {

    // TODO: duplicate of FormattedAppender.MIN_LINE_WIDTH
    private static final int TTY_MIN_COLUMNS = 40;
    private static final int TTY_DEFAULT_COLUMNS = 80;
    /**
     * Properties are used to exclude system env vars and properties.
     * It's a duplicate of constants in io.bootique.test.junit.BQTestRuntimeBuilder
     */
    private static final String EXCLUDE_SYSTEM_VARIABLES = "bq.core.excludeSystemVariables";
    private static final String EXCLUDE_SYSTEM_PROPERTIES = "bq.core.excludeSystemProperties";
    private String[] args;
    private ShutdownManager shutdownManager;
    private BootLogger bootLogger;
    private Supplier<Collection<BQModuleMetadata>> modulesSource;

    static final String NO_MODULE_COMMANDS = "no_module_commands";
    static final String NO_MODULE_OPTIONS = "no_module_options";

    private BQCoreModule() {
    }

    /**
     * @return a Builder instance to configure the module before using it to
     * initialize DI container.
     * @since 0.12
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns an instance of {@link BQCoreModuleExtender} used by downstream modules to load custom extensions to the
     * Bootique core module. Should be invoked from a downstream Module's "configure" method.
     *
     * @param binder DI binder passed to the Module that invokes this method.
     * @return an instance of {@link BQCoreModuleExtender} that can be used to load custom extensions to the Bootique
     * core.
     * @since 0.22
     */
    public static BQCoreModuleExtender extend(Binder binder) {
        return new BQCoreModuleExtender(binder);
    }

    private static Optional<Command> defaultCommand(Injector injector) {
        // default is optional, so check via injector whether it is bound...
        Key<Command> key = Key.get(Command.class, DefaultCommand.class);
        if (injector.hasProvider(key)) {
            Provider<Command> commandProvider = injector.getProvider(key);
            return Optional.of(commandProvider.get());
        }
        return Optional.empty();
    }

    @Override
    public void configure(Binder binder) {

        BQCoreModule.extend(binder)
                .initAllExtensions()
                .addValueObjectsDescriptors(createValueObjectsDescriptorsMap())
                .addOption(createConfigOption())
                .addCommand(HelpConfigCommand.class);

        // bind instances
        binder.bind(BootLogger.class).toInstance(Objects.requireNonNull(bootLogger));
        binder.bind(ShutdownManager.class).toInstance(Objects.requireNonNull(shutdownManager));
        binder.bind(String[].class, Args.class).toInstance(Objects.requireNonNull(args));

        // too much code to create config factory.. extracting it in a provider
        // class...
        binder.bind(ConfigurationFactory.class).toProvider(JsonNodeConfigurationFactoryProvider.class).inSingletonScope();

        binder.bindOptional(Key.get(Boolean.class, NO_MODULE_COMMANDS));
        binder.bindOptional(Key.get(Boolean.class, NO_MODULE_OPTIONS));
    }

    OptionMetadata createConfigOption() {
        return OptionMetadata
                .builder(CliConfigurationSource.CONFIG_OPTION,
                        "Specifies YAML config location, which can be a file path or a URL.")
                .valueRequired("yaml_location").build();
    }

    @Provides
    @Singleton
    JacksonService provideJacksonService(TypesFactory<PolymorphicConfiguration> typesFactory) {
        return new DefaultJacksonService(typesFactory.getTypes());
    }

    @Provides
    @Singleton
    TypesFactory<PolymorphicConfiguration> provideConfigTypesFactory(BootLogger logger) {
        return new TypesFactory<>(getClass().getClassLoader(), PolymorphicConfiguration.class, logger);
    }

    @Provides
    @Singleton
    Runner provideRunner(Cli cli, CommandManager commandManager, ExecutionPlanBuilder execPlanBuilder) {
        return new DefaultRunner(cli, commandManager, execPlanBuilder);
    }

    @Provides
    @Singleton
    ConfigurationSource provideConfigurationSource(Cli cli, @DIConfigs Set<String> diConfigs, BootLogger bootLogger) {
        return CliConfigurationSource
                .builder(bootLogger)
                .diConfigs(diConfigs)
                .cliConfigs(cli)
                .build();
    }

    @Provides
    @Singleton
    HelpCommand provideHelpCommand(BootLogger bootLogger, Provider<HelpGenerator> helpGeneratorProvider) {
        return new HelpCommand(bootLogger, helpGeneratorProvider);
    }

    @Provides
    @Singleton
    HelpConfigCommand provideHelpConfigCommand(BootLogger bootLogger, Provider<ConfigHelpGenerator> helpGeneratorProvider) {
        return new HelpConfigCommand(bootLogger, helpGeneratorProvider);
    }

    @Provides
    @Singleton
    CliFactory provideCliFactory(
            Provider<CommandManager> commandManagerProvider,
            ApplicationMetadata applicationMetadata) {
        return new JoptCliFactory(commandManagerProvider, applicationMetadata);
    }

    @Provides
    @Singleton
    Cli provideCli(CliFactory cliFactory, @Args String[] args) {
        return cliFactory.createCli(args);
    }

    @Provides
    @Singleton
    ExecutionPlanBuilder provideExecutionPlanBuilder(
            Provider<CliFactory> cliFactoryProvider,
            Provider<CommandManager> commandManagerProvider,
            Set<CommandRefDecorated> commandDecorators,
            BootLogger logger,
            ShutdownManager shutdownManager) {

        Provider<ExecutorService> executorProvider = () -> {
            ExecutorService service = Executors.newCachedThreadPool(new CommandDispatchThreadFactory());
            shutdownManager.addShutdownHook(service::shutdownNow);
            return service;
        };

        Map<Class<? extends Command>, CommandDecorator> merged = ExecutionPlanBuilder.mergeDecorators(commandDecorators);
        return new ExecutionPlanBuilder(cliFactoryProvider, commandManagerProvider, executorProvider, merged, logger);
    }

    @Provides
    @Singleton
    CommandManager provideCommandManager(
            Set<Command> commands,
            HelpCommand helpCommand,
            Injector injector,
            @Named(NO_MODULE_COMMANDS) Optional<Boolean> noModuleCommandsOptional) {

        boolean noModuleCommands = noModuleCommandsOptional.orElse(false);

        return new CommandManagerBuilder<>(commands)
                .defaultCommand(defaultCommand(injector))
                .hideModuleCommands(noModuleCommands)
                .helpCommand(helpCommand)
                .build();
    }

    @Provides
    @Singleton
    HelpGenerator provideHelpGenerator(
            ApplicationMetadata application,
            Terminal terminal,
            @Named(NO_MODULE_COMMANDS) Optional<Boolean> noModuleCommandsOptional) {

        boolean noModuleCommands = noModuleCommandsOptional.orElse(false);

        int maxColumns = terminal.getColumns();
        if (maxColumns < TTY_MIN_COLUMNS) {
            maxColumns = TTY_DEFAULT_COLUMNS;
        }

        return new DefaultHelpGenerator(application, maxColumns, noModuleCommands);
    }

    @Provides
    @Singleton
    ConfigHelpGenerator provideConfigHelpGenerator(ModulesMetadata modulesMetadata, Terminal terminal) {

        int maxColumns = terminal.getColumns();
        if (maxColumns < TTY_MIN_COLUMNS) {
            maxColumns = TTY_DEFAULT_COLUMNS;
        }

        return new DefaultConfigHelpGenerator(modulesMetadata, maxColumns);
    }

    @Provides
    @Singleton
    ConfigHierarchyResolver provideConfigHierarchyResolver(TypesFactory<PolymorphicConfiguration> typesFactory) {
        return ConfigHierarchyResolver.create(typesFactory.getTypes());
    }

    @Provides
    @Singleton
    ModulesMetadata provideModulesMetadata(ConfigHierarchyResolver hierarchyResolver, Map<Class<?>,
            ValueObjectDescriptor> valueObjectDescriptors) {

        ConfigMetadataCompiler configCompiler =
                new ConfigMetadataCompiler(hierarchyResolver::directSubclasses, valueObjectDescriptors);
        Collection<BQModuleMetadata> modules = this.modulesSource != null
                ? modulesSource.get()
                : Collections.emptyList();
        return new ModulesMetadataCompiler(configCompiler).compile(modules);
    }

    @Provides
    @Singleton
    ApplicationMetadata provideApplicationMetadata(
            ApplicationDescription descriptionHolder,
            CommandManager commandManager,
            Set<OptionMetadata> options,
            Set<DeclaredVariable> declaredVars,
            ModulesMetadata modulesMetadata,
            @Named(NO_MODULE_OPTIONS) Optional<Boolean> noModuleOptionsOptional) {

        boolean noModuleOptions = noModuleOptionsOptional.orElse(false);

        ApplicationMetadata.Builder builder = ApplicationMetadata
                .builder()
                .description(descriptionHolder.getDescription());

        if (noModuleOptions) {
            builder.addOptions(options.stream().filter(OptionMetadata::isAlwaysOn).collect(Collectors.toSet()));
        } else {
            builder.addOptions(options);
        }

        // merge default command options with top-level app options
        commandManager.getPublicDefaultCommand().ifPresent(c -> builder.addOptions(c.getMetadata().getOptions()));

        commandManager.getAllCommands().values().forEach(mc -> {
            if (!mc.isDefault()) {
                builder.addCommand(mc.getCommand().getMetadata());
            }
        });

        declaredVars.forEach(dv -> DeclaredVariableMetaCompiler
                .compileIfValid(dv, modulesMetadata)
                .ifPresent(builder::addVariable));

        return builder.build();
    }

    @Provides
    @Singleton
    Environment provideEnvironment(
            @EnvironmentProperties Map<String, String> diProperties,
            @EnvironmentVariables Map<String, String> diVars,
            Set<DeclaredVariable> declaredVariables) {

        DefaultEnvironment.Builder environment = DefaultEnvironment.builder();

        if (Boolean.valueOf(diProperties.get(EXCLUDE_SYSTEM_PROPERTIES))) {
            environment.excludeSystemProperties();
        }
        if (Boolean.valueOf(diProperties.containsKey(EXCLUDE_SYSTEM_VARIABLES))) {
            environment.excludeSystemVariables();
        }

        return environment.diProperties(diProperties)
                .diVariables(diVars)
                .declaredVariables(declaredVariables)
                .build();
    }

    @Provides
    @Singleton
    Terminal provideTerminal(BootLogger bootLogger) {

        // very simple OS test...
        boolean isUnix = "/".equals(System.getProperty("file.separator"));
        return isUnix ? new SttyTerminal(bootLogger) : new FixedWidthTerminal(TTY_DEFAULT_COLUMNS);
    }

    private Map<Class<?>, ValueObjectDescriptor> createValueObjectsDescriptorsMap() {
        Map<Class<?>, ValueObjectDescriptor> descriptors = new HashMap<>();
        descriptors.put(Bytes.class, new ValueObjectDescriptor("bytes expression, e.g. 5b, 23MB, 12gigabytes"));
        descriptors.put(Duration.class, new ValueObjectDescriptor("duration expression, e.g. 5ms, 2s, 1hr"));
        descriptors.put(Percent.class, new ValueObjectDescriptor("percent expression, e.g. 15%, 75%"));

        return descriptors;
    }

    public static class Builder {
        private BQCoreModule module;

        private Builder() {
            this.module = new BQCoreModule();
        }

        public BQCoreModule build() {
            return module;
        }

        public Builder bootLogger(BootLogger bootLogger) {
            module.bootLogger = bootLogger;
            return this;
        }

        public Builder shutdownManager(ShutdownManager shutdownManager) {
            module.shutdownManager = shutdownManager;
            return this;
        }

        /**
         * Sets a supplier of the app modules collection. It has to be provided externally by Bootique code that
         * assembles the stack. We have no way of discovering this information when inside the DI container.
         *
         * @param modulesSource a supplier of module collection.
         * @return this builder instance.
         * @since 0.21
         */
        public Builder moduleSource(Supplier<Collection<BQModuleMetadata>> modulesSource) {
            module.modulesSource = modulesSource;
            return this;
        }

        public Builder args(String[] args) {
            module.args = args;
            return this;
        }
    }

}
