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
package org.tobi29.scapes.vanilla.basics.entity.server;

import org.tobi29.scapes.block.GameRegistry;
import org.tobi29.scapes.chunk.WorldServer;
import org.tobi29.scapes.chunk.terrain.TerrainServer;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.engine.utils.math.vector.Vector3d;
import org.tobi29.scapes.entity.server.EntityServer;
import org.tobi29.scapes.vanilla.basics.VanillaBasics;
import org.tobi29.scapes.vanilla.basics.material.CropType;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;

public class EntityFarmlandServer extends EntityServer {
    private float nutrientA, nutrientB, nutrientC, time;
    private byte stage;
    private CropType cropType;
    private boolean updateBlock;

    public EntityFarmlandServer(WorldServer world) {
        this(world, Vector3d.ZERO, 0.0f, 0.0f, 0.0f);
    }

    public EntityFarmlandServer(WorldServer world, Vector3 pos, float nutrientA,
            float nutrientB, float nutrientC) {
        super(world, pos);
        this.nutrientA = nutrientA;
        this.nutrientB = nutrientB;
        this.nutrientC = nutrientC;
    }

    @Override
    public TagStructure write() {
        TagStructure tag = super.write();
        GameRegistry.Registry<CropType> cropRegistry =
                registry.get("VanillaBasics", "CropType");
        tag.setFloat("NutrientA", nutrientA);
        tag.setFloat("NutrientB", nutrientB);
        tag.setFloat("NutrientC", nutrientC);
        tag.setFloat("Time", time);
        tag.setByte("Stage", stage);
        if (cropType != null) {
            tag.setInteger("CropType", cropRegistry.get(cropType));
        }
        return tag;
    }

    @Override
    public void read(TagStructure tagStructure) {
        super.read(tagStructure);
        GameRegistry.Registry<CropType> cropRegistry =
                registry.get("VanillaBasics", "CropType");
        nutrientA = tagStructure.getFloat("NutrientA");
        nutrientB = tagStructure.getFloat("NutrientB");
        nutrientC = tagStructure.getFloat("NutrientC");
        time = tagStructure.getFloat("Time");
        stage = tagStructure.getByte("Stage");
        if (tagStructure.has("CropType")) {
            cropType = cropRegistry.get(tagStructure.getInteger("CropType"));
            updateBlock = true;
        } else {
            cropType = null;
        }
    }

    @Override
    public void update(double delta) {
        growth(delta);
        if (updateBlock) {
            VanillaBasics plugin =
                    (VanillaBasics) world.plugins().plugin("VanillaBasics");
            GameRegistry.Registry<CropType> cropRegistry =
                    world.registry().get("VanillaBasics", "CropType");
            VanillaMaterial materials = plugin.getMaterials();
            CropType cropType = this.cropType;
            if (cropType == null) {
                world.getTerrain().queue(handler -> handler
                        .typeData(pos.intX(), pos.intY(), pos.intZ() + 1,
                                materials.air, 0));
            } else {
                int stage = this.stage;
                world.getTerrain().queue(handler -> handler
                        .typeData(pos.intX(), pos.intY(), pos.intZ() + 1,
                                materials.crop,
                                stage + (cropRegistry.get(cropType) << 3) - 1));
            }
            updateBlock = false;
        }
    }

    @Override
    public void updateTile(TerrainServer terrain, int x, int y, int z) {
        WorldServer world = terrain.world();
        VanillaBasics plugin =
                (VanillaBasics) world.plugins().plugin("VanillaBasics");
        VanillaMaterial materials = plugin.getMaterials();
        if (terrain.type(pos.intX(), pos.intY(), pos.intZ()) !=
                materials.farmland) {
            world.removeEntity(this);
        } else if (stage > 0 &&
                terrain.type(pos.intX(), pos.intY(), pos.intZ() + 1) !=
                        materials.crop) {
            cropType = null;
        }
    }

    @Override
    public void tickSkip(long oldTick, long newTick) {
        growth((newTick - oldTick) / 20.0f);
    }

    public void seed(CropType cropType) {
        this.cropType = cropType;
        stage = 0;
        time = 0.0f;
    }

    private void growth(double delta) {
        nutrientA = (float) FastMath.min(nutrientA + 0.0000002 * delta, 1.0);
        nutrientB = (float) FastMath.min(nutrientB + 0.0000002 * delta, 1.0);
        nutrientC = (float) FastMath.min(nutrientC + 0.0000002 * delta, 1.0);
        if (cropType == null) {
            stage = 0;
            time = 0.0f;
        } else {
            if (stage < 8) {
                switch (cropType.nutrient()) {
                    case 1:
                        time += nutrientB * delta;
                        nutrientB = (float) FastMath
                                .max(nutrientB - 0.0000005 * delta, 0.0);
                        break;
                    case 2:
                        time += nutrientC * delta;
                        nutrientC = (float) FastMath
                                .max(nutrientC - 0.0000005 * delta, 0.0);
                        break;
                    default:
                        time += nutrientA * delta;
                        nutrientA = (float) FastMath
                                .max(nutrientA - 0.0000005 * delta, 0.0);
                }
                while (time >= cropType.time()) {
                    stage++;
                    if (stage >= 8) {
                        time = 0.0f;
                        stage = 8;
                    } else {
                        time -= cropType.time();
                    }
                    updateBlock = true;
                }
            }
        }
    }
}
