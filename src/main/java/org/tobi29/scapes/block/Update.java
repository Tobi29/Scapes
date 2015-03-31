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

package org.tobi29.scapes.block;

import org.tobi29.scapes.chunk.terrain.TerrainServer;

public abstract class Update {
    protected int x, y, z;
    protected double delay;
    private boolean valid = true;

    public static Update make(GameRegistry registry, int x, int y, int z,
            double delay, short id) {
        try {
            return registry.<Update>getSupplier("Core", "Update").get(id)
                    .apply(registry).set(x, y, z, delay);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Failed to make update over reflection! (id: " +
                            id + ')', e);
        }
    }

    public Update set(int x, int y, int z, double delay) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.delay = delay;
        return this;
    }

    public void markAsInvalid() {
        valid = false;
    }

    public double delay(double delta) {
        delay -= delta;
        return delay;
    }

    public double getDelay() {
        return delay;
    }

    public short getID(GameRegistry registry) {
        return (short) registry.getSupplier("Core", "Update").getID(this);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public boolean isValid() {
        return valid;
    }

    public abstract void run(TerrainServer.TerrainMutable terrain);

    public abstract boolean isValidOn(BlockType type, TerrainServer terrain);
}
