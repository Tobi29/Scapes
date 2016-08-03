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

package org.tobi29.scapes.packets;

import org.tobi29.scapes.block.GameRegistry;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;

public abstract class PacketAbstract implements Packet {
    protected final Vector3 pos;
    protected final double range;
    protected final boolean isChunkContent, vital;

    protected PacketAbstract() {
        this(null, 0.0, false);
    }

    protected PacketAbstract(Vector3 pos, double range, boolean isChunkContent) {
        this(pos, range, isChunkContent, true);
    }

    protected PacketAbstract(Vector3 pos, double range, boolean isChunkContent,
            boolean vital) {
        this.pos = pos;
        this.range = range;
        this.isChunkContent = isChunkContent;
        this.vital = vital;
    }

    protected PacketAbstract(Vector3 pos) {
        this(pos, 0.0, false);
    }

    protected PacketAbstract(Vector3 pos, double range) {
        this(pos, range, false);
    }

    protected PacketAbstract(Vector3 pos, boolean isChunkContent) {
        this(pos, 0.0, isChunkContent);
    }

    public static PacketAbstract make(GameRegistry registry, short id) {
        return registry.<GameRegistry,PacketAbstract>getSupplier("Core", "Packet").get(id)
                .apply(registry);
    }

    public short id(GameRegistry registry) {
        return (short) registry.getSupplier("Core", "Packet").id(this);
    }

    public Vector3 pos() {
        return pos;
    }

    public double range() {
        return range;
    }

    public boolean isChunkContent() {
        return isChunkContent;
    }

    public boolean isVital() {
        return vital;
    }
}
