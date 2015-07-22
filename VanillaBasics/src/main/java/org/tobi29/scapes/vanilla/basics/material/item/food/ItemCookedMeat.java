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
import org.tobi29.scapes.engine.opengl.GL;
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

public class ItemCookedMeat extends VanillaItem implements ItemHeatable {
    private TerrainTexture[] textures;
    private ItemModel[] models;

    public ItemCookedMeat(VanillaMaterial materials) {
        super(materials, "vanilla.basics.item.CookedMeat");
    }

    @Override
    public void click(MobPlayerServer entity, ItemStack item) {
        TagStructure conditionTag =
                entity.metaData("Vanilla").getStructure("Condition");
        synchronized (conditionTag) {
            if (temperature(item) >= 30) {
                double stamina = conditionTag.getDouble("Stamina");
                conditionTag.setDouble("Stamina", stamina - 0.04);
                double hunger = conditionTag.getDouble("Hunger");
                conditionTag.setDouble("Hunger", hunger + 0.4);
                double thirst = conditionTag.getDouble("Thirst");
                conditionTag.setDouble("Thirst", thirst - 0.1);
                entity.heal(10);
            } else {
                double stamina = conditionTag.getDouble("Stamina");
                conditionTag.setDouble("Stamina", stamina - 0.4);
                double hunger = conditionTag.getDouble("Hunger");
                conditionTag.setDouble("Hunger", hunger + 0.2);
                double thirst = conditionTag.getDouble("Thirst");
                conditionTag.setDouble("Thirst", thirst - 0.2);
            }
        }
        item.setAmount(item.amount() - 1);
    }

    @Override
    public double click(MobPlayerServer entity, ItemStack item, MobServer hit) {
        if (temperature(item) >= 120) {
            if (hit instanceof MobLivingServer) {
                ((MobLivingServer) hit).heal(10);
                item.setAmount(item.amount() - 1);
            }
            return 0;
        } else {
            return materials.meat.click(entity, item, hit);
        }
    }

    @Override
    public void registerTextures(TerrainTextureRegistry registry) {
        textures = new TerrainTexture[1];
        textures[0] = registry.registerTexture(
                "VanillaBasics:image/terrain/food/meat/pork/Cooked.png");
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
        switch (item.data()) {
            default:
                name.append("Cooked Porkchop");
        }
        float temperature = temperature(item);
        if (temperature > 0.1f) {
            name.append("\nTemp.:").append(FastMath.floor(temperature))
                    .append("°C");
        }
        return name.toString();
    }

    @Override
    public int maxStackSize(ItemStack item) {
        return 1;
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
                item.setAmount(0);
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
                return 120;
        }
    }

    @Override
    public float temperature(ItemStack item) {
        return item.metaData("Vanilla").getFloat("Temperature");
    }
}
