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

package io.bootique.help;

import io.bootique.meta.application.ApplicationMetadata;
import io.bootique.meta.application.CommandMetadata;
import io.bootique.meta.application.OptionMetadata;
import io.bootique.meta.config.ConfigValueMetadata;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DefaultHelpGeneratorTest {

    private static void assertLines(DefaultHelpGenerator generator, String... expectedLines) {

        StringBuilder expected = new StringBuilder();
        for (String s : expectedLines) {
            expected.append(s).append(ConsoleAppender.NEWLINE);
        }

        String help = generator.generate();
        assertNotNull(help);
        assertEquals(expected.toString(), help);
    }

    @Test
    public void testGenerate_Name() {

        ApplicationMetadata app = ApplicationMetadata.builder("myapp").build();

        assertLines(new DefaultHelpGenerator(app, 80, false),
                "NAME",
                "      myapp"
        );
    }

    @Test
    public void testGenerate_Name_Description() {

        ApplicationMetadata app = ApplicationMetadata.builder("myapp", "this is my app").build();

        assertLines(new DefaultHelpGenerator(app, 80, false),
                "NAME",
                "      myapp: this is my app"
        );
    }

    @Test
    public void testGenerate_Name_Options() {

        OptionMetadata listOpt = OptionMetadata.builder("list", "Lists everything").build();
        OptionMetadata runOpt = OptionMetadata.builder("run", "Runs specified command").valueRequired().build();
        OptionMetadata debugOpt = OptionMetadata.builder("debug", "Switches to debug mode").valueOptional("level").build();

        ApplicationMetadata app = ApplicationMetadata
                .builder("myapp")
                .addOption(listOpt)
                .addOption(runOpt)
                .addOption(debugOpt)
                .build();

        assertLines(new DefaultHelpGenerator(app, 80, false),
                "NAME",
                "      myapp",
                "",
                "OPTIONS",
                "      -d [level], --debug[=level]",
                "           Switches to debug mode",
                "",
                "      -l, --list",
                "           Lists everything",
                "",
                "      -r val, --run=val",
                "           Runs specified command"
        );
    }

    @Test
    public void testGenerate_Name_ShortOptions() {

        OptionMetadata listOpt = OptionMetadata.builder("l", "Lists everything").build();
        OptionMetadata runOpt = OptionMetadata.builder("r", "Runs specified command").valueRequired().build();
        OptionMetadata debugOpt = OptionMetadata.builder("debug", "Switches to debug mode").valueOptional("level").build();

        ApplicationMetadata app = ApplicationMetadata
                .builder("myapp")
                .addOption(listOpt)
                .addOption(runOpt)
                .addOption(debugOpt)
                .build();

        assertLines(new DefaultHelpGenerator(app, 80, false),
                "NAME",
                "      myapp",
                "",
                "OPTIONS",
                "      -d [level], --debug[=level]",
                "           Switches to debug mode",
                "",
                "      -l",
                "           Lists everything",
                "",
                "      -r val",
                "           Runs specified command"
        );
    }

    @Test
    public void testGenerate_Name_Options_ShortNameConflict() {

        OptionMetadata resetOpt = OptionMetadata.builder("reset", "Resets everything").build();
        OptionMetadata listOpt = OptionMetadata.builder("list", "Lists everything").build();
        OptionMetadata runOpt = OptionMetadata.builder("run", "Runs everything").build();
        ApplicationMetadata app = ApplicationMetadata
                .builder("myapp")
                .addOption(resetOpt)
                .addOption(listOpt)
                .addOption(runOpt)
                .build();

        assertLines(new DefaultHelpGenerator(app, 80, false),
                "NAME",
                "      myapp",
                "",
                "OPTIONS",
                "      -l, --list",
                "           Lists everything",
                "",
                "      --reset",
                "           Resets everything",
                "",
                "      --run",
                "           Runs everything"
        );
    }

    @Test
    public void testGenerate_Name_Options_ShortAliases() {

        OptionMetadata resetOpt = OptionMetadata.builder("reset", "Resets everything").build();
        OptionMetadata listOpt = OptionMetadata.builder("list", "Lists everything").build();
        OptionMetadata runOpt = OptionMetadata.builder("reset-for-real", "Resets everything and then does it again")
                .shortName('R').build();
        ApplicationMetadata app = ApplicationMetadata
                .builder("myapp")
                .addOption(resetOpt)
                .addOption(listOpt)
                .addOption(runOpt)
                .build();

        assertLines(new DefaultHelpGenerator(app, 80, false),
                "NAME",
                "      myapp",
                "",
                "OPTIONS",
                "      -l, --list",
                "           Lists everything",
                "",
                "      -r, --reset",
                "           Resets everything",
                "",
                "      -R, --reset-for-real",
                "           Resets everything and then does it again"
        );
    }

    @Test
    public void testGenerate_Name_Command_ShortAliases() {

        CommandMetadata resetOpt = CommandMetadata.builder("reset").description("Resets everything").build();
        CommandMetadata runOpt = CommandMetadata.builder("reset-for-real")
                .description("Resets everything and then does it again")
                .shortName('R').build();
        ApplicationMetadata app = ApplicationMetadata
                .builder("myapp")
                .addCommand(resetOpt)
                .addCommand(runOpt)
                .build();

        assertLines(new DefaultHelpGenerator(app, 80, false),
                "NAME",
                "      myapp",
                "",
                "OPTIONS",
                "      -r, --reset",
                "           Resets everything",
                "",
                "      -R, --reset-for-real",
                "           Resets everything and then does it again"
        );
    }

    @Test
    public void testGenerate_TrimSpace() {

        OptionMetadata opt1 = OptionMetadata.builder("a1", "  Word1 word2 \t\n").build();

        ApplicationMetadata app = ApplicationMetadata
                .builder("myapp")
                .addOption(opt1)
                .build();

        assertLines(new DefaultHelpGenerator(app, 80, false),
                "NAME",
                "      myapp",
                "",
                "OPTIONS",
                "      -a, --a1",
                "           Word1 word2"
        );
    }

    @Test
    public void testGenerate_LineFolding() {

        OptionMetadata opt1 = OptionMetadata.builder("a1", "Word1 word2 word3 word4 longword5 longerword6 morewords7").build();
        OptionMetadata opt2 = OptionMetadata.builder("b1", "Word8 word9 word10 word11 longword12 longerword13 morewords14").build();
        OptionMetadata opt3 = OptionMetadata.builder("c1", "Word15, word16").build();

        ApplicationMetadata app = ApplicationMetadata
                .builder("myapp")
                .addOption(opt1)
                .addOption(opt2)
                .addOption(opt3)
                .build();

        assertLines(new DefaultHelpGenerator(app, 41, false),
                "NAME",
                "      myapp",
                "",
                "OPTIONS",
                "      -a, --a1",
                "           Word1 word2 word3 word4",
                "           longword5 longerword6",
                "           morewords7",
                "",
                "      -b, --b1",
                "           Word8 word9 word10 word11",
                "           longword12 longerword13",
                "           morewords14",
                "",
                "      -c, --c1",
                "           Word15, word16"
        );
    }

    @Test
    public void testGenerate_LineFolding_VeryLongWord() {

        OptionMetadata opt1 = OptionMetadata.builder("a1", "Word1_word2_word3_word4_longword5_longerword6_morewords7").build();
        OptionMetadata opt2 = OptionMetadata.builder("b1", "Word1 word2_word3_word4_longword5_longerword6_morewords7").build();
        OptionMetadata opt3 = OptionMetadata.builder("c1", "Word1_word2_word3_word4_longword5_longerword6 morewords7").build();

        ApplicationMetadata app = ApplicationMetadata
                .builder("myapp")
                .addOption(opt1)
                .addOption(opt2)
                .addOption(opt3)
                .build();

        // must insert a forced break...
        assertLines(new DefaultHelpGenerator(app, 41, false),
                "NAME",
                "      myapp",
                "",
                "OPTIONS",
                "      -a, --a1",
                "           Word1_word2_word3_word4_longwo",
                "           rd5_longerword6_morewords7",
                "",
                "      -b, --b1",
                "           Word1 word2_word3_word4_longwo",
                "           rd5_longerword6_morewords7",
                "",
                "      -c, --c1",
                "           Word1_word2_word3_word4_longwo",
                "           rd5_longerword6 morewords7"
        );
    }

    @Test
    public void testGenerate_LineFolding_BigDisplay() {

        OptionMetadata opt1 = OptionMetadata.builder("a1", "Word1_word2_word3_word4_longword5_longerword6_morewords7").build();
        OptionMetadata opt2 = OptionMetadata.builder("b1", "Word1 word2_word3_word4_longword5_longerword6_morewords7").build();
        OptionMetadata opt3 = OptionMetadata.builder("c1", "Word1_word2_word3_word4_longword5_longerword6 morewords7").build();

        ApplicationMetadata app = ApplicationMetadata
                .builder("myapp")
                .addOption(opt1)
                .addOption(opt2)
                .addOption(opt3)
                .build();

        // must insert a forced break...
        assertLines(new DefaultHelpGenerator(app, 80, false),
                "NAME",
                "      myapp",
                "",
                "OPTIONS",
                "      -a, --a1",
                "           Word1_word2_word3_word4_longword5_longerword6_morewords7",
                "",
                "      -b, --b1",
                "           Word1 word2_word3_word4_longword5_longerword6_morewords7",
                "",
                "      -c, --c1",
                "           Word1_word2_word3_word4_longword5_longerword6 morewords7"
        );
    }

    @Test
    public void testGenerate_EnvironmentSection() {

        ConfigValueMetadata var1 = ConfigValueMetadata.builder("VAR1").description("var1 description").build();
        ConfigValueMetadata var2 = ConfigValueMetadata.builder("VAR2").description("var2 description.").build();


        ApplicationMetadata app = ApplicationMetadata
                .builder("myapp")
                .addVariable(var2)
                .addVariable(var1)
                .build();

        // must insert a forced break...
        assertLines(new DefaultHelpGenerator(app, 80, false),
                "NAME",
                "      myapp",
                "",
                "ENVIRONMENT",
                "      VAR1",
                "           var1 description",
                "",
                "      VAR2",
                "           var2 description."
        );
    }
}
