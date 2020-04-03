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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.bootique.config.ConfigurationFactory;
import io.bootique.config.ConfigurationSource;
import io.bootique.config.jackson.JsonNodeConfigurationFactory;
import io.bootique.env.Environment;
import io.bootique.jackson.JacksonService;
import io.bootique.log.DefaultBootLogger;
import io.bootique.meta.application.ApplicationMetadata;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JsonNodeConfigurationFactoryProviderTest {

    private static ConfigurationFactory factory(String... yaml) {
        return factory(Collections.emptyMap(), yaml);
    }

    private static ConfigurationFactory factory(Map<String, String> propertyOverrides, String... yaml) {
        ConfigurationSource mockSource = mock(ConfigurationSource.class);

        when(mockSource.get()).thenReturn(asList(yaml).stream().map(s -> {

            // need to store YAML on disk to have a URL for it

            try {
                Path dir = Paths.get("target", "junit", "tmp");
                Files.createDirectories(dir);
                Path tmp = Files.createTempFile(dir, "BQConfigurationFactoryProviderTest", ".yml");
                Files.write(tmp, s.getBytes());
                return tmp.toUri().toURL();

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }));

        Environment mockEnvironment = mock(Environment.class);
        when(mockEnvironment.frameworkProperties()).thenReturn(propertyOverrides);

        return factory(mockSource, mockEnvironment);
    }

    private static ConfigurationFactory factory(ConfigurationSource mockSource, Environment mockEnvironment) {

        ApplicationMetadata app = ApplicationMetadata.builder("myapp", "this is my app").build();

        JacksonService mockJackson = mock(JacksonService.class);
        when(mockJackson.newObjectMapper()).thenReturn(new ObjectMapper());

        ConfigurationFactory factory = new JsonNodeConfigurationFactoryProvider(mockSource, mockEnvironment, mockJackson,
                new DefaultBootLogger(true), app , Collections.emptySet(), Collections.emptySet(), null).get();

        assertNotNull(factory);
        assertTrue(factory instanceof JsonNodeConfigurationFactory);

        return factory;
    }

    @Test
    public void testGet_SingleConfig() {

        Bean1 b1 = factory("s: SS\ni: 55").config(Bean1.class, "");
        assertNotNull(b1);
        assertEquals("SS", b1.s);
        assertEquals(55, b1.i);
    }

    @Test
    public void testGet_MultiConfig() {

        Bean1 b1 = factory("s: SS\ni: 55", "l: 12345\ni: 56").config(Bean1.class, "");
        assertNotNull(b1);
        assertEquals("SS", b1.s);
        assertEquals(56, b1.i);
        assertEquals(12345l, b1.l);
    }

    @Test
    public void testGet_SingleConfig_PropOverride() {

        Map<String, String> overrides = new HashMap<String, String>() {
            private static final long serialVersionUID = 1L;

            {
                put("s", "SS");
                put("i", "55");
            }
        };

        Bean1 b1 = factory(overrides, "s: replace_me\ni: 5").config(Bean1.class, "");
        assertNotNull(b1);
        assertEquals("SS", b1.s);
        assertEquals(55, b1.i);
    }

    @Test
    public void testGet_SingleConfig_PropOverride_Nested() {

        Map<String, String> overrides = new HashMap<String, String>() {
            private static final long serialVersionUID = 1L;

            {
                put("b1.s", "SS");
                put("b1.i", "55");
            }
        };

        Bean1 b1 = factory(overrides, "b1:\n  s: replace_me\n  i: 6").config(Bean1.class, "b1");
        assertNotNull(b1);
        assertEquals("SS", b1.s);
        assertEquals(55, b1.i);
    }

    public static class Bean1 {

        private String s;
        private int i;
        private long l;

        public String getS() {
            return s;
        }

        public int getI() {
            return i;
        }

        public long getL() {
            return l;
        }

    }
}
