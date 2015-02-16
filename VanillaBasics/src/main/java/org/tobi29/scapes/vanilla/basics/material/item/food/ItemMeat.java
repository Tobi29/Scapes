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

import org.tobi29.scapes.block.ItemStack;
import org.tobi29.scapes.block.TerrainTexture;
import org.tobi29.scapes.block.TerrainTextureRegistry;
import org.tobi29.scapes.block.models.ItemModel;
import org.tobi29.scapes.block.models.ItemModelSimple;
import org.tobi29.scapes.engine.opengl.GraphicsSystem;
import org.tobi29.scapes.engine.opengl.shader.Shader;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.entity.server.MobItemServer;
import org.tobi29.scapes.entity.server.MobLivingServer;
import org.tobi29.scapes.entity.server.MobPlayerServer;
import org.tobi29.scapes.entity.server.MobServer;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;
import org.tobi29.scapes.vanilla.basics.material.item.ItemHeatable;
import org.tobi29.scapes.vanilla.basics.material.item.VanillaItem;

public class ItemMeat extends VanillaItem implements ItemHeatable {
    private TerrainTexture[] textures;
    private ItemModel[] models;

    public ItemMeat(VanillaMaterial materials) {
        super(materials, "vanilla.basics.item.Meat");
    }

    @Override
    public void click(MobPlayerServer entity, ItemStack item) {
        TagStructure conditionTag =
                entity.getMetaData("Vanilla").getStructure("Condition");
        synchronized (conditionTag) {
            double stamina = conditionTag.getDouble("Stamina");
            conditionTag.setDouble("Stamina", stamina - 0.8);
            double hunger = conditionTag.getDouble("Hunger");
            conditionTag.setDouble("Hunger", hunger + 0.1);
            double thirst = conditionTag.getDouble("Thirst");
            conditionTag.setDouble("Thirst", thirst - 0.3);
        }
        entity.damage(5.0);
        item.setAmount(item.getAmount() - 1);
    }

    @Override
    public double click(MobPlayerServer entity, ItemStack item, MobServer hit) {
        if (hit instanceof MobLivingServer) {
            ((MobLivingServer) hit).damage(1);
            item.setAmount(item.getAmount() - 1);
        }
        return 0;
    }

    @Override
    public void registerTextures(TerrainTextureRegistry registry) {
        textures = new TerrainTexture[1];
        textures[0] = registry.registerTexture(
                "VanillaBasics:image/terrain/food/meat/pork/Raw.png");
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
    public void render(ItemStack item, GraphicsSystem graphics, Shader shader,
            float r, float g, float b, float a) {
        models[item.getData()].render(graphics, shader);
    }

    @Override
    public void renderInventory(ItemStack item, GraphicsSystem graphics,
            Shader shader, float r, float g, float b, float a) {
        models[item.getData()].renderInventory(graphics, shader);
    }

    @Override
    public String getName(ItemStack item) {
        StringBuilder name = new StringBuilder(40);
        switch (item.getData()) {
            default:
                name.append("Porkchop");
        }
        float temperature = getTemperature(item);
        if (temperature > 0.1f) {
            name.append("\nTemp.:").append(FastMath.floor(temperature))
                    .append("°C");
        }
        return name.toString();
    }

    @Override
    public int getStackSize(ItemStack item) {
        return getTemperature(item) == 0 ? 32 : 1;
    }

    @Override
    public void heat(ItemStack item, float temperature) {
        float currentTemperature = getTemperature(item);
        if (currentTemperature < 1 && temperature < currentTemperature) {
            item.getMetaData("Vanilla").setFloat("Temperature", 0.0f);
        } else {
            item.getMetaData("Vanilla").setFloat("Temperature", FastMath.max(
                    currentTemperature +
                            (temperature - currentTemperature) / 400.0f, 1.1f));
            if (currentTemperature >= getMeltingPoint(item)) {
                item.setMaterial(materials.cookedMeat);
            }
        }
    }

    @Override
    public void cool(ItemStack item) {
        float currentTemperature = getTemperature(item);
        if (currentTemperature < 1) {
            item.getMetaData("Vanilla").setFloat("Temperature", 0.0f);
        } else {
            item.getMetaData("Vanilla")
                    .setFloat("Temperature", currentTemperature / 1.002f);
        }
    }

    @Override
    public void cool(MobItemServer item) {
        float currentTemperature = getTemperature(item.getItem());
        if (currentTemperature < 1) {
            item.getItem().getMetaData("Vanilla").setFloat("Temperature", 0.0f);
        } else {
            if (item.isInWater()) {
                item.getItem().getMetaData("Vanilla")
                        .setFloat("Temperature", currentTemperature / 4.0f);
            } else {
                item.getItem().getMetaData("Vanilla")
                        .setFloat("Temperature", currentTemperature / 1.002f);
            }
        }
    }

    @Override
    public float getMeltingPoint(ItemStack item) {
        switch (item.getData()) {
            default:
                return 60;
        }
    }

    @Override
    public float getTemperature(ItemStack item) {
        return item.getMetaData("Vanilla").getFloat("Temperature");
    }
}
