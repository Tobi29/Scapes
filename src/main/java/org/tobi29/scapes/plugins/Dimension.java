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

package org.tobi29.scapes.plugins;

import org.tobi29.scapes.chunk.World;
import org.tobi29.scapes.chunk.WorldEnvironment;

/**
 * Basic interface dor dimension addons
 */
public interface Dimension extends Addon {
    /**
     * Called to create a {@code WorldEnvironment}
     *
     * @param world The {@code World} that the environment is created for
     * @return A newly created {@code WorldEnvironment} for the given world
     */
    WorldEnvironment createEnvironment(World world);
}
