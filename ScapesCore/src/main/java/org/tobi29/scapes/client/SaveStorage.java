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

package org.tobi29.scapes.client;

import java8.util.stream.Stream;
import org.tobi29.scapes.server.format.WorldSource;

import java.io.IOException;

public interface SaveStorage {
    Stream<String> list() throws IOException;

    boolean exists(String name) throws IOException;

    WorldSource get(String name) throws IOException;

    boolean delete(String name) throws IOException;

    boolean loadClasses();
}
