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

package org.tobi29.scapes.vanilla.basics.generator;

import org.tobi29.scapes.block.BlockType;
import org.tobi29.scapes.block.Inventory;
import org.tobi29.scapes.block.Material;
import org.tobi29.scapes.chunk.*;
import org.tobi29.scapes.chunk.generator.ChunkGenerator;
import org.tobi29.scapes.chunk.generator.ChunkPopulator;
import org.tobi29.scapes.chunk.terrain.TerrainServer;
import org.tobi29.scapes.chunk.terrain.infinite.TerrainInfiniteChunk;
import org.tobi29.scapes.chunk.terrain.infinite.TerrainInfiniteServer;
import org.tobi29.scapes.engine.opengl.GL;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.engine.utils.math.vector.Vector3d;
import org.tobi29.scapes.engine.utils.math.vector.Vector3i;
import org.tobi29.scapes.entity.CreatureType;
import org.tobi29.scapes.entity.server.*;
import org.tobi29.scapes.packets.PacketEntityMetaData;
import org.tobi29.scapes.packets.PacketUpdateInventory;
import org.tobi29.scapes.vanilla.basics.VanillaBasics;
import org.tobi29.scapes.vanilla.basics.entity.server.EntityTornadoServer;
import org.tobi29.scapes.vanilla.basics.entity.server.MobPigServer;
import org.tobi29.scapes.vanilla.basics.entity.server.MobSkeletonServer;
import org.tobi29.scapes.vanilla.basics.entity.server.MobZombieServer;
import org.tobi29.scapes.vanilla.basics.material.VanillaMaterial;
import org.tobi29.scapes.vanilla.basics.material.item.ItemHeatable;
import org.tobi29.scapes.vanilla.basics.packet.PacketDayTimeSync;
import org.tobi29.scapes.vanilla.basics.packet.PacketLightning;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class WorldEnvironmentOverworld implements WorldEnvironment {
    private final World world;
    private final VanillaMaterial materials;
    private final ChunkGeneratorOverworld gen;
    private final ChunkPopulatorOverworld pop;
    private final ClimateGenerator climateGenerator;
    private final BiomeGenerator biomeGenerator;
    private long simulationCount;
    private double syncWait = 2.0, playerUpdateWait = 0.25, itemUpdateWait =
            1.0, tickWait = 0.05;

    public WorldEnvironmentOverworld(World world, VanillaBasics plugin) {
        this.world = world;
        materials = plugin.getMaterials();
        {
            Random random = new Random(world.getSeed());
            TerrainGenerator terrainGenerator = new TerrainGenerator(random);
            climateGenerator = new ClimateGenerator(random, terrainGenerator);
            biomeGenerator = new BiomeGenerator(climateGenerator);
            gen = new ChunkGeneratorOverworld(random, terrainGenerator,
                    plugin.getMaterials());
        }
        pop = new ChunkPopulatorOverworld(world, plugin, biomeGenerator);
        if (world instanceof WorldServer) {
            WorldServer worldServer = (WorldServer) world;
            worldServer.addSpawner(new MobSpawner() {
                @Override
                public double getMobsPerChunk() {
                    return 0.1;
                }

                @Override
                public int getSpawnAttempts() {
                    return 1;
                }

                @Override
                public int getChunkChance() {
                    return 2;
                }

                @Override
                public boolean canSpawn(TerrainServer terrain, int x, int y,
                        int z) {
                    if (terrain.light(x, y, z) < 7) {
                        if (!terrain.type(x, y, z).isSolid(terrain, x, y, z) &&
                                terrain.type(x, y, z)
                                        .isTransparent(terrain, x, y, z) &&
                                !terrain.type(x, y, z + 1)
                                        .isSolid(terrain, x, y, z + 1) &&
                                terrain.type(x, y, z + 1)
                                        .isTransparent(terrain, x, y, z + 1) &&
                                terrain.type(x, y, z - 1)
                                        .isSolid(terrain, x, y, z - 1) &&
                                !terrain.type(x, y, z - 1)
                                        .isTransparent(terrain, x, y, z - 1)) {
                            return true;
                        }
                    }
                    return false;
                }

                @Override
                public MobServer spawn(TerrainServer terrain, int x, int y,
                        int z) {
                    Random random = ThreadLocalRandom.current();
                    return new MobZombieServer(terrain.world(),
                            new Vector3d(x + 0.5, y + 0.5, z + 1.0),
                            Vector3d.ZERO, 0.0, random.nextDouble() * 360.0);
                }

                @Override
                public CreatureType getCreatureType() {
                    return CreatureType.MONSTER;
                }
            });
            worldServer.addSpawner(new MobSpawner() {
                @Override
                public double getMobsPerChunk() {
                    return 0.1;
                }

                @Override
                public int getSpawnAttempts() {
                    return 1;
                }

                @Override
                public int getChunkChance() {
                    return 2;
                }

                @Override
                public boolean canSpawn(TerrainServer terrain, int x, int y,
                        int z) {
                    if (terrain.light(x, y, z) < 7) {
                        if (!terrain.type(x, y, z).isSolid(terrain, x, y, z) &&
                                terrain.type(x, y, z)
                                        .isTransparent(terrain, x, y, z) &&
                                !terrain.type(x, y, z + 1)
                                        .isSolid(terrain, x, y, z + 1) &&
                                terrain.type(x, y, z + 1)
                                        .isTransparent(terrain, x, y, z + 1) &&
                                terrain.type(x, y, z - 1)
                                        .isSolid(terrain, x, y, z - 1) &&
                                !terrain.type(x, y, z - 1)
                                        .isTransparent(terrain, x, y, z - 1)) {
                            return true;
                        }
                    }
                    return false;
                }

                @Override
                public MobServer spawn(TerrainServer terrain, int x, int y,
                        int z) {
                    Random random = ThreadLocalRandom.current();
                    return new MobSkeletonServer(terrain.world(),
                            new Vector3d(x + 0.5, y + 0.5, z + 1.0),
                            Vector3d.ZERO, 0, random.nextDouble() * 360.0);
                }

                @Override
                public CreatureType getCreatureType() {
                    return CreatureType.MONSTER;
                }
            });
            worldServer.addSpawner(new MobSpawner() {
                @Override
                public double getMobsPerChunk() {
                    return 0.01;
                }

                @Override
                public int getChunkChance() {
                    return 10;
                }

                @Override
                public int getSpawnAttempts() {
                    return 1;
                }

                @Override
                public boolean canSpawn(TerrainServer terrain, int x, int y,
                        int z) {
                    if (terrain.light(x, y, z) >= 7) {
                        if (!terrain.type(x, y, z).isSolid(terrain, x, y, z) &&
                                terrain.type(x, y, z)
                                        .isTransparent(terrain, x, y, z) &&
                                terrain.type(x, y, z - 1) == materials.grass) {
                            return true;
                        }
                    }
                    return false;
                }

                @Override
                public MobServer spawn(TerrainServer terrain, int x, int y,
                        int z) {
                    Random random = ThreadLocalRandom.current();
                    return new MobPigServer(terrain.world(),
                            new Vector3d(x + 0.5, y + 0.5, z + 0.6875),
                            Vector3d.ZERO, 0, random.nextDouble() * 360.0);
                }

                @Override
                public CreatureType getCreatureType() {
                    return CreatureType.CREATURE;
                }
            });
            worldServer.entityListener(entity -> {
                if (!(entity instanceof MobPlayerServer)) {
                    return;
                }
                MobPlayerServer player = (MobPlayerServer) entity;
                player.listener("VanillaBasics:Condition",
                        (EntityServer.SpawnListener) () -> {
                            TagStructure conditionTag =
                                    player.getMetaData("Vanilla")
                                            .getStructure("Condition");
                            synchronized (conditionTag) {
                                conditionTag.setDouble("Stamina", 1.0);
                                conditionTag.setDouble("Hunger", 1.0);
                                conditionTag.setDouble("Thirst", 1.0);
                                conditionTag.setDouble("BodyTemperature", 37.0);
                            }
                        });
                player.listener("VanillaBasics:Condition",
                        (MobLivingServer.JumpListener) () -> {
                            TagStructure conditionTag =
                                    player.getMetaData("Vanilla")
                                            .getStructure("Condition");
                            synchronized (conditionTag) {
                                double stamina =
                                        conditionTag.getDouble("Stamina");
                                double bodyTemperature = conditionTag
                                        .getDouble("BodyTemperature");
                                conditionTag
                                        .setDouble("Stamina", stamina - 0.1);
                                conditionTag.setDouble("BodyTemperature",
                                        bodyTemperature + 0.1);
                            }
                        });
                player.listener("VanillaBasics:Condition",
                        (MobPlayerServer.PunchListener) strength -> {
                            TagStructure conditionTag =
                                    player.getMetaData("Vanilla")
                                            .getStructure("Condition");
                            synchronized (conditionTag) {
                                double stamina =
                                        conditionTag.getDouble("Stamina");
                                double bodyTemperature = conditionTag
                                        .getDouble("BodyTemperature");
                                conditionTag.setDouble("Stamina",
                                        stamina - 0.1 * strength);
                                conditionTag.setDouble("BodyTemperature",
                                        bodyTemperature + 0.05 * strength);
                            }
                        });
                player.listener("VanillaBasics:DeathMessage",
                        (MobLivingServer.DeathListener) () -> worldServer
                                .getConnection()
                                .chat(player.getNickname() + " died!"));
            });
        }
    }

    public ClimateGenerator getClimateGenerator() {
        return climateGenerator;
    }

    @Override
    public ChunkGenerator getGenerator() {
        return gen;
    }

    @Override
    public ChunkPopulator getPopulator() {
        return pop;
    }

    @Override
    public Vector3 calculateSpawn(TerrainServer terrain) {
        int y = -22500;
        int x = -16;
        int z = 0;
        while (z < 256) {
            x += 16;
            boolean flag = false;
            while (!flag) {
                double temperature = climateGenerator.getTemperature(x, y);
                if (temperature < 10.0 || temperature > 15.0 ||
                        !gen.isValidSpawn(x, y)) {
                    x += 512;
                } else {
                    flag = true;
                }
            }
            z = terrain.getHighestTerrainBlockZAt(x, y);
        }
        return new Vector3i(x, y, z);
    }

    @Override
    public void load(TagStructure tagStructure) {
        if (tagStructure.has("DayTime")) {
            climateGenerator.setDayTime(tagStructure.getDouble("DayTime"));
        } else {
            climateGenerator.setDayTime(0.1);
        }
        if (tagStructure.has("Day")) {
            climateGenerator.setDay(tagStructure.getLong("Day"));
        } else {
            climateGenerator.setDay(4);
        }
        simulationCount = tagStructure.getLong("SimulationCount");
    }

    @Override
    public TagStructure save() {
        TagStructure tagStructure = new TagStructure();
        tagStructure.setDouble("DayTime", climateGenerator.getDayTime());
        tagStructure.setLong("Day", climateGenerator.getDay());
        tagStructure.setLong("SimulationCount", simulationCount);
        return tagStructure;
    }

    @Override
    public void tick(double delta) {
        climateGenerator.add(0.000277777777778 * delta);
        if (world instanceof WorldServer) {
            WorldServer worldServer = (WorldServer) world;
            playerUpdateWait -= delta;
            while (playerUpdateWait <= 0.0) {
                playerUpdateWait += 0.25;
                worldServer.getPlayers().forEach(player -> {
                    double health = player.getLives();
                    double maxHealth = player.getMaxLives();
                    TagStructure conditionTag = player.getMetaData("Vanilla")
                            .getStructure("Condition");
                    synchronized (conditionTag) {
                        double stamina = conditionTag.getDouble("Stamina");
                        double hunger = conditionTag.getDouble("Hunger");
                        double thirst = conditionTag.getDouble("Thirst");
                        double bodyTemperature =
                                conditionTag.getDouble("BodyTemperature");
                        boolean ground = player.isOnGround();
                        boolean inWater = player.isInWater();
                        Vector3 pos = player.getPos();
                        double temperature = climateGenerator
                                .getTemperature(pos.intX(), pos.intY(),
                                        pos.intZ());
                        if (stamina > 0.2 && health < maxHealth) {
                            double rate = stamina * 0.5;
                            player.heal(rate);
                            stamina -= rate * 0.1;
                        }
                        if (inWater) {
                            double rate = FastMath.clamp(
                                    FastMath.lengthSqr(player.getSpeed()) *
                                            0.00125, 0.0, 0.05);
                            stamina -= rate;
                            bodyTemperature += rate;
                            thirst -= rate * 0.075;
                        } else if (ground) {
                            double rate = FastMath.clamp(
                                    FastMath.lengthSqr(player.getSpeed()) *
                                            0.00025, 0.0, 0.05);
                            stamina -= rate;
                            bodyTemperature += rate;
                        }
                        stamina -= 0.00025;
                        if (inWater && thirst < 1.0) {
                            thirst += 0.025;
                        }
                        if (stamina < 1.0) {
                            double rate =
                                    hunger * thirst * 0.05 * (1 - stamina);
                            stamina += rate;
                            hunger -= rate * 0.004;
                            thirst -= rate * 0.01;
                        }
                        bodyTemperature +=
                                (temperature - bodyTemperature) / 2000.0;
                        if (bodyTemperature < 37.0) {
                            double rate =
                                    FastMath.max(37.0 - bodyTemperature, 0.0);
                            rate = FastMath.min(rate * 8.0 * stamina, 1.0) *
                                    0.04;
                            bodyTemperature += rate;
                            stamina -= rate * 0.5;
                        } else if (bodyTemperature > 37.0) {
                            double rate =
                                    FastMath.max(bodyTemperature - 37.0, 0.0);
                            rate = FastMath.min(rate * thirst, 1.0) * 0.06;
                            bodyTemperature -= rate;
                            thirst -= rate * 0.05;
                        }
                        hunger = FastMath.clamp(hunger, 0.0, 1.0);
                        thirst = FastMath.clamp(thirst, 0.0, 1.0);
                        stamina = FastMath.min(stamina, 1.0);
                        if (stamina <= 0.0) {
                            player.damage(5.0);
                        }
                        conditionTag.setDouble("Stamina", stamina);
                        conditionTag.setDouble("Hunger", hunger);
                        conditionTag.setDouble("Thirst", thirst);
                        conditionTag
                                .setDouble("BodyTemperature", bodyTemperature);
                    }
                    player.getConnection()
                            .send(new PacketEntityMetaData(player, "Vanilla"));
                });
            }
            itemUpdateWait -= delta;
            while (itemUpdateWait <= 0.0) {
                itemUpdateWait += 1.0;
                worldServer.getEntities().forEach(entity -> {
                    if (entity instanceof EntityContainerServer) {
                        Inventory inventory =
                                ((EntityContainerServer) entity).getInventory();
                        boolean flag = false;
                        for (int i = 0; i < inventory.getSize(); i++) {
                            Material type = inventory.getItem(i).getMaterial();
                            if (type instanceof ItemHeatable) {
                                ((ItemHeatable) type)
                                        .cool(inventory.getItem(i));
                                flag = true;
                            }
                        }
                        if (flag) {
                            ((EntityContainerServer) entity).getViewers()
                                    .forEach(viewer -> viewer.getConnection()
                                            .send(new PacketUpdateInventory(
                                                    (EntityContainerServer) entity)));
                        }
                    } else if (entity instanceof MobItemServer) {
                        Material type = ((MobItemServer) entity).getItem()
                                .getMaterial();
                        if (type instanceof ItemHeatable) {
                            ((ItemHeatable) type).cool((MobItemServer) entity);
                        }
                    }
                });
            }
            syncWait -= delta;
            while (syncWait <= 0.0) {
                syncWait += 4.0;
                world.getConnection().send(new PacketDayTimeSync(
                        climateGenerator.getDayTime(),
                        climateGenerator.getDay()));
            }
            tickWait -= delta;
            while (tickWait <= 0.0) {
                tickWait += 0.05;
                simulationCount++;
                if (simulationCount >= Long.MAX_VALUE - 10) {
                    simulationCount = 0;
                }
                TerrainInfiniteServer terrain =
                        (TerrainInfiniteServer) worldServer.getTerrain();
                for (TerrainInfiniteChunk chunk : terrain.getLoadedChunks()) {
                    Random random = ThreadLocalRandom.current();
                    if (chunk.isLoaded()) {
                        simulateSeason(terrain, chunk.getBlockX(),
                                chunk.getBlockY(), 16, 16,
                                chunk.getMetaData("Vanilla"));
                    }
                    int x = random.nextInt(16);
                    int y = random.nextInt(16);
                    double weather = climateGenerator
                            .getWeather(x + chunk.getBlockX(),
                                    y + chunk.getBlockY());
                    if (random.nextInt((int) (513 - weather * 512)) == 0 &&
                            random.nextInt(1000) == 0 &&
                            weather > 0.7f) {
                        world.getConnection()
                                .send(new PacketLightning(chunk.getBlockX() + x,
                                        chunk.getBlockY() + y,
                                        chunk.getHighestTerrainBlockZAt(x, y)));
                    } else if (
                            random.nextInt((int) (513 - weather * 512)) == 0 &&
                                    random.nextInt(10000) == 0 &&
                                    weather > 0.85f) {
                        EntityServer entity =
                                new EntityTornadoServer(worldServer,
                                        new Vector3d(chunk.getBlockX() + x,
                                                chunk.getBlockY() + y,
                                                chunk.getHighestTerrainBlockZAt(
                                                        x, y)));
                        entity.onSpawn();
                        worldServer.addEntity(entity);
                    }
                }
            }
        }
    }

    @Override
    public float getSunLightReduction(double x, double y) {
        return (float) climateGenerator.getSunLightReduction(x, y);
    }

    @Override
    public Vector3 getSunLightNormal(double x, double y) {
        double latitude = climateGenerator.getLatitude(y);
        double elevation = climateGenerator.getSunElevationD(latitude);
        double azimuth = climateGenerator.getSunAzimuthD(elevation, latitude);
        azimuth += FastMath.HALF_PI;
        double rz = FastMath.sinTable(elevation);
        double rd = FastMath.cosTable(elevation);
        double rx = FastMath.cosTable(azimuth) * rd;
        double ry = FastMath.sinTable(azimuth) * rd;
        double mix = FastMath.clamp(elevation * 100.0, -1.0, 1.0);
        return FastMath.normalizeSafe(new Vector3d(rx, ry, rz)).multiply(mix);
    }

    @Override
    public WorldSkybox createSkybox(WorldClient world, GL gl) {
        return new WorldSkyboxOverworld(climateGenerator, biomeGenerator, world,
                gl);
    }

    public void simulateSeason(TerrainServer.TerrainMutable terrain, int x,
            int y, int dx, int dy, TagStructure tagStructure) {
        long chunkSimulationCount = tagStructure.getLong("SimulationCount");
        int count;
        if (chunkSimulationCount <= 0) {
            count = 1;
        } else {
            long delta = simulationCount - chunkSimulationCount;
            Random random = ThreadLocalRandom.current();
            if (delta < 180 + random.nextInt(40)) {
                return;
            }
            count = FastMath.max(10240 / (int) delta, 1);
        }
        tagStructure.setLong("SimulationCount", simulationCount);
        simulateSeason(terrain, x, y, dx, dy, count);
    }

    private void simulateSeason(TerrainServer.TerrainMutable chunk, int x,
            int y, int dx, int dy, int chance) {
        Random random = ThreadLocalRandom.current();
        double humidity00 = climateGenerator.getHumidity(x, y);
        double temperature00 = climateGenerator.getTemperature(x, y);
        double humidity10 = climateGenerator.getHumidity(x + 15, y);
        double temperature10 = climateGenerator.getTemperature(x + 15, y);
        double humidity01 = climateGenerator.getHumidity(x, y + 15);
        double temperature01 = climateGenerator.getTemperature(x, y + 15);
        double humidity11 = climateGenerator.getHumidity(x + 15, y + 15);
        double temperature11 = climateGenerator.getTemperature(x + 15, y + 15);
        for (int yy = 0; yy < dx; yy++) {
            int yyy = yy + y;
            double mixY = yy / 15.0;
            double humidity0 = FastMath.mix(humidity00, humidity01, mixY);
            double humidity1 = FastMath.mix(humidity10, humidity11, mixY);
            double temperature0 =
                    FastMath.mix(temperature00, temperature01, mixY);
            double temperature1 =
                    FastMath.mix(temperature10, temperature11, mixY);
            for (int xx = 0; xx < dy; xx++) {
                if (random.nextInt(chance) == 0) {
                    int xxx = xx + x;
                    int z = chunk.getHighestTerrainBlockZAt(xxx, yyy);
                    double mixX = xx / 15.0;
                    double humidity = FastMath.mix(humidity0, humidity1, mixX);
                    double temperature = climateGenerator.getTemperatureD(
                            FastMath.mix(temperature0, temperature1, mixX), z);
                    BlockType spaceType = chunk.type(xxx, yyy, z);
                    BlockType groundType = chunk.type(xxx, yyy, z - 1);
                    if (humidity > 0.2) {
                        if (groundType == materials.dirt) {
                            chunk.typeData(xxx, yyy, z - 1, materials.grass,
                                    (short) random.nextInt(9));
                        } else if (groundType == materials.grass) {
                            chunk.data(xxx, yyy, z - 1,
                                    (short) random.nextInt(9));
                        }
                    } else {
                        if (groundType == materials.grass) {
                            chunk.typeData(xxx, yyy, z - 1, materials.dirt,
                                    (short) 0);
                        }
                    }
                    if (temperature > 1.0) {
                        if (spaceType == materials.snow) {
                            if (chunk.data(xxx, yyy, z) < 8) {
                                chunk.data(xxx, yyy, z,
                                        (short) (chunk.data(xxx, yyy, z) + 1));
                            } else {
                                chunk.typeData(xxx, yyy, z, materials.air,
                                        (short) 0);
                            }
                        }
                    } else {
                        double weather = climateGenerator.getWeather(xxx, yyy);
                        if (temperature < 0.0 &&
                                (weather > 0.5 || chance == 1)) {
                            if (spaceType == materials.air ||
                                    spaceType == materials.flower ||
                                    spaceType == materials.stoneRock) {
                                chunk.typeData(xxx, yyy, z, materials.snow,
                                        (short) 8);
                            } else if (spaceType == materials.snow &&
                                    chunk.data(xxx, yyy, z) > 0) {
                                chunk.data(xxx, yyy, z,
                                        (short) (chunk.data(xxx, yyy, z) - 1));
                            }
                        }
                    }
                }
            }
        }
    }
}
