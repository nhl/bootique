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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.bootique.cli.Cli;
import io.bootique.config.ConfigurationFactory;
import io.bootique.config.ConfigurationSource;
import io.bootique.config.OptionRefWithConfig;
import io.bootique.config.OptionRefWithConfigPath;
import io.bootique.config.jackson.InPlaceLeftHandMerger;
import io.bootique.config.jackson.InPlaceMapOverrider;
import io.bootique.config.jackson.InPlaceResourceOverrider;
import io.bootique.config.jackson.JsonNodeConfigurationBuilder;
import io.bootique.config.jackson.JsonNodeConfigurationFactory;
import io.bootique.config.jackson.JsonNodeJsonParser;
import io.bootique.config.jackson.JsonNodeYamlParser;
import io.bootique.config.jackson.MultiFormatJsonNodeParser;
import io.bootique.config.jackson.MultiFormatJsonNodeParser.ParserType;
import io.bootique.env.Environment;
import io.bootique.jackson.JacksonService;
import io.bootique.log.BootLogger;
import io.bootique.meta.application.ApplicationMetadata;
import io.bootique.meta.application.OptionMetadata;
import joptsimple.OptionSpec;

import java.io.InputStream;
import java.net.URL;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.Function;

import javax.inject.Inject;
import javax.inject.Provider;

import static java.util.Collections.singletonMap;
import static java.util.function.Function.identity;

/**
 * @since 0.17
 */
public class JsonNodeConfigurationFactoryProvider implements Provider<ConfigurationFactory> {

    private ConfigurationSource configurationSource;
    private Environment environment;
    private JacksonService jacksonService;
    private BootLogger bootLogger;
    private Set<OptionMetadata> optionMetadata;
    private Set<OptionRefWithConfig> optionDecorators;
    private Set<OptionRefWithConfigPath> optionPathDecorators;
    private Cli cli;

    @Inject
    public JsonNodeConfigurationFactoryProvider(
            ConfigurationSource configurationSource,
            Environment environment,
            JacksonService jacksonService,
            BootLogger bootLogger,
            ApplicationMetadata applicationMetadata,
            Set<OptionRefWithConfig> optionDecorators,
            Set<OptionRefWithConfigPath> optionPathDecorators,
            Cli cli) {

        this.configurationSource = configurationSource;
        this.environment = environment;
        this.jacksonService = jacksonService;
        this.bootLogger = bootLogger;
        this.optionMetadata = new HashSet<>(applicationMetadata.getOptions());
        this.optionDecorators = optionDecorators;
        this.optionPathDecorators = optionPathDecorators;
        this.cli = cli;
    }

    protected JsonNode loadConfiguration(Map<String, String> properties) {

        // hopefully sharing the mapper between parsers is safe... Does it
        // change the state during parse?
        ObjectMapper textToJsonMapper = jacksonService.newObjectMapper();
        Map<ParserType, Function<InputStream, Optional<JsonNode>>> parsers = new EnumMap<>(ParserType.class);
        parsers.put(ParserType.YAML, new JsonNodeYamlParser(textToJsonMapper));
        parsers.put(ParserType.JSON, new JsonNodeJsonParser(textToJsonMapper));

        Function<URL, Optional<JsonNode>> parser = new MultiFormatJsonNodeParser(parsers, bootLogger);

        BinaryOperator<JsonNode> singleConfigMerger = new InPlaceLeftHandMerger(bootLogger);

        Function<JsonNode, JsonNode> overrider = andCliOptionOverrider(identity(), parser, singleConfigMerger);

        if (!properties.isEmpty()) {
            overrider = overrider.andThen(new InPlaceMapOverrider(properties));
        }

        return JsonNodeConfigurationBuilder.builder()
                .parser(parser)
                .merger(singleConfigMerger)
                .resources(configurationSource)
                .overrider(overrider)
                .build();
    }

    private Function<JsonNode, JsonNode> andCliOptionOverrider(
            Function<JsonNode, JsonNode> overrider,
            Function<URL, Optional<JsonNode>> parser,
            BinaryOperator<JsonNode> singleConfigMerger) {

        if (optionMetadata.isEmpty()) {
            return overrider;
        }

        List<OptionSpec<?>> detectedOptions = cli.detectedOptions();
        if (detectedOptions.isEmpty()) {
            return overrider;
        }

        for (OptionSpec<?> cliOpt : detectedOptions) {

            OptionMetadata omd = findMetadata(cliOpt);

            if (omd == null) {
                continue;
            }

            // config decorators are loaded first, and then can be overridden from options...
            for (OptionRefWithConfig decorator : optionDecorators) {
                if (decorator.getOptionName().equals(omd.getName())) {
                    overrider = overrider.andThen(new InPlaceResourceOverrider(decorator.getConfigResource().getUrl(),
                            parser, singleConfigMerger));
                }
            }

            for (OptionRefWithConfigPath pathDecorator : optionPathDecorators) {
                if (pathDecorator.getOptionName().equals(omd.getName())) {
                    String cliValue = cli.optionString(omd.getName());
                    overrider = overrider.andThen(new InPlaceMapOverrider(
                            singletonMap(pathDecorator.getConfigPath(), cliValue)
                    ));
                }
            }
        }

        return overrider;
    }

    private OptionMetadata findMetadata(OptionSpec<?> option) {

        List<String> optionNames = option.options();

        // TODO: allow lookup of option metadata by name to avoid linear scans...
        // Though we are dealing with small collection, so shouldn't be too horrible.

        for (OptionMetadata omd : optionMetadata) {
            if (optionNames.contains(omd.getName())) {
                return omd;
            }
        }

        // this was likely a command, not an option.
        return null;
    }

    @Override
    public ConfigurationFactory get() {

        Map<String, String> properties = environment.frameworkProperties();

        JsonNode rootNode = loadConfiguration(properties);

        bootLogger.trace(() -> "Merged configuration: " + rootNode.toString());

        ObjectMapper jsonToObjectMapper = jacksonService.newObjectMapper();
        return new JsonNodeConfigurationFactory(rootNode, jsonToObjectMapper);
    }
}
