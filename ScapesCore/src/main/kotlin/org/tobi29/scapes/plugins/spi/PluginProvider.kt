/*
 * Copyright 2012-2018 Tobi29
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

@file:Suppress("NOTHING_TO_INLINE")

package org.tobi29.scapes.plugins.spi

import org.tobi29.io.Path
import org.tobi29.io.ReadSource
import org.tobi29.scapes.plugins.Plugin
import org.tobi29.utils.Version
import org.tobi29.utils.compare

interface PluginProvider {
    val plugins: List<PluginHandle>
}

typealias PluginHandle = Pair<PluginDescription, () -> Plugin>

data class PluginDescription(
    val id: String,
    val name: String,
    val parent: String,
    val version: Version,
    val scapesVersion: Version,
    val assetRoot: Path,
    val icon: ReadSource
)

inline fun PluginDescription(
    id: String,
    name: String,
    parent: String,
    version: Version,
    scapesVersion: Version,
    assetRoot: Path,
    icon: String
): PluginDescription = PluginDescription(
    id, name, parent, version, scapesVersion, assetRoot, assetRoot[icon]
)

data class PluginReference(
    val id: String,
    val version: Version
)

fun PluginDescription.refer(): PluginReference =
    PluginReference(id, version)

fun PluginDescription.matches(other: PluginReference): Boolean {
    if (id != other.id) return false
    return compare(version, other.version).inside(
        Version.Comparison.LOWER_REVISION,
        Version.Comparison.HIGHER_REVISION
    )
}

fun PluginDescription.supports(other: PluginReference): Boolean {
    if (id != other.id) return false
    return compare(version, other.version).inside(
        Version.Comparison.LOWER_REVISION,
        Version.Comparison.HIGHER_MINOR
    )
}
