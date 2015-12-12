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
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.entity.server.MobItemServer;
import org.tobi29.scapes.vanilla.basics.material.MetalType;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;
import org.tobi29.scapes.vanilla.basics.util.IngotUtil;

public class ItemOreChunk extends VanillaItem
        implements ItemHeatable, ItemResearch {
    private TerrainTexture[] textures;
    private ItemModel[] models;

    public ItemOreChunk(VanillaMaterial materials) {
        super(materials, "vanilla.basics.item.OreChunk");
    }

    @Override
    public void registerTextures(TerrainTextureRegistry registry) {
        textures = new TerrainTexture[10];
        for (short i = 0; i < textures.length; i++) {
            textures[i] = registry.registerTexture(itemTexture(i));
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
        StringBuilder name = new StringBuilder(50);
        name.append(oreName(item));
        float temperature = temperature(item);
        if (temperature > 0.1f) {
            name.append("\nTemp.:").append(FastMath.floor(temperature))
                    .append("°C");
        }
        return name.toString();
    }

    @Override
    public int maxStackSize(ItemStack item) {
        if (item.data() == 0) {
            return temperature(item) == 0 ? 16 : 1;
        } else {
            return temperature(item) == 0 ? 4 : 1;
        }
    }

    public String itemTexture(short data) {
        switch (data) {
            case 0:
                return "VanillaBasics:image/terrain/ore/chunk/Bismuthinite.png";
            case 1:
                return "VanillaBasics:image/terrain/ore/chunk/Chalcocite.png";
            case 2:
                return "VanillaBasics:image/terrain/ore/chunk/Cassiterite.png";
            case 3:
                return "VanillaBasics:image/terrain/ore/chunk/Sphalerite.png";
            case 4:
                return "VanillaBasics:image/terrain/ore/chunk/Pyrite.png";
            case 5:
                return "VanillaBasics:image/terrain/ore/chunk/Magnetite.png";
            case 6:
                return "VanillaBasics:image/terrain/ore/chunk/Silver.png";
            case 7:
                return "VanillaBasics:image/terrain/ore/chunk/Gold.png";
            case 8:
            case 9:
                return "VanillaBasics:image/terrain/ore/chunk/IronBloom.png";
            default:
                throw new IllegalArgumentException("Unknown data: {}" + data);
        }
    }

    public String oreName(ItemStack item) {
        switch (item.data()) {
            case 0:
                return "Bismuthinite";
            case 1:
                return "Chalcocite";
            case 2:
                return "Cassiterite";
            case 3:
                return "Sphalerite";
            case 4:
                return "Pyrite";
            case 5:
                return "Magnetite";
            case 6:
                return "Native Silver";
            case 7:
                return "Native Gold";
            case 8:
                return "Iron Bloom";
            case 9:
                return "Worked Iron Bloom";
            default:
                throw new IllegalArgumentException(
                        "Unknown data: {}" + item.data());
        }
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
                int data = item.data();
                if (data == 4 || data == 5) {
                    item.setData(8);
                } else {
                    MetalType metal = metal(item);
                    item.setMaterial(materials.ingot, 1);
                    IngotUtil.createIngot(item, metal, temperature);
                }
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
            case 0:
                return 271.0f;
            case 1:
                return 1084.0f;
            case 2:
                return 231.0f;
            case 3:
                return 419.0f;
            case 4:
                return 1538.0f;
            case 5:
                return 1538.0f;
            case 6:
                return 961.0f;
            case 7:
                return 1064.0f;
            case 8:
                return 1538.0f;
            case 9:
                return 1538.0f;
            default:
                throw new IllegalArgumentException(
                        "Unknown data: {}" + item.data());
        }
    }

    @Override
    public float temperature(ItemStack item) {
        return item.metaData("Vanilla").getFloat("Temperature");
    }

    public MetalType metal(ItemStack item) {
        switch (item.data()) {
            case 0:
                return plugin.getMetalType("Bismuth");
            case 1:
                return plugin.getMetalType("Copper");
            case 2:
                return plugin.getMetalType("Tin");
            case 3:
                return plugin.getMetalType("Zinc");
            case 6:
                return plugin.getMetalType("Silver");
            case 7:
                return plugin.getMetalType("Gold");
            case 9:
                return plugin.getMetalType("Iron");
            default:
                throw new IllegalArgumentException(
                        "Unknown data: {}" + item.data());
        }
    }

    @Override
    public String[] identifiers(ItemStack item) {
        return new String[]{"vanilla.basics.item.OreChunk",
                "vanilla.basics.item.OreChunk." + oreName(item)};
    }
}
