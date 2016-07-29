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

import org.tobi29.scapes.block.GameRegistry;
import org.tobi29.scapes.block.ItemStack;
import org.tobi29.scapes.block.TerrainTexture;
import org.tobi29.scapes.block.TerrainTextureRegistry;
import org.tobi29.scapes.block.models.ItemModel;
import org.tobi29.scapes.block.models.ItemModelSimple;
import org.tobi29.scapes.engine.graphics.GL;
import org.tobi29.scapes.engine.graphics.Shader;
import org.tobi29.scapes.vanilla.basics.material.CropType;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;
import org.tobi29.scapes.vanilla.basics.material.item.ItemResearch;
import org.tobi29.scapes.vanilla.basics.material.item.VanillaItem;

import java.util.List;

public class ItemCrop extends VanillaItem implements ItemResearch {
    private final GameRegistry.Registry<CropType> cropRegistry;
    private TerrainTexture[] textures;
    private ItemModel[] models;

    public ItemCrop(VanillaMaterial materials,
            GameRegistry.Registry<CropType> cropRegistry) {
        super(materials, "vanilla.basics.item.Crop");
        this.cropRegistry = cropRegistry;
    }

    @Override
    public void registerTextures(TerrainTextureRegistry registry) {
        List<CropType> types = cropRegistry.values();
        textures = new TerrainTexture[types.size()];
        int i = 0;
        for (CropType type : types) {
            textures[i++] =
                    registry.registerTexture(type.texture() + "/Drop.png");
        }
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
    public void render(ItemStack item, GL gl, Shader shader, float r, float g,
            float b, float a) {
        models[item.data()].render(gl, shader);
    }

    @Override
    public void renderInventory(ItemStack item, GL gl, Shader shader, float r,
            float g, float b, float a) {
        models[item.data()].renderInventory(gl, shader);
    }

    @Override
    public String name(ItemStack item) {
        return materials.crop.name(item);
    }

    @Override
    public int maxStackSize(ItemStack item) {
        return 16;
    }

    @Override
    public String[] identifiers(ItemStack item) {
        return new String[]{"vanilla.basics.item.Crop",
                "vanilla.basics.item.Crop." + materials.crop.name(item)};
    }
}
