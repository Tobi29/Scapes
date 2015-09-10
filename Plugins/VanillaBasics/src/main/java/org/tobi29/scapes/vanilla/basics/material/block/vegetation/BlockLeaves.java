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
import org.tobi29.scapes.chunk.EnvironmentClient;
import org.tobi29.scapes.chunk.data.ChunkMesh;
import org.tobi29.scapes.chunk.terrain.Terrain;
import org.tobi29.scapes.chunk.terrain.TerrainClient;
import org.tobi29.scapes.chunk.terrain.TerrainRenderInfo;
import org.tobi29.scapes.chunk.terrain.TerrainServer;
import org.tobi29.scapes.engine.opengl.GL;
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
import org.tobi29.scapes.vanilla.basics.generator.ClimateInfoLayer;
import org.tobi29.scapes.vanilla.basics.generator.EnvironmentClimate;
import org.tobi29.scapes.vanilla.basics.material.TreeType;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;
import org.tobi29.scapes.vanilla.basics.material.block.CollisionLeaves;
import org.tobi29.scapes.vanilla.basics.material.block.VanillaBlock;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
    public List<AABBElement> collision(int data, int x, int y, int z) {
        List<AABBElement> aabbs = new ArrayList<>();
        aabbs.add(new AABBElement(new AABB(x, y, z, x + 1, y + 1, z + 1),
                CollisionLeaves.INSTANCE));
        return aabbs;
    }

    @Override
    public boolean destroy(TerrainServer.TerrainMutable terrain, int x, int y,
            int z, Face face, MobPlayerServer player, ItemStack item) {
        destroy(terrain, new Vector3i(x, y, z), terrain.data(x, y, z), 256);
        return true;
    }

    @Override
    public double resistance(ItemStack item, int data) {
        return 0.1;
    }

    @Override
    public List<ItemStack> drops(ItemStack item, int data) {
        TreeType type = treeRegistry.get(data);
        Random random = new Random();
        List<ItemStack> drops = new ArrayList<>();
        if (random.nextInt(type.dropChance()) == 0) {
            if (random.nextInt(3) == 0) {
                drops.add(new ItemStack(materials.sapling, data));
            } else {
                drops.add(new ItemStack(materials.stick, (short) 0));
            }
        }
        return drops;
    }

    @Override
    public String footStepSound(int data) {
        return "VanillaBasics:sound/footsteps/Grass.ogg";
    }

    @Override
    public String breakSound(ItemStack item, int data) {
        return "VanillaBasics:sound/blocks/Foliage.ogg";
    }

    @Override
    public float particleColorR(Face face, TerrainClient terrain, int x, int y,
            int z) {
        EnvironmentClient environment = terrain.world().environment();
        if (environment instanceof EnvironmentClimate) {
            EnvironmentClimate environmentClimate =
                    (EnvironmentClimate) environment;
            ClimateGenerator climateGenerator = environmentClimate.climate();
            TreeType type = treeRegistry.get(terrain.data(x, y, z));
            double temperature = climateGenerator.temperature(x, y, z);
            double mix = FastMath.clamp(temperature / 30.0f, 0.0f, 1.0f);
            Vector3 colorCold = type.colorCold();
            Vector3 colorWarm = type.colorWarm();
            double r =
                    FastMath.mix(colorCold.floatX(), colorWarm.floatX(), mix);
            if (!type.isEvergreen()) {
                double autumn = climateGenerator.autumnLeaves(y);
                Vector3 colorAutumn = type.colorAutumn();
                r = FastMath.mix(r, colorAutumn.floatX(), autumn);
            }
            return (float) r;
        }
        return 1.0f;
    }

    @Override
    public float particleColorG(Face face, TerrainClient terrain, int x, int y,
            int z) {
        EnvironmentClient environment = terrain.world().environment();
        if (environment instanceof EnvironmentClimate) {
            EnvironmentClimate environmentClimate =
                    (EnvironmentClimate) environment;
            ClimateGenerator climateGenerator = environmentClimate.climate();
            TreeType type = treeRegistry.get(terrain.data(x, y, z));
            double temperature = climateGenerator.temperature(x, y, z);
            double mix = FastMath.clamp(temperature / 30.0f, 0.0f, 1.0f);
            Vector3 colorCold = type.colorCold();
            Vector3 colorWarm = type.colorWarm();
            double g =
                    FastMath.mix(colorCold.floatY(), colorWarm.floatY(), mix);
            if (!type.isEvergreen()) {
                double autumn = climateGenerator.autumnLeaves(y);
                Vector3 colorAutumn = type.colorAutumn();
                g = FastMath.mix(g, colorAutumn.floatY(), autumn);
            }
            return (float) g;
        }
        return 1.0f;
    }

    @Override
    public float particleColorB(Face face, TerrainClient terrain, int x, int y,
            int z) {
        EnvironmentClient environment = terrain.world().environment();
        if (environment instanceof EnvironmentClimate) {
            EnvironmentClimate environmentClimate =
                    (EnvironmentClimate) environment;
            ClimateGenerator climateGenerator = environmentClimate.climate();
            TreeType type = treeRegistry.get(terrain.data(x, y, z));
            double temperature = climateGenerator.temperature(x, y, z);
            double mix = FastMath.clamp(temperature / 30.0f, 0.0f, 1.0f);
            Vector3 colorCold = type.colorCold();
            Vector3 colorWarm = type.colorWarm();
            double b =
                    FastMath.mix(colorCold.floatZ(), colorWarm.floatZ(), mix);
            if (!type.isEvergreen()) {
                double autumn = climateGenerator.autumnLeaves(y);
                Vector3 colorAutumn = type.colorAutumn();
                b = FastMath.mix(b, colorAutumn.floatZ(), autumn);
            }
            return (float) b;
        }
        return 1.0f;
    }

    @Override
    public Optional<TerrainTexture> particleTexture(Face face,
            TerrainClient terrain, int x, int y, int z) {
        return Optional.of(texturesFancy[terrain.data(x, y, z)]);
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
            TerrainClient terrain, TerrainRenderInfo info, int x, int y, int z,
            float xx, float yy, float zz, boolean lod) {
        if (!isCovered(terrain, x, y, z)) {
            TreeType type = treeRegistry.get(data);
            ClimateInfoLayer climateLayer = info.get("VanillaBasics:Climate");
            double temperature = climateLayer.temperature(x, y, z);
            double mix =
                    FastMath.clamp((temperature + 20.0f) / 50.0f, 0.0f, 1.0f);
            Vector3 colorCold = type.colorCold();
            Vector3 colorWarm = type.colorWarm();
            double r =
                    FastMath.mix(colorCold.floatX(), colorWarm.floatX(), mix);
            double g =
                    FastMath.mix(colorCold.floatY(), colorWarm.floatY(), mix);
            double b =
                    FastMath.mix(colorCold.floatZ(), colorWarm.floatZ(), mix);
            if (!type.isEvergreen()) {
                EnvironmentClient environment = terrain.world().environment();
                if (environment instanceof EnvironmentClimate) {
                    EnvironmentClimate environmentClimate =
                            (EnvironmentClimate) environment;
                    ClimateGenerator climateGenerator =
                            environmentClimate.climate();
                    double autumn = climateGenerator.autumnLeaves(y);
                    Vector3 colorAutumn = type.colorAutumn();
                    r = FastMath.mix(r, colorAutumn.floatX(), autumn);
                    g = FastMath.mix(g, colorAutumn.floatY(), autumn);
                    b = FastMath.mix(b, colorAutumn.floatZ(), autumn);
                }
            }
            if (lod) {
                modelsFancy[data]
                        .addToChunkMesh(mesh, terrain, x, y, z, xx, yy, zz,
                                (float) r, (float) g, (float) b, 1.0f, lod);
            } else {
                modelsFast[data]
                        .addToChunkMesh(mesh, terrain, x, y, z, xx, yy, zz,
                                (float) r, (float) g, (float) b, 1.0f, lod);
            }
        }
    }

    @Override
    public void registerTextures(TerrainTextureRegistry registry) {
        List<TreeType> types = treeRegistry.values();
        texturesFancy = new TerrainTexture[types.size()];
        texturesFast = new TerrainTexture[types.size()];
        for (int i = 0; i < types.size(); i++) {
            String texture = types.get(i).texture();
            texturesFancy[i] =
                    registry.registerTexture(texture + "/LeavesFancy.png",
                            ShaderAnimation.LEAVES);
            texturesFast[i] =
                    registry.registerTexture(texture + "/LeavesFast.png");
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
            Vector3 colorCold = type.colorCold();
            Vector3 colorWarm = type.colorWarm();
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
    public void render(ItemStack item, GL gl, Shader shader, float r, float g,
            float b, float a) {
        modelsColored[item.data()].render(gl, shader);
    }

    @Override
    public void renderInventory(ItemStack item, GL gl, Shader shader, float r,
            float g, float b, float a) {
        modelsColored[item.data()].renderInventory(gl, shader);
    }

    @Override
    public String name(ItemStack item) {
        return materials.log.name(item) + " Leaves";
    }

    @Override
    public int maxStackSize(ItemStack item) {
        return 16;
    }

    private void destroy(TerrainServer.TerrainMutable terrain, Vector3 pos,
            int data, int length) {
        if (terrain.type(pos.intX(), pos.intY(), pos.intZ()) != this ||
                terrain.data(pos.intX(), pos.intY(), pos.intZ()) != data) {
            return;
        }
        Pool<MutableVector3> checks = new Pool<>(MutableVector3i::new),
                checks2 = new Pool<>(MutableVector3i::new),
                checksSwap;
        checks.push().set(pos);
        for (int i = 0; i < 5 && !checks.isEmpty(); i++) {
            for (MutableVector3 check : checks) {
                if (terrain.data(check.intX(), check.intY(), check.intZ()) ==
                        data) {
                    if (terrain
                            .type(check.intX(), check.intY(), check.intZ()) ==
                            materials.log) {
                        return;
                    }
                    if (i < 10 && terrain.type(check.intX(), check.intY(),
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
        terrain.world().dropItems(drops(new ItemStack(materials.air, (short) 0),
                        terrain.data(pos.intX(), pos.intY(), pos.intZ())),
                pos.intX(), pos.intY(), pos.intZ());
        terrain.typeData(pos.intX(), pos.intY(), pos.intZ(), materials.air,
                (short) 0);
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
            if (terrain.type(xx, yy, zz).isReplaceable(terrain, xx, yy, zz)) {
                return false;
            }
        }
        return true;
    }
}
