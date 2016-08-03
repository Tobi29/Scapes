/*
 * Copyright 2012-2016 Tobi29
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

import java8.util.Optional;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;

public interface ChunkArraySection {
    int data(int x, int y, int z, int offset);

    int data(int offset);

    void data(int x, int y, int z, int offset, int value);

    void data(int offset, int value);

    void dataUnsafe(int x, int y, int z, int offset, int value);

    void dataUnsafe(int offset, int value);

    boolean isEmpty();

    boolean compress();

    Optional<TagStructure> save();

    void load(TagStructure tag);
}
