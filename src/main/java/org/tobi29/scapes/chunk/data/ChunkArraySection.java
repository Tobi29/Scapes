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

package org.tobi29.scapes.chunk.data;

import org.tobi29.scapes.engine.utils.io.tag.TagStructure;

import java.util.Optional;

public interface ChunkArraySection {
    int getData(int x, int y, int z, int offset);

    int getData(int offset);

    void setData(int x, int y, int z, int offset, int value);

    void setData(int offset, int value);

    boolean isEmpty();

    boolean compress();

    Optional<TagStructure> save();

    void load(TagStructure tag);
}
