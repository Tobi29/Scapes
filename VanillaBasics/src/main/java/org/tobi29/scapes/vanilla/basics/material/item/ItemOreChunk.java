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
import org.tobi29.scapes.engine.opengl.GraphicsSystem;
import org.tobi29.scapes.engine.opengl.shader.Shader;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.entity.server.MobItemServer;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;

public class ItemOreChunk extends VanillaItem
        implements ItemHeatable, ItemResearch {
    private TerrainTexture[] textures;
    private ItemModel[] models;

    public ItemOreChunk(VanillaMaterial materials) {
        super(materials, "vanilla.basics.item.OreChunk");
    }

    @Override
    public void registerTextures(TerrainTextureRegistry registry) {
        textures = new TerrainTexture[11];
        for (short i = 0; i < textures.length; i++) {
            textures[i] = registry.registerTexture(getItemTexture(i));
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
        StringBuilder name = new StringBuilder(50);
        name.append(getOreName(item));
        float temperature = getTemperature(item);
        if (temperature > 0.1f) {
            name.append("\nTemp.:").append(FastMath.floor(temperature))
                    .append("°C");
        }
        return name.toString();
    }

    @Override
    public int getStackSize(ItemStack item) {
        if (item.getData() == 0) {
            return getTemperature(item) == 0 ? 16 : 1;
        } else {
            return getTemperature(item) == 0 ? 4 : 1;
        }
    }

    public String getItemTexture(short data) {
        switch (data) {
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
            case 10:
                return "VanillaBasics:image/terrain/ore/chunk/Bismuthinite.png";
            default:
                return "VanillaBasics:image/terrain/ore/chunk/Coal.png";
        }
    }

    public String getOreName(ItemStack item) {
        switch (item.getData()) {
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
            case 10:
                return "Bismuthinite";
            default:
                return "Coal";
        }
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
                String metal = getMetalName(item);
                if (metal != null) {
                    if ("Iron Bloom".equals(metal)) {
                        item.setData((short) 8);
                    } else {
                        item.setMaterial(materials.ingot);
                        item.setData((short) 1);
                        item.getMetaData("Vanilla")
                                .setString("MetalType", metal);
                    }
                }
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
            case 10:
                return 271.0f;
            default:
                return 1000000000000.0f;
        }
    }

    @Override
    public float getTemperature(ItemStack item) {
        return item.getMetaData("Vanilla").getFloat("Temperature");
    }

    public String getMetalName(ItemStack item) {
        switch (item.getData()) {
            case 1:
                return "Copper";
            case 2:
                return "Tin";
            case 3:
                return "Zinc";
            case 4:
                return "Iron Bloom";
            case 5:
                return "Iron Bloom";
            case 6:
                return "Silver";
            case 7:
                return "Gold";
            case 9:
                return "Iron";
            case 10:
                return "Bismuth";
            default:
                return null;
        }
    }

    @Override
    public String getInfoText(ItemStack item) {
        return null;
    }

    @Override
    public String[] getIdentifiers(ItemStack item) {
        return new String[]{"vanilla.basics.item.OreChunk",
                "vanilla.basics.item.OreChunk." + getOreName(item)};
    }
}
