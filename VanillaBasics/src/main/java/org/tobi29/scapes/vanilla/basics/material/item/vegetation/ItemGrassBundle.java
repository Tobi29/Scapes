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

package org.tobi29.scapes.vanilla.basics.material.item.vegetation;

import org.tobi29.scapes.block.ItemStack;
import org.tobi29.scapes.block.TerrainTexture;
import org.tobi29.scapes.block.TerrainTextureRegistry;
import org.tobi29.scapes.block.models.ItemModel;
import org.tobi29.scapes.block.models.ItemModelSimple;
import org.tobi29.scapes.engine.opengl.GL;
import org.tobi29.scapes.engine.opengl.shader.Shader;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;
import org.tobi29.scapes.vanilla.basics.material.item.ItemFuel;
import org.tobi29.scapes.vanilla.basics.material.item.VanillaItem;

public class ItemGrassBundle extends VanillaItem implements ItemFuel {
    private TerrainTexture[] textures;
    private ItemModel[] models;

    public ItemGrassBundle(VanillaMaterial materials) {
        super(materials, "vanilla.basics.item.GrassBundle");
    }

    @Override
    public void registerTextures(TerrainTextureRegistry registry) {
        textures = new TerrainTexture[2];
        textures[0] = registry.registerTexture(
                "VanillaBasics:image/terrain/other/GrassBundle.png");
        textures[1] = registry.registerTexture(
                "VanillaBasics:image/terrain/other/Straw.png");
    }

    @Override
    public void createModels(TerrainTextureRegistry registry) {
        models = new ItemModel[textures.length];
        for (int i = 0; i < textures.length; i++) {
            models[i] =
                    new ItemModelSimple(textures[i], 1.0f, 1.0f, 1.0f, 1.0f);
        }
    }

    @Override
    public void render(ItemStack item, GL gl, Shader shader,
            float r, float g, float b, float a) {
        models[item.data()].render(gl, shader);
    }

    @Override
    public void renderInventory(ItemStack item, GL gl,
            Shader shader, float r, float g, float b, float a) {
        models[item.data()].renderInventory(gl, shader);
    }

    @Override
    public String name(ItemStack item) {
        switch (item.data()) {
            case 1:
                return "Straw";
            default:
                return "Grass Bundle";
        }
    }

    @Override
    public int maxStackSize(ItemStack item) {
        return 128;
    }

    @Override
    public float fuelTemperature(ItemStack item) {
        return 0.06f;
    }

    @Override
    public float fuelTime(ItemStack item) {
        return 1600.0f;
    }

    @Override
    public int fuelTier(ItemStack item) {
        return 0;
    }
}
