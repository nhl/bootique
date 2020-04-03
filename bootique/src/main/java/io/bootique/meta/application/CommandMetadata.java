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

package io.bootique.meta.application;

import io.bootique.command.Command;
import io.bootique.meta.MetadataNode;
import io.bootique.names.ClassToName;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @since 0.20
 */
public class CommandMetadata implements MetadataNode {

    private String name;
    private String description;
    private String shortName;
    private boolean alwaysOn;
    private Collection<OptionMetadata> options;

    public CommandMetadata() {
        this.options = new ArrayList<>();
    }

    public static Builder builder(Class<? extends Command> commandType) {
        return new Builder().commandType(commandType);
    }

    public static Builder builder(String commandName) {
        return new Builder().name(commandName);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    /**
     * Returns an option representation of this command, that may be used in help generation or exposing the command
     * in a CLI parser.
     *
     * @return option representation of this command.
     * @since 0.21
     */
    public OptionMetadata asOption() {
        // TODO: cache the value?
        // using getters instead of vars ; some getters have logic
        return OptionMetadata.builder(getName()).shortName(getShortName()).description(getDescription()).build();
    }

    public Collection<OptionMetadata> getOptions() {
        return options;
    }

    /**
     * Returns the short name
     *
     * @return command short name.
     * @since 0.21
     */
    public String getShortName() {
        return (shortName != null) ? shortName : name.substring(0, 1);
    }

    /**
     * Returns whether the command should be displays even if the method "noModuleCommands" is applied.
     *
     * @return whether the command should be displays even if the method "noModuleCommands" is applied.
     * @since 2.0
     */
    public boolean isAlwaysOn() {
        return alwaysOn;
    }

    public static class Builder {

        private static ClassToName NAME_BUILDER = ClassToName
                .builder()
                .convertToLowerCase()
                .partsSeparator("-")
                .stripSuffix("Command")
                .build();

        private CommandMetadata command;

        private Builder() {
            this.command = new CommandMetadata();
        }

        public CommandMetadata build() {
            validateName(command.name);
            return command;
        }

        public Builder commandType(Class<? extends Command> commandType) {
            command.name = NAME_BUILDER.toName(commandType);
            return this;
        }

        public Builder name(String name) {
            command.name = validateName(name);
            return this;
        }

        public Builder shortName(char shortName) {
            command.shortName = String.valueOf(shortName);
            return this;
        }

        public Builder description(String description) {
            this.command.description = description;
            return this;
        }

        public Builder addOption(OptionMetadata option) {
            this.command.options.add(option);
            return this;
        }

        public Builder addOptions(Collection<OptionMetadata> options) {
            this.command.options.addAll(options);
            return this;
        }

        public Builder addOption(OptionMetadata.Builder optionBuilder) {
            return addOption(optionBuilder.build());
        }

        /**
         * @return this builder instance.
         * @since 2.0
         */
        public Builder alwaysOn() {
            this.command.alwaysOn = true;
            return this;
        }

        private String validateName(String name) {
            if (name == null) {
                throw new IllegalArgumentException("Null 'name'");
            }

            if (name.length() == 0) {
                throw new IllegalArgumentException("Empty 'name'");
            }

            return name;
        }
    }
}
