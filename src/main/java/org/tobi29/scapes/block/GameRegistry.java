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

package org.tobi29.scapes.block;

import org.tobi29.scapes.chunk.IDStorage;
import org.tobi29.scapes.engine.utils.Pair;
import org.tobi29.scapes.entity.client.*;
import org.tobi29.scapes.entity.server.*;
import org.tobi29.scapes.packets.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GameRegistry {
    private final Map<Pair<String, String>, Registry<?>> registries =
            new ConcurrentHashMap<>();
    private final Map<Pair<String, String>, SupplierRegistry<?>>
            supplierRegistries = new ConcurrentHashMap<>();
    private final Map<Class<? extends EntityServer>, Integer> entityIDs =
            new ConcurrentHashMap<>();
    private final Map<String, Material> materialNames =
            new ConcurrentHashMap<>();
    private final List<CraftingRecipeType> craftingRecipes = new ArrayList<>();
    private final List<CraftingRecipeType> craftingRecipesTable =
            new ArrayList<>();
    private final IDStorage idStorage;
    private final BlockType air;
    private BlockType[] blocks;
    private Material[] materials;
    private boolean locked;

    public GameRegistry(IDStorage idStorage) {
        this.idStorage = idStorage;
        materials = new Material[1];
        air = new BlockAir(this);
        blocks = new BlockType[1];
        materials[0] = air;
        blocks[(short) 0] = air;
        air.id = 0;
        Registry<EntityServer.Supplier> entityServerRegistry =
                add("Core", "Entity", 1, Integer.MAX_VALUE);
        Registry<EntityClient.Supplier> entityClientRegistry =
                add("Core", "EntityClient", 1, Integer.MAX_VALUE);
        int id = entityServerRegistry.register(null, "core.mob.Player");
        entityClientRegistry.register(MobPlayerClient::new, id);
        entityIDs.put(MobPlayerServer.class, id);
        registerEntity(EntityBlockBreakServer::new, EntityBlockBreakClient::new,
                EntityBlockBreakServer.class, "core.entity.BlockBreak");
        registerEntity(MobItemServer::new, MobItemClient::new,
                MobItemServer.class, "core.mob.Item");
        registerEntity(MobFlyingBlockServer::new, MobFlyingBlockClient::new,
                MobFlyingBlockServer.class, "core.mob.FlyingBlock");
        registerEntity(MobBombServer::new, MobBombClient::new,
                MobBombServer.class, "core.mob.Bomb");
        SupplierRegistry<Packet> packetRegistry =
                addSupplier("Core", "Packet", 1, Short.MAX_VALUE);
        packetRegistry
                .register(PacketRequestChunk::new, "core.packet.RequestChunk");
        packetRegistry.register(PacketRequestEntity::new,
                "core.packet.RequestEntity");
        packetRegistry.register(PacketSendChunk::new, "core.packet.SendChunk");
        packetRegistry
                .register(PacketBlockChange::new, "core.packet.BlockChange");
        packetRegistry.register(PacketBlockChangeAir::new,
                "core.packet.BlockChangeAir");
        packetRegistry.register(PacketEntityAdd::new, "core.packet.EntityAdd");
        packetRegistry
                .register(PacketEntityChange::new, "core.packet.EntityChange");
        packetRegistry.register(PacketEntityMetaData::new,
                "core.packet.EntityMetaData");
        packetRegistry.register(PacketMobMoveRelative::new,
                "core.packet.MobMoveRelative");
        packetRegistry.register(PacketMobMoveAbsolute::new,
                "core.packet.MobMoveAbsolute");
        packetRegistry
                .register(PacketMobChangeRot::new, "core.packet.MobChangeRot");
        packetRegistry.register(PacketMobChangeSpeed::new,
                "core.packet.MobChangeSpeed");
        packetRegistry.register(PacketMobChangeState::new,
                "core.packet.MobChangeState");
        packetRegistry.register(PacketMobDamage::new, "core.packet.MobDamage");
        packetRegistry.register(PacketEntityDespawn::new,
                "core.packet.EntityDespawn");
        packetRegistry
                .register(PacketSoundEffect::new, "core.packet.SoundEffect");
        packetRegistry
                .register(PacketInteraction::new, "core.packet.Interaction");
        packetRegistry.register(PacketInventoryInteraction::new,
                "core.packet.InventoryInteraction");
        packetRegistry
                .register(PacketOpenCrafting::new, "core.packet.OpenCrafting");
        packetRegistry.register(PacketOpenGui::new, "core.packet.OpenGui");
        packetRegistry.register(PacketCloseGui::new, "core.packet.CloseGui");
        packetRegistry.register(PacketUpdateInventory::new,
                "core.packet.UpdateInventory");
        packetRegistry.register(PacketUpdateStatistics::new,
                "core.packet.UpdateStatistics");
        packetRegistry.register(PacketChat::new, "core.packet.Chat");
        packetRegistry
                .register(PacketPlayerHunger::new, "core.packet.PlayerHunger");
        packetRegistry.register(PacketItemUse::new, "core.packet.ItemUse");
        packetRegistry.register(PacketCrafting::new, "core.packet.Crafting");
        packetRegistry
                .register(PacketDisconnect::new, "core.packet.Disconnect");
        packetRegistry.register(PacketSetWorld::new, "core.packet.SetWorld");
        packetRegistry.register(PacketPing::new, "core.packet.Ping");
        packetRegistry.register(PacketSkin::new, "core.packet.Skin");
        SupplierRegistry<Update> updateRegistry =
                addSupplier("Core", "Update", 1, Short.MAX_VALUE);
        updateRegistry
                .register(UpdateBlockUpdate::new, "core.update.BlockUpdate");
        updateRegistry.register(UpdateBlockUpdateUpdateTile::new,
                "core.update.BlockUpdateUpdateTile");
    }

    public void lock() {
        locked = true;
    }

    @SuppressWarnings("unchecked")
    public synchronized <E> Registry<E> add(String module, String type, int min,
            int max) {
        if (locked) {
            throw new IllegalStateException("Initializing already ended");
        }
        Pair<String, String> pair = new Pair<>(module, type);
        Registry<?> registry = registries.get(pair);
        if (registry == null) {
            registry = new Registry<>(module, type, min, max);
            registries.put(pair, registry);
        }
        return (Registry<E>) registry;
    }

    @SuppressWarnings("unchecked")
    public synchronized <E> SupplierRegistry<E> addSupplier(String module,
            String type, int min, int max) {
        if (locked) {
            throw new IllegalStateException("Initializing already ended");
        }
        Pair<String, String> pair = new Pair<>(module, type);
        SupplierRegistry<?> registry = supplierRegistries.get(pair);
        if (registry == null) {
            registry = new SupplierRegistry<>(module, type, min, max);
            registries.put(pair, registry);
            supplierRegistries.put(pair, registry);
        }
        return (SupplierRegistry<E>) registry;
    }

    @SuppressWarnings("unchecked")
    public <E> Registry<E> get(String module, String type) {
        return (Registry<E>) registries.get(new Pair<>(module, type));
    }

    @SuppressWarnings("unchecked")
    public <E> SupplierRegistry<E> getSupplier(String module, String type) {
        return (SupplierRegistry<E>) supplierRegistries
                .get(new Pair<>(module, type));
    }

    public Material getMaterial(String name) {
        return materialNames.get(name);
    }

    public Material getMaterial(int id) {
        return materials[id];
    }

    public Material[] getMaterials() {
        return materials;
    }

    public BlockType getBlock(int id) {
        return blocks[id];
    }

    public BlockType[] getBlocks() {
        return blocks;
    }

    public BlockType getAir() {
        return air;
    }

    public int getEntityID(EntityServer entity) {
        return entityIDs.get(entity.getClass());
    }

    public void registerMaterial(Material material) {
        if (locked) {
            throw new IllegalStateException("Initializing already ended");
        }
        String nameID = material.getNameID();
        boolean blockType = material instanceof BlockType;
        int id = idStorage.get("Core", "Material", nameID,
                blockType ? 1 : Short.MAX_VALUE + 1,
                blockType ? Short.MAX_VALUE : Integer.MAX_VALUE);
        material.id = id;
        if (id >= materials.length) {
            Material[] materials2 = materials;
            materials = new Material[id + 1];
            System.arraycopy(materials2, 0, materials, 0, materials2.length);
        }
        materials[id] = material;
        if (material instanceof BlockType) {
            if (id >= blocks.length) {
                BlockType[] blocks2 = blocks;
                blocks = new BlockType[id + 1];
                System.arraycopy(blocks2, 0, blocks, 0, blocks2.length);
            }
            blocks[id] = (BlockType) material;
        }
        materialNames.put(nameID, material);
        materialNames.put(nameID
                .substring(nameID.lastIndexOf('.') + 1, nameID.length()),
                material);
    }

    public void registerEntity(EntityServer.Supplier serverSupplier,
            EntityClient.Supplier clientSupplier,
            Class<? extends EntityServer> entityClass, String name) {
        Registry<EntityServer.Supplier> entityServerRegistry =
                get("Core", "Entity");
        Registry<EntityClient.Supplier> entityClientRegistry =
                get("Core", "EntityClient");
        int id = entityServerRegistry.register(serverSupplier, name);
        entityClientRegistry.register(clientSupplier, id);
        entityIDs.put(entityClass, id);
    }

    public void registerCraftingRecipe(CraftingRecipeType recipe,
            boolean needsTable) {
        if (locked) {
            throw new IllegalStateException("Initializing already ended");
        }
        if (!needsTable) {
            craftingRecipes.add(recipe);
        }
        craftingRecipesTable.add(recipe);
    }

    public List<CraftingRecipeType> getCraftingRecipes(boolean needsTable) {
        if (needsTable) {
            return craftingRecipesTable;
        } else {
            return craftingRecipes;
        }
    }

    @FunctionalInterface
    public interface Supplier<E> {
        E get(GameRegistry registry);
    }

    public class Registry<E> {
        protected final String module, type;
        protected final int min, max;
        protected final Map<Integer, E> objects = new ConcurrentHashMap<>();
        protected final Map<E, Integer> ids = new ConcurrentHashMap<>();
        protected final List<E> values = new ArrayList<>();

        private Registry(String module, String type, int min, int max) {
            this.module = module;
            this.type = type;
            this.min = min;
            this.max = max;
        }

        public int register(E element, String name) {
            if (locked) {
                throw new IllegalStateException("Initializing already ended");
            }
            int id = idStorage.get(module, type, name, min, max);
            register(element, id);
            return id;
        }

        public void register(E element, int id) {
            if (locked) {
                throw new IllegalStateException("Initializing already ended");
            }
            if (element == null) {
                return;
            }
            objects.put(id, element);
            ids.put(element, id);
            while (values.size() <= id) {
                values.add(null);
            }
            values.set(id, element);
        }

        public List<E> values() {
            return Collections.unmodifiableList(values);
        }

        public E get(int id) {
            return objects.get(id);
        }

        public int get(E object) {
            return ids.get(object);
        }
    }

    public class SupplierRegistry<E> extends Registry<Supplier<? extends E>> {
        private final Map<Class<?>, Integer> suppliers =
                new ConcurrentHashMap<>();

        private SupplierRegistry(String module, String type, int min, int max) {
            super(module, type, min, max);
        }

        @Override
        public int register(Supplier<? extends E> element, String name) {
            if (locked) {
                throw new IllegalStateException("Initializing already ended");
            }
            int id = super.register(element, name);
            suppliers.put(element.get(GameRegistry.this).getClass(), id);
            return id;
        }

        public int getID(E object) {
            return suppliers.get(object.getClass());
        }
    }
}
