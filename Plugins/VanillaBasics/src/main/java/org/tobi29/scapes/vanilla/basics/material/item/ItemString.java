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

package org.tobi29.scapes.vanilla.basics.material.item;

import org.tobi29.scapes.block.ItemStack;
import org.tobi29.scapes.block.TerrainTexture;
import org.tobi29.scapes.block.TerrainTextureRegistry;
import org.tobi29.scapes.block.models.ItemModel;
import org.tobi29.scapes.block.models.ItemModelSimple;
import org.tobi29.scapes.engine.graphics.GL;
import org.tobi29.scapes.engine.graphics.Shader;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;

public class ItemString extends VanillaItem {
    private TerrainTexture[] textures;
    private ItemModel[] models;

    public ItemString(VanillaMaterial materials) {
        super(materials, "vanilla.basics.item.String");
    }

    @Override
    public void registerTextures(TerrainTextureRegistry registry) {
        textures = new TerrainTexture[2];
        textures[0] = registry.registerTexture(
                "VanillaBasics:image/terrain/other/String.png");
        textures[1] = registry.registerTexture(
                "VanillaBasics:image/terrain/other/Fabric.png");
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
    public void render(ItemStack item, GL gl, Shader shader) {
        models[item.data()].render(gl, shader);
    }

    @Override
    public void renderInventory(ItemStack item, GL gl, Shader shader) {
        models[item.data()].renderInventory(gl, shader);
    }

    @Override
    public String name(ItemStack item) {
        switch (item.data()) {
            case 1:
                return "Fabric";
            default:
                return "String";
        }
    }

    @Override
    public int maxStackSize(ItemStack item) {
        return 32;
    }
}
