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

import io.bootique.meta.MetadataNode;

/**
 * A descriptor of a command-line option.
 *
 * @since 0.20
 */
public class OptionMetadata implements MetadataNode {

    private String name;
    private String description;
    private String shortName;
    private OptionValueCardinality valueCardinality;
    private String valueName;
    private String defaultValue;
    private boolean alwaysOn;

    public static Builder builder(String name) {
        return new Builder().name(name);
    }

    public static Builder builder(String name, String description) {
        return new Builder().name(name).description(description);
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
     * @return option short name.
     * @since 0.21
     */
    public String getShortName() {
        return (shortName != null) ? shortName : name.substring(0, 1);
    }

    public OptionValueCardinality getValueCardinality() {
        return valueCardinality;
    }

    public String getValueName() {
        return valueName;
    }

    /**
     * Returns the default value for this option. I.e. the value that will be used if the option is provided on
     * command line without an explicit value.
     *
     * @return the default value for this option.
     * @since 0.24
     */
    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * Returns whether the option should be displays even if the method "noModuleOptions" is applied.
     *
     * @return whether the option should be displays even if the method "noModuleOptions" is applied.
     * @since 2.0
     */
    public boolean isAlwaysOn() {
        return alwaysOn;
    }

    public static class Builder {

        private OptionMetadata option;

        protected Builder() {
            this.option = new OptionMetadata();
            this.option.valueCardinality = OptionValueCardinality.NONE;
        }

        public Builder name(String name) {
            this.option.name = validateName(name);
            return this;
        }

        public Builder shortName(String shortName) {
            option.shortName = validateShortName(shortName);
            return this;
        }

        public Builder shortName(char shortName) {
            option.shortName = String.valueOf(shortName);
            return this;
        }

        public Builder description(String description) {
            this.option.description = description;
            return this;
        }

        public Builder valueRequired() {
            return valueRequired("");
        }

        public Builder valueRequired(String valueName) {
            this.option.valueCardinality = OptionValueCardinality.REQUIRED;
            this.option.valueName = valueName;
            return this;
        }

        public Builder valueOptional() {
            return valueOptional("");
        }

        public Builder valueOptional(String valueName) {
            this.option.valueCardinality = OptionValueCardinality.OPTIONAL;
            this.option.valueName = valueName;
            return this;
        }

        /**
         * Mark option as always displayed
         *
         * @return this builder instance.
         * @since 2.0
         */
        public Builder alwaysOn() {
            this.option.alwaysOn = true;
            return this;
        }


        /**
         * Marks value optional and sets the default value for this option that will be used if the option is provided on
         * command line without an explicit value.
         *
         * @param defaultValue a default value for the option.
         * @return this builder instance
         * @since 1.0.RC1
         */
        public Builder valueOptionalWithDefault(String defaultValue) {
            return valueOptionalWithDefault("", defaultValue);
        }

        /**
         * Marks value optional and sets the default value for this option that will be used if the option is provided on
         * command line without an explicit value.
         *
         * @param valueName a description of value
         * @param defaultValue a default value for the option.
         * @return this builder instance
         * @since 1.0.RC1
         */
        public Builder valueOptionalWithDefault(String valueName, String defaultValue) {
            this.option.defaultValue = defaultValue;
            return valueOptional(valueName);
        }

        public OptionMetadata build() {
            validateName(option.name);
            return option;
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

        private String validateShortName(String shortName) {
            if (shortName == null) {
                throw new IllegalArgumentException("Null 'shortName'");
            }

            if (shortName.length() != 1) {
                throw new IllegalArgumentException("'shortName' must be exactly one char long: " + shortName);
            }

            return shortName;
        }
    }

}
