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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ItemIngot extends VanillaItem implements ItemHeatable, ItemMetal {
    private final Map<MetalType, ItemModel> modelsShaped =
            new ConcurrentHashMap<>(), modelsRaw = new ConcurrentHashMap<>();
    private TerrainTexture textureShaped, textureRaw, textureMold, textureStone;
    private ItemModel modelMold, modelStone;

    public ItemIngot(VanillaMaterial materials) {
        super(materials, "vanilla.basics.item.Ingot");
    }

    @Override
    public void registerTextures(TerrainTextureRegistry registry) {
        textureShaped = registry.registerTexture(
                "VanillaBasics:image/terrain/metals/ingot/Shaped.png");
        textureRaw = registry.registerTexture(
                "VanillaBasics:image/terrain/metals/ingot/Raw.png");
        textureMold = registry.registerTexture(
                "VanillaBasics:image/terrain/tools/MoldFilled.png");
        textureStone = registry.registerTexture(
                "VanillaBasics:image/terrain/metals/ingot/Stone.png");
    }

    @Override
    public void createModels(TerrainTextureRegistry registry) {
        modelMold = new ItemModelSimple(textureMold, 1.0f, 1.0f, 1.0f, 1.0f);
        modelStone = new ItemModelSimple(textureStone, 1.0f, 1.0f, 1.0f, 1.0f);
    }

    @Override
    public void render(ItemStack item, GL gl, Shader shader,
            float r, float g, float b, float a) {
        if ("Stone".equals(item.metaData("Vanilla").getString("MetalType"))) {
            modelStone.render(gl, shader);
        } else {
            MetalType metal = plugin.getMetalType(
                    item.metaData("Vanilla").getString("MetalType"));
            if (metal == null) {
                metal = plugin.getMetalType("Iron");
            }
            switch (item.data()) {
                case 1:
                    modelShaped(metal).render(gl, shader);
                    break;
                default:
                    modelRaw(metal).render(gl, shader);
                    modelMold.render(gl, shader);
                    break;
            }
        }
    }

    @Override
    public void renderInventory(ItemStack item, GL gl,
            Shader shader, float r, float g, float b, float a) {
        if ("Stone".equals(item.metaData("Vanilla").getString("MetalType"))) {
            modelStone.renderInventory(gl, shader);
        } else {
            MetalType metal = plugin.getMetalType(
                    item.metaData("Vanilla").getString("MetalType"));
            if (metal == null) {
                metal = plugin.getMetalType("Iron");
            }
            switch (item.data()) {
                case 1:
                    modelShaped(metal).renderInventory(gl, shader);
                    break;
                default:
                    modelRaw(metal).renderInventory(gl, shader);
                    modelMold.renderInventory(gl, shader);
                    break;
            }
        }
    }

    @Override
    public String name(ItemStack item) {
        StringBuilder name = new StringBuilder(50);
        if (item.data() == 0) {
            name.append("Unshaped ");
        }
        MetalType metalType = metalType(item);
        name.append(metalType.ingotName());
        float temperature = temperature(item);
        if (temperature > 0.1f) {
            name.append("\nTemp.:").append(FastMath.floor(temperature))
                    .append("°C");
            if (temperature > meltingPoint(item)) {
                name.append("\n - Liquid");
            }
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
            if (currentTemperature >= meltingPoint(item) && item.data() == 1) {
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
                        .setFloat("Temperature", currentTemperature / 1.1f);
            } else {
                item.item().metaData("Vanilla")
                        .setFloat("Temperature", currentTemperature / 1.002f);
            }
        }
    }

    @Override
    public float meltingPoint(ItemStack item) {
        MetalType metal = plugin.getMetalType(
                item.metaData("Vanilla").getString("MetalType"));
        if (metal != null) {
            return metal.meltingPoint();
        }
        return 100.0f;
    }

    @Override
    public float temperature(ItemStack item) {
        return item.metaData("Vanilla").getFloat("Temperature");
    }

    @Override
    public MetalType metalType(ItemStack item) {
        return plugin.getMetalType(
                item.metaData("Vanilla").getString("MetalType"));
    }

    private ItemModel modelRaw(MetalType metal) {
        ItemModel model = modelsRaw.get(metal);
        if (model == null) {
            float r = metal.r();
            float g = metal.g();
            float b = metal.b();
            model = new ItemModelSimple(textureRaw, r, g, b, 1.0f);
            modelsRaw.put(metal, model);
        }
        return model;
    }

    private ItemModel modelShaped(MetalType metal) {
        ItemModel model = modelsShaped.get(metal);
        if (model == null) {
            float r = metal.r();
            float g = metal.g();
            float b = metal.b();
            model = new ItemModelSimple(textureShaped, r, g, b, 1.0f);
            modelsShaped.put(metal, model);
        }
        return model;
    }
}
