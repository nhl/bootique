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

import io.bootique.annotation.DIConfigs;
import io.bootique.annotation.DefaultCommand;
import io.bootique.annotation.EnvironmentProperties;
import io.bootique.annotation.EnvironmentVariables;
import io.bootique.annotation.LogLevels;
import io.bootique.command.Command;
import io.bootique.command.CommandDecorator;
import io.bootique.command.CommandRefDecorated;
import io.bootique.config.OptionRefWithConfig;
import io.bootique.config.OptionRefWithConfigPath;
import io.bootique.di.Binder;
import io.bootique.di.MapBuilder;
import io.bootique.di.SetBuilder;
import io.bootique.di.TypeLiteral;
import io.bootique.env.DeclaredVariable;
import io.bootique.help.ValueObjectDescriptor;
import io.bootique.meta.application.OptionMetadata;

import java.util.Map;
import java.util.logging.Level;

import static java.util.Arrays.asList;

/**
 * Provides API to contribute custom extensions to BQCoreModule.
 * This class is a syntactic sugar for Bootique DI MapBuilder and SetBuilder.
 *
 * @since 0.22
 */
public class BQCoreModuleExtender extends ModuleExtender<BQCoreModuleExtender> {

    private SetBuilder<String> configs;
    private MapBuilder<String, String> properties;
    private MapBuilder<String, String> variables;
    private MapBuilder<String, Level> logLevels;
    private SetBuilder<DeclaredVariable> declaredVariables;
    private SetBuilder<OptionMetadata> options;
    private SetBuilder<Command> commands;
    private SetBuilder<CommandRefDecorated> commandDecorators;
    private SetBuilder<OptionRefWithConfig> optionDecorators;
    private MapBuilder<Class<?>, ValueObjectDescriptor> valueObjectsDescriptors;
    private SetBuilder<OptionRefWithConfigPath> optionPathDecorators;

    protected BQCoreModuleExtender(Binder binder) {
        super(binder);
    }

    @Override
    public BQCoreModuleExtender initAllExtensions() {
        contributeConfigs();
        contributeProperties();
        contributeVariables();
        contributeVariableDeclarations();
        contributeLogLevels();
        contributeOptions();
        contributeCommands();
        contributeCommandDecorators();
        contributeOptionDecorators();
        contributeValueObjectsDescriptors();
        contributeOptionPathDecorators();

        return this;
    }

    public BQCoreModuleExtender noModuleCommands() {
        binder.bind(Boolean.class, BQCoreModule.NO_MODULE_COMMANDS).toInstance(true);
        return this;
    }

    public BQCoreModuleExtender noModuleOptions() {
        binder.bind(Boolean.class, BQCoreModule.NO_MODULE_OPTIONS).toInstance(true);
        return this;
    }

    /**
     * Initializes optional default command that will be executed if no explicit command is found in startup arguments.
     *
     * @param commandType a class of the default command.
     * @return this extender instance.
     */
    public BQCoreModuleExtender setDefaultCommand(Class<? extends Command> commandType) {
        binder.bind(Command.class, DefaultCommand.class).to(commandType).inSingletonScope();
        return this;
    }

    /**
     * Initializes optional default command that will be executed if no explicit command is found in startup arguments.
     *
     * @param command an instance of the default command.
     * @return this extender instance.
     */
    public BQCoreModuleExtender setDefaultCommand(Command command) {
        binder.bind(Command.class, DefaultCommand.class).toInstance(command);
        return this;
    }

    /**
     * Binds an optional application description used in help messages, etc.
     *
     * @param description optional application description used in help messages, etc.
     * @return this extender instance.
     */
    public BQCoreModuleExtender setApplicationDescription(String description) {
        binder.bind(ApplicationDescription.class).toInstance(new ApplicationDescription(description));
        return this;
    }

    public BQCoreModuleExtender setLogLevel(String name, Level level) {
        contributeLogLevels().put(name, level);
        return this;
    }

    public BQCoreModuleExtender setLogLevels(Map<String, Level> levels) {
        levels.forEach(this::setLogLevel);
        return this;
    }

    public BQCoreModuleExtender setProperty(String name, String value) {
        contributeProperties().put(name, value);
        return this;
    }

    public BQCoreModuleExtender setProperties(Map<String, String> properties) {
        properties.forEach(this::setProperty);
        return this;
    }

    public BQCoreModuleExtender setVar(String name, String value) {
        contributeVariables().put(name, value);
        return this;
    }

    public BQCoreModuleExtender setVars(Map<String, String> vars) {
        vars.forEach(this::setVar);
        return this;
    }

    /**
     * Declares a configuration variable for the given config path and given name.
     *
     * @param configPath a dot-separated "path" that navigates through the configuration tree to the property that
     *                   should be bound form a variable. E.g. "jdbc.myds.password".
     * @param name       public name of the variable.
     * @return this extender instance.
     */
    public BQCoreModuleExtender declareVar(String configPath, String name) {
        return declareVar(configPath, name, null);
    }

    /**
     * @param configPath  a dot-separated "path" that navigates through the configuration tree to the property that
     *                    should be bound form a variable. E.g. "jdbc.myds.password".
     * @param name        public name of the variable.
     * @param description the description for variable.
     * @return this extender instance.
     * @since 1.0.RC1
     * Declares a configuration variable for the given config path, given name and given description.
     */
    public BQCoreModuleExtender declareVar(String configPath, String name, String description) {
        DeclaredVariable var = new DeclaredVariable(configPath, name, description);
        contributeVariableDeclarations().add(var);
        return this;
    }

    public BQCoreModuleExtender declareVars(Map<String, String> varsByConfigPaths) {
        varsByConfigPaths.forEach(this::declareVar);
        return this;
    }

    /**
     * Registers a URL of a configuration resource to be loaded by the app unconditionally and prior to any explicitly
     * specified configs. Can be called multiple times for multiple resources.
     *
     * @param configResourceId a resource path compatible with {@link io.bootique.resource.ResourceFactory} denoting
     *                         a configuration source. E.g. "a/b/my.yml", or "classpath:com/foo/another.yml".
     * @return this extender instance.
     * @since 0.25
     */
    public BQCoreModuleExtender addConfig(String configResourceId) {
        contributeConfigs().add(configResourceId);
        return this;
    }

    /**
     * Maps a CLI option to a URL of a configuration resource to be conditionally loaded by the app when
     * that option is used. The config is loaded prior to any configuration potentially loaded via the option.
     * This method can be called multiple times for the same option, mapping multiple config resources.
     * An option with "optionName" must be declared separately via {@link #addOption(OptionMetadata)} or
     * {@link #addOptions(OptionMetadata...)}.
     *
     * @param optionName       the name of the CLI option
     * @param configResourceId a resource path compatible with {@link io.bootique.resource.ResourceFactory} denoting
     *                         a configuration source. E.g. "a/b/my.yml", or "classpath:com/foo/another.yml".
     * @return this extender instance.
     * @since 0.25
     * @since 1.0.RC1 renamed from addConfigOnOption to mapConfigResource
     */
    public BQCoreModuleExtender mapConfigResource(String optionName, String configResourceId) {
        // using Multibinder to support multiple decorators for the same option
        contributeOptionDecorators().add(new OptionRefWithConfig(optionName, configResourceId));
        return this;
    }

    /**
     * Maps a CLI option to a config path. The option runtime value is assigned to the
     * configuration property denoted by the path. An option with "optionName" must be declared separately via
     * {@link #addOption(OptionMetadata)} or {@link #addOptions(OptionMetadata...)}.
     *
     * @param optionName the name of the CLI option
     * @param configPath a dot-separated "path" that navigates configuration tree to the desired property.
     *                   E.g. "jdbc.myds.password".
     * @return this extender instance
     * @since 1.0.RC1
     */
    public BQCoreModuleExtender mapConfigPath(String optionName, String configPath) {
        contributeOptionPathDecorators().add(new OptionRefWithConfigPath(optionName, configPath));
        return this;
    }

    /**
     * Adds a new option to the list of Bootique CLI options.
     *
     * @param option a descriptor of the CLI option to be added to Bootique.
     * @return this extender instance.
     */
    public BQCoreModuleExtender addOption(OptionMetadata option) {
        contributeOptions().add(option);
        return this;
    }

    /**
     * Adds zero or more new options to the list of Bootique CLI options.
     *
     * @param options an array of descriptors of the CLI options to be added to Bootique.
     * @return this extender instance.
     */
    public BQCoreModuleExtender addOptions(OptionMetadata... options) {
        if (options != null) {
            asList(options).forEach(this::addOption);
        }
        return this;
    }

    public BQCoreModuleExtender addCommand(Command command) {
        contributeCommands().add(command);
        return this;
    }

    public BQCoreModuleExtender addCommand(Class<? extends Command> commandType) {
        // TODO: what does singleton scope means when adding to collection?
        contributeCommands().add(commandType).inSingletonScope();
        return this;
    }

    /**
     * Decorates a given command. When that command is invoked, other commands defined in the decorator will be invoked
     * as well.
     *
     * @param commandType      "Raw" command type
     * @param commandDecorator command decorator.
     * @return this extender instance
     * @since 0.25
     */
    public BQCoreModuleExtender decorateCommand(Class<? extends Command> commandType, CommandDecorator commandDecorator) {
        contributeCommandDecorators().add(new CommandRefDecorated(commandType, commandDecorator));
        return this;
    }

    /**
     * Binds help descriptors keyed by the type of configuration value objects. Standard config value objects include
     * classes like {@link io.bootique.value.Bytes}, {@link io.bootique.value.Duration}, etc.
     *
     * @param valueObjectsDescriptors - map of descriptors by described value type
     * @return this extender instance
     * @since 1.0.RC1
     */
    public BQCoreModuleExtender addValueObjectsDescriptors(Map<Class<?>, ValueObjectDescriptor> valueObjectsDescriptors) {
        MapBuilder<Class<?>, ValueObjectDescriptor> binder = contributeValueObjectsDescriptors();
        valueObjectsDescriptors.forEach(binder::put);
        return this;
    }

    /**
     * Binds descriptors with string description to value objects.
     *
     * @param object                 - the value object
     * @param valueObjectsDescriptor - descriptor for value object.
     * @return this extender instance
     * @since 1.0.RC1
     */
    public BQCoreModuleExtender addValueObjectDescriptor(Class<?> object, ValueObjectDescriptor valueObjectsDescriptor) {
        contributeValueObjectsDescriptors().put(object, valueObjectsDescriptor);
        return this;
    }

    protected MapBuilder<Class<?>, ValueObjectDescriptor> contributeValueObjectsDescriptors() {
        return valueObjectsDescriptors != null
                ? valueObjectsDescriptors
                : (valueObjectsDescriptors = newMap(new TypeLiteral<Class<?>>() {
        }, TypeLiteral.of(ValueObjectDescriptor.class)));
    }

    protected MapBuilder<String, Level> contributeLogLevels() {
        return logLevels != null ? logLevels : (logLevels = newMap(String.class, Level.class, LogLevels.class));
    }

    protected SetBuilder<OptionMetadata> contributeOptions() {
        // no synchronization. we don't care if it is created twice. It will still work with DI container.
        return options != null ? options : (options = newSet(OptionMetadata.class));
    }

    protected SetBuilder<DeclaredVariable> contributeVariableDeclarations() {
        // no synchronization. we don't care if it is created twice. It will still work with DI container.
        return declaredVariables != null ? declaredVariables : (declaredVariables = newSet(DeclaredVariable.class));
    }

    protected SetBuilder<Command> contributeCommands() {
        // no synchronization. we don't care if it is created twice. It will still work with DI container.
        return commands != null ? commands : (commands = newSet(Command.class));
    }

    protected SetBuilder<CommandRefDecorated> contributeCommandDecorators() {
        return commandDecorators != null ? commandDecorators : (commandDecorators = newSet(CommandRefDecorated.class));
    }

    protected SetBuilder<OptionRefWithConfig> contributeOptionDecorators() {
        return optionDecorators != null ? optionDecorators : (optionDecorators = newSet(OptionRefWithConfig.class));
    }

    protected SetBuilder<OptionRefWithConfigPath> contributeOptionPathDecorators() {
        return optionPathDecorators != null ? optionPathDecorators : (optionPathDecorators = newSet(OptionRefWithConfigPath.class));
    }

    protected MapBuilder<String, String> contributeProperties() {
        return properties != null ? properties : (properties = newMap(String.class, String.class, EnvironmentProperties.class));
    }

    protected MapBuilder<String, String> contributeVariables() {
        return variables != null ? variables : (variables = newMap(String.class, String.class, EnvironmentVariables.class));
    }

    protected SetBuilder<String> contributeConfigs() {
        return configs != null ? configs : (configs = newSet(String.class, DIConfigs.class));
    }
}
