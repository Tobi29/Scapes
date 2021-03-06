/*
 * Copyright 2012-2017 Tobi29
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
package org.tobi29.scapes.entity.server

import org.tobi29.utils.ComponentType
import org.tobi29.stdex.ConcurrentHashSet

typealias ContainerViewers = ConcurrentHashSet<MobPlayerServer>

inline val EntityServer.viewers: ContainerViewers
    get() = this[CONTAINER_VIEWERS_COMPONENT]

val CONTAINER_VIEWERS_COMPONENT = ComponentType.of<EntityServer, ContainerViewers, Any> { ContainerViewers() }
