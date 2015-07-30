/*
 * Copyright 2012-2015 Tobi29
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tobi29.scapes.client;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ChatHistory {
    private final List<ChatLine> lines = new CopyOnWriteArrayList<>();

    public void addLine(String text) {
        String[] lines = text.split("\n");
        for (String line : lines) {
            this.lines.add(0, new ChatLine(line));
        }
    }

    public void update() {
        long time = System.currentTimeMillis();
        lines.removeAll(lines.stream().filter(line -> time - line.time > 10000)
                .collect(Collectors.toList()));
    }

    public Stream<String> lines() {
        return lines.stream().map(line -> line.text);
    }

    private static class ChatLine {
        private final String text;
        private final long time;

        public ChatLine(String text) {
            this.text = text;
            time = System.currentTimeMillis();
        }
    }
}
