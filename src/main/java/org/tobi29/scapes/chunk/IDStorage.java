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
package org.tobi29.scapes.chunk;

import org.tobi29.scapes.engine.utils.io.tag.TagStructure;

import java.util.Map;

public class IDStorage {
    private final TagStructure tagStructure;

    public IDStorage(TagStructure tagStructure) {
        this.tagStructure = tagStructure;
    }

    public int get(String module, String type, String name) {
        return get(module, type, name, 0, Integer.MAX_VALUE);
    }

    public synchronized int get(String module, String type, String name, int i,
            int max) {
        TagStructure typeTag =
                tagStructure.getStructure(module).getStructure(type);
        if (typeTag.has(name)) {
            return typeTag.getInteger(name);
        }
        while (true) {
            if (i > max) {
                throw new IllegalStateException(
                        "Overflowed IDs for: " + module + "->" + type);
            }
            boolean contains = false;
            for (Map.Entry<String, Object> entry : typeTag.getTagEntrySet()) {
                Object value = entry.getValue();
                if (value instanceof Number) {
                    if (((Number) value).intValue() == i) {
                        contains = true;
                        break;
                    }
                }
            }
            if (contains) {
                i++;
            } else {
                break;
            }
        }
        typeTag.setInteger(name, i);
        return i;
    }

    public synchronized void set(String module, String type, String name,
            int value) {
        tagStructure.getStructure(module).getStructure(type)
                .setInteger(name, value);
    }

    public TagStructure save() {
        return tagStructure;
    }
}
