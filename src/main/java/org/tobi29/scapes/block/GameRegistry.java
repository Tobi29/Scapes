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
import org.tobi29.scapes.plugins.Plugins;
import org.tobi29.scapes.plugins.WorldType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

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
    private final IDStorage idStorage;
    private final BlockType air;
    private BlockType[] blocks;
    private Material[] materials;
    private boolean locked;

    public GameRegistry(IDStorage idStorage, Plugins plugins) {
        this.idStorage = idStorage;
        materials = new Material[1];
        air = new BlockAir(this);
        blocks = new BlockType[1];
        materials[0] = air;
        blocks[0] = air;
        air.id = 0;
        Registry<EntityServer.Supplier> entityServerRegistry =
                add("Core", "Entity", 1, Integer.MAX_VALUE);
        Registry<EntityClient.Supplier> entityClientRegistry =
                add("Core", "EntityClient", 1, Integer.MAX_VALUE);
        WorldType worldType = plugins.worldType();
        int id = entityServerRegistry.reg(null, "core.mob.Player");
        entityClientRegistry.register(worldType.playerSupplier(), id);
        entityIDs.put(worldType.playerClass(), id);
        registerEntity(EntityBlockBreakServer::new, EntityBlockBreakClient::new,
                EntityBlockBreakServer.class, "core.entity.BlockBreak");
        registerEntity(MobItemServer::new, MobItemClient::new,
                MobItemServer.class, "core.mob.Item");
        registerEntity(MobFlyingBlockServer::new, MobFlyingBlockClient::new,
                MobFlyingBlockServer.class, "core.mob.FlyingBlock");
        registerEntity(MobBombServer::new, MobBombClient::new,
                MobBombServer.class, "core.mob.Bomb");
        SupplierRegistry<Packet> pr =
                addSupplier("Core", "Packet", 1, Short.MAX_VALUE);
        pr.regS(PacketRequestChunk::new, "core.packet.RequestChunk");
        pr.regS(PacketRequestEntity::new, "core.packet.RequestEntity");
        pr.regS(PacketSendChunk::new, "core.packet.SendChunk");
        pr.regS(PacketBlockChange::new, "core.packet.BlockChange");
        pr.regS(PacketBlockChangeAir::new, "core.packet.BlockChangeAir");
        pr.regS(PacketEntityAdd::new, "core.packet.EntityAdd");
        pr.regS(PacketEntityChange::new, "core.packet.EntityChange");
        pr.regS(PacketEntityMetaData::new, "core.packet.EntityMetaData");
        pr.regS(PacketMobMoveRelative::new, "core.packet.MobMoveRelative");
        pr.regS(PacketMobMoveAbsolute::new, "core.packet.MobMoveAbsolute");
        pr.regS(PacketMobChangeRot::new, "core.packet.MobChangeRot");
        pr.regS(PacketMobChangeSpeed::new, "core.packet.MobChangeSpeed");
        pr.regS(PacketMobChangeState::new, "core.packet.MobChangeState");
        pr.regS(PacketMobDamage::new, "core.packet.MobDamage");
        pr.regS(PacketEntityDespawn::new, "core.packet.EntityDespawn");
        pr.regS(PacketSoundEffect::new, "core.packet.SoundEffect");
        pr.regS(PacketInteraction::new, "core.packet.Interaction");
        pr.regS(PacketInventoryInteraction::new,
                "core.packet.InventoryInteraction");
        pr.regS(PacketOpenCrafting::new, "core.packet.OpenCrafting");
        pr.regS(PacketOpenGui::new, "core.packet.OpenGui");
        pr.regS(PacketCloseGui::new, "core.packet.CloseGui");
        pr.regS(PacketUpdateInventory::new, "core.packet.UpdateInventory");
        pr.regS(PacketUpdateStatistics::new, "core.packet.UpdateStatistics");
        pr.regS(PacketChat::new, "core.packet.Chat");
        pr.regS(PacketItemUse::new, "core.packet.ItemUse");
        pr.regS(PacketCrafting::new, "core.packet.Crafting");
        pr.regS(PacketDisconnect::new, "core.packet.Disconnect");
        pr.regS(PacketSetWorld::new, "core.packet.SetWorld");
        pr.regS(PacketPingClient::new, "core.packet.PingClient");
        pr.regS(PacketPingServer::new, "core.packet.PingServer");
        pr.regS(PacketSkin::new, "core.packet.Skin");
        SupplierRegistry<Update> ur =
                addSupplier("Core", "Update", 1, Short.MAX_VALUE);
        ur.regS(UpdateBlockUpdate::new, "core.update.BlockUpdate");
        ur.regS(UpdateBlockUpdateUpdateTile::new,
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

    public Optional<Material> material(String name) {
        return Optional.ofNullable(materialNames.get(name));
    }

    public Optional<Material> material(int id) {
        return Optional.ofNullable(materials[id]);
    }

    public Material[] materials() {
        return materials;
    }

    public Optional<BlockType> block(int id) {
        return Optional.ofNullable(blocks[id]);
    }

    public BlockType[] blocks() {
        return blocks;
    }

    public BlockType air() {
        return air;
    }

    public int entityID(EntityServer entity) {
        return entityIDs.get(entity.getClass());
    }

    public void registerMaterial(Material material) {
        if (locked) {
            throw new IllegalStateException("Initializing already ended");
        }
        String nameID = material.nameID();
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
        materialNames.put(nameID.substring(nameID.lastIndexOf('.') + 1,
                        nameID.length()), material);
    }

    public void registerEntity(EntityServer.Supplier serverSupplier,
            EntityClient.Supplier clientSupplier,
            Class<? extends EntityServer> entityClass, String name) {
        Registry<EntityServer.Supplier> entityServerRegistry =
                get("Core", "Entity");
        Registry<EntityClient.Supplier> entityClientRegistry =
                get("Core", "EntityClient");
        int id = entityServerRegistry.reg(serverSupplier, name);
        entityClientRegistry.register(clientSupplier, id);
        entityIDs.put(entityClass, id);
    }

    public void registerCraftingRecipe(CraftingRecipeType recipe) {
        if (locked) {
            throw new IllegalStateException("Initializing already ended");
        }
        craftingRecipes.add(recipe);
    }

    public List<CraftingRecipeType> getCraftingRecipes() {
        return craftingRecipes;
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

        public int reg(E element, String name) {
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

    public class SupplierRegistry<E>
            extends Registry<Function<GameRegistry, ? extends E>> {
        private final Map<Class<?>, Integer> suppliers =
                new ConcurrentHashMap<>();

        private SupplierRegistry(String module, String type, int min, int max) {
            super(module, type, min, max);
        }

        public int regS(Supplier<? extends E> element, String name) {
            return reg(registry -> element.get(), name);
        }

        @Override
        public int reg(Function<GameRegistry, ? extends E> element,
                String name) {
            if (locked) {
                throw new IllegalStateException("Initializing already ended");
            }
            int id = super.reg(element, name);
            suppliers.put(element.apply(GameRegistry.this).getClass(), id);
            return id;
        }

        public int id(E object) {
            return suppliers.get(object.getClass());
        }
    }
}
