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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A helper for printing text to a fixed-width console that handles text folding and offsets.
 */
public class ConsoleAppender {

    static final String NEWLINE = System.getProperty("line.separator");

    static final int MIN_LINE_WIDTH = 10;
    private static final Pattern SPACE = Pattern.compile("\\s+");

    private Appendable out;
    private int lineWidth;
    private String offset;

    public ConsoleAppender(Appendable out, int lineWidth) {

        if (lineWidth < MIN_LINE_WIDTH) {
            throw new IllegalArgumentException("Line width is too small. Minimal supported width is " + MIN_LINE_WIDTH);
        }

        this.offset = "";
        this.out = out;
        this.lineWidth = lineWidth;
    }

    protected ConsoleAppender(ConsoleAppender proto) {
        out = proto.out;
        lineWidth = proto.lineWidth;
        offset = proto.offset;

        // do not copy "transient" properties
    }

    private static List<String> asList(String... parts) {
        return parts != null ? Arrays.asList(parts) : Collections.emptyList();
    }

    /**
     * Creates and returns a new appender with the specified base offset.
     *
     * @return a new appender with the specified base offset.
     */
    public ConsoleAppender withOffset(int offset) {

        StringBuilder padding = new StringBuilder();
        for (int i = 0; i < offset; i++) {
            padding.append(" ");
        }

        return withOffset(padding.toString());
    }

    public ConsoleAppender withOffset(String offset) {
        ConsoleAppender offsetAppender = new ConsoleAppender(this);
        offsetAppender.offset = offsetAppender.offset + Objects.requireNonNull(offset);
        return offsetAppender;
    }

    public void println() {
        try {
            out.append(NEWLINE);
        } catch (IOException e) {
            throw new RuntimeException("Error printing help", e);
        }
    }

    public void println(String... phrases) {
        println(asList(phrases));
    }

    public void println(Collection<String> phrases) {

        try {
            out.append(this.offset);
            for (String p : phrases) {
                out.append(p);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error printing help", e);
        }

        println();
    }

    public void foldPrintln(String... phrases) {
        foldPrintln(asList(phrases));
    }

    public void foldPrintln(Collection<String> phrases) {

        foldToLines(phrases).forEach(line -> {
            try {
                out.append(this.offset);
                out.append(line);
                out.append(NEWLINE);
            } catch (IOException e) {
                throw new RuntimeException("Error printing help", e);
            }
        });
    }

    protected Collection<String> foldToLines(Collection<String> phrases) {

        int offset = this.offset.length();

        // refuse to fold lines to columns shorter than MIN_WIDTH
        int maxLength = lineWidth - offset > MIN_LINE_WIDTH ? lineWidth - offset : MIN_LINE_WIDTH;

        List<String> folded = new ArrayList<>();
        StringBuilder line = new StringBuilder();

        String joined = String.join("", phrases);

        SPACE.splitAsStream(joined).forEach(word -> {
            String separator = line.length() > 0 ? " " : "";

            if (line.length() + separator.length() + word.length() <= maxLength) {
                line.append(separator).append(word);
            } else {
                foldAtWord(maxLength, folded, line, word, separator);
            }
        });

        // save leftovers
        if (line.length() > 0) {
            folded.add(line.toString());
        }

        return folded;
    }

    private void foldAtWord(int maxLength, List<String> folded, StringBuilder line, String word, String separator) {
        if (word.length() <= maxLength) {
            folded.add(line.toString());
            line.setLength(0);
            line.append(word);
        } else {

            // fold long words...

            // append head to existing line
            int head = maxLength - separator.length() - line.length();
            if (head > 0) {
                line.append(separator).append(word.substring(0, head));
                folded.add(line.toString());
                line.setLength(0);
            }
            else if(head < 0) {
                // head is -1 when the line is full and even the separator won't fit.
                head = 0;
            }

            int len = word.length();
            int start = head;

            int end = -1;
            while (start < len) {

                end = start + maxLength;
                if (end > len) {
                    // don't fold yet, there may be more words...
                    end = len;
                    line.append(word, start, end);
                } else {

                    line.append(word, start, end);
                    folded.add(line.toString());
                    line.setLength(0);
                }

                start = end;
            }
        }
    }

}
