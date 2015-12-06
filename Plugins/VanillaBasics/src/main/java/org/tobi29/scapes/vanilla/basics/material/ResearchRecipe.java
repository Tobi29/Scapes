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
package org.tobi29.scapes.vanilla.basics.material;

import java8.util.stream.Stream;
import org.tobi29.scapes.engine.utils.Streams;

public class ResearchRecipe {
    private final String[] items;
    private final String name;
    private final String text;

    public ResearchRecipe(String name, String text, String[] items) {
        this.items = items;
        this.name = name;
        this.text = text;
    }

    public Stream<String> items() {
        return Streams.of(items);
    }

    public String name() {
        return name;
    }

    public String text() {
        return text;
    }
}
