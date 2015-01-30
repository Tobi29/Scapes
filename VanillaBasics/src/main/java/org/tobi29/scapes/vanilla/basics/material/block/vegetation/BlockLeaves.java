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

package org.tobi29.scapes.vanilla.basics.material.block.vegetation;

import org.tobi29.scapes.block.*;
import org.tobi29.scapes.block.models.BlockModel;
import org.tobi29.scapes.block.models.BlockModelComplex;
import org.tobi29.scapes.block.models.BlockModelSimpleBlock;
import org.tobi29.scapes.chunk.data.ChunkMesh;
import org.tobi29.scapes.chunk.terrain.Terrain;
import org.tobi29.scapes.chunk.terrain.TerrainClient;
import org.tobi29.scapes.chunk.terrain.TerrainServer;
import org.tobi29.scapes.engine.opengl.GraphicsSystem;
import org.tobi29.scapes.engine.opengl.shader.Shader;
import org.tobi29.scapes.engine.utils.Pool;
import org.tobi29.scapes.engine.utils.math.AABB;
import org.tobi29.scapes.engine.utils.math.Face;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.vector.MutableVector3;
import org.tobi29.scapes.engine.utils.math.vector.MutableVector3i;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.engine.utils.math.vector.Vector3i;
import org.tobi29.scapes.entity.server.MobPlayerServer;
import org.tobi29.scapes.vanilla.basics.generator.ClimateGenerator;
import org.tobi29.scapes.vanilla.basics.generator.WorldEnvironmentOverworld;
import org.tobi29.scapes.vanilla.basics.material.TreeType;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;
import org.tobi29.scapes.vanilla.basics.material.block.CollisionLeaves;
import org.tobi29.scapes.vanilla.basics.material.block.VanillaBlock;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BlockLeaves extends VanillaBlock {
    private final GameRegistry.Registry<TreeType> treeRegistry;
    private TerrainTexture[] texturesFancy;
    private TerrainTexture[] texturesFast;
    private BlockModel[] modelsFancy;
    private BlockModel[] modelsFast;
    private BlockModel[] modelsColored;

    public BlockLeaves(VanillaMaterial materials,
            GameRegistry.Registry<TreeType> treeRegistry) {
        super(materials, "vanilla.basics.block.Leaves");
        this.treeRegistry = treeRegistry;
    }

    @Override
    public void addCollision(Pool<AABBElement> aabbs, Terrain terrain, int x,
            int y, int z) {
        aabbs.push()
                .set(x, y, z, x + 1, y + 1, z + 1, CollisionLeaves.INSTANCE);
    }

    @Override
    public List<AABBElement> getCollision(int data, int x, int y, int z) {
        List<AABBElement> aabbs = new ArrayList<>();
        aabbs.add(new AABBElement(new AABB(x, y, z, x + 1, y + 1, z + 1),
                CollisionLeaves.INSTANCE));
        return aabbs;
    }

    @Override
    public boolean destroy(TerrainServer.TerrainMutable terrain, int x, int y,
            int z, Face face, MobPlayerServer player, ItemStack item) {
        destroy(terrain, new Vector3i(x, y, z), terrain.getBlockData(x, y, z),
                256);
        return true;
    }

    @Override
    public double getResistance(ItemStack item, int data) {
        return 0.1;
    }

    @Override
    public List<ItemStack> getDrops(ItemStack item, int data) {
        TreeType type = treeRegistry.get(data);
        Random random = new Random();
        List<ItemStack> drops = new ArrayList<>();
        if (random.nextInt(type.getDropChance()) == 0) {
            if (random.nextInt(3) == 0) {
                drops.add(new ItemStack(materials.sapling, data));
            } else {
                drops.add(new ItemStack(materials.stick, (short) 0));
            }
        }
        return drops;
    }

    @Override
    public String getFootStep(int data) {
        return "VanillaBasics:sound/footsteps/Grass.ogg";
    }

    @Override
    public String getBreak(ItemStack item, int data) {
        return "VanillaBasics:sound/blocks/Foliage.ogg";
    }

    @Override
    public float getParticleColorR(Face face, TerrainClient terrain, int x,
            int y, int z) {
        TreeType type = treeRegistry.get(terrain.getBlockData(x, y, z));
        WorldEnvironmentOverworld environment =
                (WorldEnvironmentOverworld) terrain.getWorld().getEnvironment();
        ClimateGenerator climateGenerator = environment.getClimateGenerator();
        double temperature = climateGenerator.getTemperature(x, y, z);
        double mix = FastMath.clamp(temperature / 30.0f, 0.0f, 1.0f);
        Vector3 colorCold = type.getColorCold();
        Vector3 colorWarm = type.getColorWarm();
        double r = FastMath.mix(colorCold.floatX(), colorWarm.floatX(), mix);
        if (!type.isEvergreen()) {
            double autumn = climateGenerator.getAutumnLeaves(y);
            Vector3 colorAutumn = type.getColorAutumn();
            r = FastMath.mix(r, colorAutumn.floatX(), autumn);
        }
        return (float) r;
    }

    @Override
    public float getParticleColorG(Face face, TerrainClient terrain, int x,
            int y, int z) {
        TreeType type = treeRegistry.get(terrain.getBlockData(x, y, z));
        WorldEnvironmentOverworld environment =
                (WorldEnvironmentOverworld) terrain.getWorld().getEnvironment();
        ClimateGenerator climateGenerator = environment.getClimateGenerator();
        double temperature = climateGenerator.getTemperature(x, y, z);
        double mix = FastMath.clamp(temperature / 30.0f, 0.0f, 1.0f);
        Vector3 colorCold = type.getColorCold();
        Vector3 colorWarm = type.getColorWarm();
        double g = FastMath.mix(colorCold.floatY(), colorWarm.floatY(), mix);
        if (!type.isEvergreen()) {
            double autumn = climateGenerator.getAutumnLeaves(y);
            Vector3 colorAutumn = type.getColorAutumn();
            g = FastMath.mix(g, colorAutumn.floatY(), autumn);
        }
        return (float) g;
    }

    @Override
    public float getParticleColorB(Face face, TerrainClient terrain, int x,
            int y, int z) {
        TreeType type = treeRegistry.get(terrain.getBlockData(x, y, z));
        WorldEnvironmentOverworld environment =
                (WorldEnvironmentOverworld) terrain.getWorld().getEnvironment();
        ClimateGenerator climateGenerator = environment.getClimateGenerator();
        double temperature = climateGenerator.getTemperature(x, y, z);
        double mix = FastMath.clamp(temperature / 30.0f, 0.0f, 1.0f);
        Vector3 colorCold = type.getColorCold();
        Vector3 colorWarm = type.getColorWarm();
        double b = FastMath.mix(colorCold.floatZ(), colorWarm.floatZ(), mix);
        if (!type.isEvergreen()) {
            double autumn = climateGenerator.getAutumnLeaves(y);
            Vector3 colorAutumn = type.getColorAutumn();
            b = FastMath.mix(b, colorAutumn.floatZ(), autumn);
        }
        return (float) b;
    }

    @Override
    public TerrainTexture getParticleTexture(Face face, TerrainClient terrain,
            int x, int y, int z) {
        return texturesFancy[terrain.getBlockData(x, y, z)];
    }

    @Override
    public boolean isTransparent(Terrain terrain, int x, int y, int z) {
        return true;
    }

    @Override
    public byte lightTrough(Terrain terrain, int x, int y, int z) {
        return -2;
    }

    @Override
    public short connectStage(TerrainClient terrain, int x, int y, int z) {
        return 3;
    }

    @Override
    public void addToChunkMesh(ChunkMesh mesh, ChunkMesh meshAlpha, int data,
            TerrainClient terrain, int x, int y, int z, float xx, float yy,
            float zz, boolean lod) {
        TreeType type = treeRegistry.get(data);
        WorldEnvironmentOverworld environment =
                (WorldEnvironmentOverworld) terrain.getWorld().getEnvironment();
        ClimateGenerator climateGenerator = environment.getClimateGenerator();
        double temperature = climateGenerator.getTemperature(x, y, z);
        double mix = FastMath.clamp((temperature + 20.0f) / 50.0f, 0.0f, 1.0f);
        Vector3 colorCold = type.getColorCold();
        Vector3 colorWarm = type.getColorWarm();
        double r = FastMath.mix(colorCold.floatX(), colorWarm.floatX(), mix);
        double g = FastMath.mix(colorCold.floatY(), colorWarm.floatY(), mix);
        double b = FastMath.mix(colorCold.floatZ(), colorWarm.floatZ(), mix);
        if (!type.isEvergreen()) {
            double autumn = climateGenerator.getAutumnLeaves(y);
            Vector3 colorAutumn = type.getColorAutumn();
            r = FastMath.mix(r, colorAutumn.floatX(), autumn);
            g = FastMath.mix(g, colorAutumn.floatY(), autumn);
            b = FastMath.mix(b, colorAutumn.floatZ(), autumn);
        }
        if (lod && !isCovered(terrain, x, y, z)) {
            modelsFancy[data].addToChunkMesh(mesh, terrain, x, y, z, xx, yy, zz,
                    (float) r, (float) g, (float) b, 1.0f);
        } else {
            modelsFast[data].addToChunkMesh(mesh, terrain, x, y, z, xx, yy, zz,
                    (float) r, (float) g, (float) b, 1.0f);
        }
    }

    @Override
    public boolean needsLodUpdate(int data, TerrainClient terrain, int x, int y,
            int z) {
        return true;
    }

    @Override
    public void registerTextures(TerrainTextureRegistry registry) {
        List<TreeType> types = treeRegistry.values();
        texturesFancy = new TerrainTexture[types.size()];
        texturesFast = new TerrainTexture[types.size()];
        for (int i = 0; i < types.size(); i++) {
            TreeType type = types.get(i);
            String texture = type.getTexture();
            String textureRoot = type.getTextureRoot();
            texturesFancy[i] =
                    registry.registerTexture(textureRoot + "/leaves/fancy/" +
                            texture + ".png", ShaderAnimation.LEAVE);
            texturesFast[i] =
                    registry.registerTexture(textureRoot + "/leaves/fast/" +
                            texture + ".png");
        }
    }

    @Override
    public void createModels(TerrainTextureRegistry registry) {
        List<TreeType> types = treeRegistry.values();
        modelsFancy = new BlockModel[types.size()];
        modelsFast = new BlockModel[types.size()];
        modelsColored = new BlockModel[types.size()];
        for (int i = 0; i < types.size(); i++) {
            TreeType type = treeRegistry.get(i);
            Vector3 colorCold = type.getColorCold();
            Vector3 colorWarm = type.getColorWarm();
            float r =
                    FastMath.mix(colorCold.floatX(), colorWarm.floatX(), 0.5f);
            float g =
                    FastMath.mix(colorCold.floatY(), colorWarm.floatY(), 0.5f);
            float b =
                    FastMath.mix(colorCold.floatZ(), colorWarm.floatZ(), 0.5f);
            List<BlockModelComplex.Shape> shapes;
            BlockModelComplex.Shape shape;
            shapes = new ArrayList<>();
            shape = new BlockModelComplex.ShapeBox(texturesFancy[i],
                    texturesFancy[i], texturesFancy[i], texturesFancy[i],
                    texturesFancy[i], texturesFancy[i], -8, -8, -8, 8, 8, 8,
                    1.0f, 1.0f, 1.0f, 1.0f);
            shapes.add(shape);
            modelsFancy[i] = new BlockModelComplex(registry, shapes, 0.0625f);
            modelsFast[i] =
                    new BlockModelSimpleBlock(this, registry, texturesFast[i],
                            texturesFast[i], texturesFast[i], texturesFast[i],
                            texturesFast[i], texturesFast[i], 1.0f, 1.0f, 1.0f,
                            1.0f);
            shapes = new ArrayList<>();
            shape = new BlockModelComplex.ShapeBox(texturesFancy[i],
                    texturesFancy[i], texturesFancy[i], texturesFancy[i],
                    texturesFancy[i], texturesFancy[i], -8, -8, -8, 8, 8, 8, r,
                    g, b, 1.0f);
            shapes.add(shape);
            modelsColored[i] = new BlockModelComplex(registry, shapes, 0.0625f);
        }
    }

    @Override
    public void render(ItemStack item, GraphicsSystem graphics, Shader shader,
            float r, float g, float b, float a) {
        modelsColored[item.getData()].render(graphics, shader);
    }

    @Override
    public void renderInventory(ItemStack item, GraphicsSystem graphics,
            Shader shader, float r, float g, float b, float a) {
        modelsColored[item.getData()].renderInventory(graphics, shader);
    }

    @Override
    public String getName(ItemStack item) {
        return materials.log.getName(item) + " Leaves";
    }

    @Override
    public int getStackSize(ItemStack item) {
        return 16;
    }

    private void destroy(TerrainServer.TerrainMutable terrain, Vector3 pos,
            int data, int length) {
        if (terrain.getBlockType(pos.intX(), pos.intY(), pos.intZ()) != this ||
                terrain.getBlockData(pos.intX(), pos.intY(), pos.intZ()) !=
                        data) {
            return;
        }
        Pool<MutableVector3> checks = new Pool<>(MutableVector3i::new),
                checks2 = new Pool<>(MutableVector3i::new),
                checksSwap;
        checks.push().set(pos);
        for (int i = 0; i < 5 && !checks.isEmpty(); i++) {
            for (MutableVector3 check : checks) {
                if (terrain.getBlockData(check.intX(), check.intY(),
                        check.intZ()) == data) {
                    if (terrain.getBlockType(check.intX(), check.intY(),
                            check.intZ()) == materials.log) {
                        return;
                    }
                    if (i < 10 &&
                            terrain.getBlockType(check.intX(), check.intY(),
                                    check.intZ()) == this) {
                        checks2.push().set(check.intX(), check.intY(),
                                check.intZ() + 1);
                        checks2.push().set(check.intX(), check.intY(),
                                check.intZ() - 1);
                        checks2.push().set(check.intX(), check.intY() - 1,
                                check.intZ());
                        checks2.push().set(check.intX() + 1, check.intY(),
                                check.intZ());
                        checks2.push().set(check.intX(), check.intY() + 1,
                                check.intZ());
                        checks2.push().set(check.intX() - 1, check.intY(),
                                check.intZ());
                    }
                }
            }
            checks.reset();
            checksSwap = checks;
            checks = checks2;
            checks2 = checksSwap;
        }
        terrain.getWorld().dropItems(
                getDrops(new ItemStack(materials.air, (short) 0),
                        terrain.getBlockData(pos.intX(), pos.intY(),
                                pos.intZ())), pos.intX(), pos.intY(),
                pos.intZ());
        terrain.setBlockTypeAndData(pos.intX(), pos.intY(), pos.intZ(),
                materials.air, (short) 0);
        if (length-- > 0) {
            destroy(terrain, pos.plus(new Vector3i(-1, -1, -1)), data, length);
            destroy(terrain, pos.plus(new Vector3i(0, -1, -1)), data, length);
            destroy(terrain, pos.plus(new Vector3i(1, -1, -1)), data, length);
            destroy(terrain, pos.plus(new Vector3i(-1, 0, -1)), data, length);
            destroy(terrain, pos.plus(new Vector3i(0, 0, -1)), data, length);
            destroy(terrain, pos.plus(new Vector3i(1, 0, -1)), data, length);
            destroy(terrain, pos.plus(new Vector3i(-1, 1, -1)), data, length);
            destroy(terrain, pos.plus(new Vector3i(0, 1, -1)), data, length);
            destroy(terrain, pos.plus(new Vector3i(1, 1, -1)), data, length);
            destroy(terrain, pos.plus(new Vector3i(-1, -1, 0)), data, length);
            destroy(terrain, pos.plus(new Vector3i(0, -1, 0)), data, length);
            destroy(terrain, pos.plus(new Vector3i(1, -1, 0)), data, length);
            destroy(terrain, pos.plus(new Vector3i(-1, 0, 0)), data, length);
            destroy(terrain, pos.plus(new Vector3i(0, 0, 0)), data, length);
            destroy(terrain, pos.plus(new Vector3i(1, 0, 0)), data, length);
            destroy(terrain, pos.plus(new Vector3i(-1, 1, 0)), data, length);
            destroy(terrain, pos.plus(new Vector3i(0, 1, 0)), data, length);
            destroy(terrain, pos.plus(new Vector3i(1, 1, 0)), data, length);
            destroy(terrain, pos.plus(new Vector3i(-1, -1, 1)), data, length);
            destroy(terrain, pos.plus(new Vector3i(0, -1, 1)), data, length);
            destroy(terrain, pos.plus(new Vector3i(1, -1, 1)), data, length);
            destroy(terrain, pos.plus(new Vector3i(-1, 0, 1)), data, length);
            destroy(terrain, pos.plus(new Vector3i(0, 0, 1)), data, length);
            destroy(terrain, pos.plus(new Vector3i(1, 0, 1)), data, length);
            destroy(terrain, pos.plus(new Vector3i(-1, 1, 1)), data, length);
            destroy(terrain, pos.plus(new Vector3i(0, 1, 1)), data, length);
            destroy(terrain, pos.plus(new Vector3i(1, 1, 1)), data, length);
        }
    }

    private boolean isCovered(Terrain terrain, int x, int y, int z) {
        for (Face face : Face.values()) {
            int xx = x + face.getX();
            int yy = y + face.getY();
            int zz = z + face.getZ();
            if (terrain.getBlockType(xx, yy, zz)
                    .isReplaceable(terrain, xx, yy, zz)) {
                return false;
            }
        }
        return true;
    }
}
