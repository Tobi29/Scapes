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

package org.tobi29.scapes.vanilla.basics.material.item.vegetation;

import org.tobi29.scapes.block.GameRegistry;
import org.tobi29.scapes.block.ItemStack;
import org.tobi29.scapes.block.TerrainTexture;
import org.tobi29.scapes.block.TerrainTextureRegistry;
import org.tobi29.scapes.block.models.ItemModel;
import org.tobi29.scapes.block.models.ItemModelSimple;
import org.tobi29.scapes.chunk.terrain.TerrainServer;
import org.tobi29.scapes.engine.graphics.GL;
import org.tobi29.scapes.engine.graphics.Shader;
import org.tobi29.scapes.engine.utils.math.Face;
import org.tobi29.scapes.entity.server.MobPlayerServer;
import org.tobi29.scapes.vanilla.basics.entity.server.EntityFarmlandServer;
import org.tobi29.scapes.vanilla.basics.material.CropType;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;
import org.tobi29.scapes.vanilla.basics.material.item.VanillaItem;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class ItemSeed extends VanillaItem {
    private final GameRegistry.Registry<CropType> cropRegistry;
    private TerrainTexture[] textures;
    private ItemModel[] models;

    public ItemSeed(VanillaMaterial materials,
            GameRegistry.Registry<CropType> cropRegistry) {
        super(materials, "vanilla.basics.item.Seed");
        this.cropRegistry = cropRegistry;
    }

    @Override
    public double click(MobPlayerServer entity, ItemStack item,
            TerrainServer terrain, int x, int y, int z, Face face) {
        if (face == Face.UP) {
            item.setAmount(item.amount() - 1);
            Random random = ThreadLocalRandom.current();
            if (random.nextInt(1) == 0) {
                terrain.world().entities(x, y, z)
                        .filter(farmland -> farmland instanceof EntityFarmlandServer)
                        .forEach(farmland -> ((EntityFarmlandServer) farmland)
                                .seed(CropType.WHEAT));
            }
        }
        return 0.0;
    }

    @Override
    public void registerTextures(TerrainTextureRegistry registry) {
        List<CropType> types = cropRegistry.values();
        textures = new TerrainTexture[types.size()];
        int i = 0;
        for (CropType type : types) {
            textures[i++] =
                    registry.registerTexture(type.texture() + "/Seed.png");
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
    public void render(ItemStack item, GL gl, Shader shader) {
        models[item.data()].render(gl, shader);
    }

    @Override
    public void renderInventory(ItemStack item, GL gl, Shader shader) {
        models[item.data()].renderInventory(gl, shader);
    }

    @Override
    public String name(ItemStack item) {
        return materials.crop.name(item) + " Seeds";
    }

    @Override
    public int maxStackSize(ItemStack item) {
        return 128;
    }
}
