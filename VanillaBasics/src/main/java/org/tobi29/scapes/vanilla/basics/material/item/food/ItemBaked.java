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

package org.tobi29.scapes.vanilla.basics.material.item.food;

import org.tobi29.scapes.block.GameRegistry;
import org.tobi29.scapes.block.ItemStack;
import org.tobi29.scapes.block.TerrainTexture;
import org.tobi29.scapes.block.TerrainTextureRegistry;
import org.tobi29.scapes.block.models.ItemModel;
import org.tobi29.scapes.block.models.ItemModelSimple;
import org.tobi29.scapes.engine.opengl.GL;
import org.tobi29.scapes.engine.opengl.shader.Shader;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.entity.server.MobPlayerServer;
import org.tobi29.scapes.vanilla.basics.material.CropType;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;
import org.tobi29.scapes.vanilla.basics.material.item.VanillaItem;

import java.util.List;

public class ItemBaked extends VanillaItem {
    private final GameRegistry.Registry<CropType> cropRegistry;
    private TerrainTexture[] textures;
    private ItemModel[] models;

    public ItemBaked(VanillaMaterial materials,
            GameRegistry.Registry<CropType> cropRegistry) {
        super(materials, "vanilla.basics.item.Baked");
        this.cropRegistry = cropRegistry;
    }

    @Override
    public void click(MobPlayerServer entity, ItemStack item) {
        TagStructure conditionTag =
                entity.getMetaData("Vanilla").getStructure("Condition");
        synchronized (conditionTag) {
            double stamina = conditionTag.getDouble("Stamina");
            conditionTag.setDouble("Stamina", stamina - 0.1);
            double hunger = conditionTag.getDouble("Hunger");
            conditionTag.setDouble("Hunger", hunger + 0.1);
            double thirst = conditionTag.getDouble("Thirst");
            conditionTag.setDouble("Thirst", thirst - 0.1);
        }
        item.setAmount(item.getAmount() - 1);
    }

    @Override
    public void registerTextures(TerrainTextureRegistry registry) {
        List<CropType> types = cropRegistry.values();
        textures = new TerrainTexture[types.size()];
        int i = 0;
        for (CropType type : types) {
            textures[i++] =
                    registry.registerTexture(type.getTexture() + "/Baked.png");
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
    public void render(ItemStack item, GL gl, Shader shader,
            float r, float g, float b, float a) {
        models[item.getData()].render(gl, shader);
    }

    @Override
    public void renderInventory(ItemStack item, GL gl,
            Shader shader, float r, float g, float b, float a) {
        models[item.getData()].renderInventory(gl, shader);
    }

    @Override
    public String getName(ItemStack item) {
        return cropRegistry.get(item.getData()).getBakedName();
    }

    @Override
    public int getStackSize(ItemStack item) {
        return 16;
    }
}
