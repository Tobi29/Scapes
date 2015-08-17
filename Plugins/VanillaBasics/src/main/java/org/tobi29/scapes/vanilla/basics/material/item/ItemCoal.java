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

package org.tobi29.scapes.vanilla.basics.material.item;

import org.tobi29.scapes.block.ItemStack;
import org.tobi29.scapes.block.TerrainTexture;
import org.tobi29.scapes.block.TerrainTextureRegistry;
import org.tobi29.scapes.block.models.ItemModel;
import org.tobi29.scapes.block.models.ItemModelSimple;
import org.tobi29.scapes.engine.opengl.GL;
import org.tobi29.scapes.engine.opengl.shader.Shader;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;

public class ItemCoal extends VanillaItem implements ItemFuel {
    private TerrainTexture texture;
    private ItemModel model;

    public ItemCoal(VanillaMaterial materials) {
        super(materials, "vanilla.basics.item.Coal");
    }

    @Override
    public void registerTextures(TerrainTextureRegistry registry) {
        texture = registry.registerTexture(
                "VanillaBasics:image/terrain/ore/coal/Coal.png");
    }

    @Override
    public void createModels(TerrainTextureRegistry registry) {
        model = new ItemModelSimple(texture, 1.0f, 1.0f, 1.0f, 1.0f);
    }

    @Override
    public void render(ItemStack item, GL gl, Shader shader, float r, float g,
            float b, float a) {
        model.render(gl, shader);
    }

    @Override
    public void renderInventory(ItemStack item, GL gl, Shader shader, float r,
            float g, float b, float a) {
        model.renderInventory(gl, shader);
    }

    @Override
    public String name(ItemStack item) {
        return "Coal";
    }

    @Override
    public int maxStackSize(ItemStack item) {
        return 16;
    }

    @Override
    public float fuelTemperature(ItemStack item) {
        return 0.8f;
    }

    @Override
    public float fuelTime(ItemStack item) {
        return 200.0f;
    }

    @Override
    public int fuelTier(ItemStack item) {
        return 50;
    }
}