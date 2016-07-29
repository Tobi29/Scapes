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
import org.tobi29.scapes.engine.graphics.GL;
import org.tobi29.scapes.engine.graphics.Shader;
import org.tobi29.scapes.engine.utils.math.Face;
import org.tobi29.scapes.entity.server.MobPlayerServer;
import org.tobi29.scapes.entity.server.MobServer;

public abstract class Material {
    protected final GameRegistry registry;
    protected final String nameID;
    protected int id;

    protected Material(GameRegistry registry, String nameID) {
        this.nameID = nameID;
        this.registry = registry;
    }

    public ItemStack example(int data) {
        return new ItemStack(this, data);
    }

    public void click(MobPlayerServer entity, ItemStack item) {
    }

    public double click(MobPlayerServer entity, ItemStack item,
            TerrainServer terrain, int x, int y, int z, Face face) {
        return 0.0;
    }

    public double click(MobPlayerServer entity, ItemStack item, MobServer hit) {
        return 0.0;
    }

    public String nameID() {
        return nameID;
    }

    public GameRegistry registry() {
        return registry;
    }

    public int itemID() {
        return id;
    }

    public int toolLevel(ItemStack item) {
        return 0;
    }

    public String toolType(ItemStack item) {
        return "None";
    }

    public boolean isTool(ItemStack item) {
        return false;
    }

    public boolean isWeapon(ItemStack item) {
        return false;
    }

    public int hitWait(ItemStack item) {
        return 500;
    }

    public double hitRange(ItemStack item) {
        return 2;
    }

    public abstract void registerTextures(TerrainTextureRegistry registry);

    public abstract void createModels(TerrainTextureRegistry registry);

    public abstract void render(ItemStack item, GL gl, Shader shader, float r,
            float g, float b, float a);

    public abstract void renderInventory(ItemStack item, GL gl, Shader shader,
            float r, float g, float b, float a);

    public float playerLight(ItemStack item) {
        return 0;
    }

    public abstract String name(ItemStack item);

    public abstract int maxStackSize(ItemStack item);
}
