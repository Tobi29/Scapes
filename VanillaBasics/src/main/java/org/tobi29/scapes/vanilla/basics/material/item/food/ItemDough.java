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
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.entity.server.MobItemServer;
import org.tobi29.scapes.vanilla.basics.material.CropType;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;
import org.tobi29.scapes.vanilla.basics.material.item.ItemHeatable;
import org.tobi29.scapes.vanilla.basics.material.item.ItemResearch;
import org.tobi29.scapes.vanilla.basics.material.item.VanillaItem;

import java.util.List;

public class ItemDough extends VanillaItem
        implements ItemHeatable, ItemResearch {
    private final GameRegistry.Registry<CropType> cropRegistry;
    private TerrainTexture[] textures;
    private ItemModel[] models;

    public ItemDough(VanillaMaterial materials,
            GameRegistry.Registry<CropType> cropRegistry) {
        super(materials, "vanilla.basics.item.Dough");
        this.cropRegistry = cropRegistry;
    }

    @Override
    public void registerTextures(TerrainTextureRegistry registry) {
        List<CropType> types = cropRegistry.values();
        textures = new TerrainTexture[types.size()];
        int i = 0;
        for (CropType type : types) {
            textures[i++] =
                    registry.registerTexture(type.texture() + "/Dough.png");
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
        StringBuilder name = new StringBuilder(40);
        name.append(materials.crop.name(item)).append("Dough");
        float temperature = temperature(item);
        if (temperature > 0.1f) {
            name.append("\nTemp.:").append(FastMath.floor(temperature))
                    .append("°C");
        }
        return name.toString();
    }

    @Override
    public int maxStackSize(ItemStack item) {
        return temperature(item) == 0 ? 8 : 1;
    }

    @Override
    public void heat(ItemStack item, float temperature) {
        float currentTemperature = temperature(item);
        if (currentTemperature < 1 && temperature < currentTemperature) {
            item.metaData("Vanilla").setFloat("Temperature", 0.0f);
        } else {
            item.metaData("Vanilla").setFloat("Temperature", FastMath.max(
                    currentTemperature +
                            (temperature - currentTemperature) / 400.0f, 1.1f));
            if (currentTemperature >= meltingPoint(item)) {
                item.setMaterial(materials.baked);
            }
        }
    }

    @Override
    public void cool(ItemStack item) {
        float currentTemperature = temperature(item);
        if (currentTemperature < 1) {
            item.metaData("Vanilla").setFloat("Temperature", 0.0f);
        } else {
            item.metaData("Vanilla")
                    .setFloat("Temperature", currentTemperature / 1.002f);
        }
    }

    @Override
    public void cool(MobItemServer item) {
        float currentTemperature = temperature(item.item());
        if (currentTemperature < 1) {
            item.item().metaData("Vanilla").setFloat("Temperature", 0.0f);
        } else {
            if (item.isInWater()) {
                item.item().metaData("Vanilla")
                        .setFloat("Temperature", currentTemperature / 4.0f);
            } else {
                item.item().metaData("Vanilla")
                        .setFloat("Temperature", currentTemperature / 1.002f);
            }
        }
    }

    @Override
    public float meltingPoint(ItemStack item) {
        switch (item.data()) {
            default:
                return 40;
        }
    }

    @Override
    public float temperature(ItemStack item) {
        return item.metaData("Vanilla").getFloat("Temperature");
    }

    @Override
    public String[] identifiers(ItemStack item) {
        return new String[]{"vanilla.basics.item.Dough",
                "vanilla.basics.item.Dough." + materials.crop.name(item)};
    }
}
