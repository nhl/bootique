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

import java.util.Objects;

/**
 * A wrapper around the command instance that provides contextual attributes for that command within Bootique.
 *
 * @since 0.25
 */
public class ManagedCommand {

    private boolean hidden;
    private boolean _default;
    private boolean help;

    private Command command;

    protected ManagedCommand() {
    }

    public static Builder builder(Command command) {
        return new Builder(command);
    }

    public static ManagedCommand forCommand(Command command) {
        return builder(command).build();
    }

    public Command getCommand() {
        return command;
    }

    public boolean isHidden() {
        return hidden;
    }

    public boolean isDefault() {
        return _default;
    }

    public boolean isHelp() {
        return help;
    }

    public static class Builder {

        private ManagedCommand managedCommand;

        public Builder(Command command) {
            managedCommand = new ManagedCommand();
            managedCommand.command = Objects.requireNonNull(command);
        }

        public Builder asDefault() {
            managedCommand._default = true;
            return this;
        }

        public Builder asHidden() {
            managedCommand.hidden = true;
            return this;
        }

        public Builder asHelp() {
            managedCommand.help = true;
            return this;
        }

        public ManagedCommand build() {
            return managedCommand;
        }
    }
}
