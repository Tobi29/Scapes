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

import java8.util.stream.Stream;
import org.tobi29.scapes.engine.utils.Streams;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class ChatHistory {
    private final List<ChatLine> lines = new ArrayList<>();
    private final Map<Object, Runnable> changeListeners = new WeakHashMap<>();

    public synchronized void addLine(String text) {
        String[] lines = text.split("\n");
        for (String line : lines) {
            this.lines.add(0, new ChatLine(line));
        }
        Streams.forEach(changeListeners.values(), Runnable::run);
    }

    @SuppressWarnings("CallToNativeMethodWhileLocked")
    public synchronized void update() {
        long time = System.currentTimeMillis();
        List<ChatLine> removals =
                Streams.collect(lines, line -> time - line.time > 10000);
        if (!removals.isEmpty()) {
            lines.removeAll(removals);
            Streams.forEach(changeListeners.values(), Runnable::run);
        }
    }

    public synchronized void listener(Object owner, Runnable listener) {
        changeListeners.put(owner, listener);
    }

    public synchronized Stream<String> lines() {
        return Streams.of(lines).map(line -> line.text);
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
