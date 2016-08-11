/*
 * Copyright 2012-2016 Tobi29
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

package org.tobi29.scapes.vanilla.basics;

import org.tobi29.scapes.block.GameRegistry;
import org.tobi29.scapes.chunk.WorldClient;
import org.tobi29.scapes.chunk.WorldServer;
import org.tobi29.scapes.entity.client.EntityClient;
import org.tobi29.scapes.entity.server.EntityServer;
import org.tobi29.scapes.vanilla.basics.entity.client.*;
import org.tobi29.scapes.vanilla.basics.entity.server.*;

class VanillaBasicsEntities {
    static void registerEntities(GameRegistry registry) {
        GameRegistry.AsymSupplierRegistry<WorldServer, EntityServer, WorldClient, EntityClient>
                r = registry.getAsymSupplier("Core", "Entity");
        r.reg(MobPigServer::new, MobPigClient::new, MobPigServer.class,
                "vanilla.basics.mob.Pig");
        r.reg(MobZombieServer::new, MobZombieClient::new, MobZombieServer.class,
                "vanilla.basics.mob.Zombie");
        r.reg(MobSkeletonServer::new, MobSkeletonClient::new,
                MobSkeletonServer.class, "vanilla.basics.mob.Skeleton");
        r.reg(MobBombServer::new, MobBombClient::new, MobBombServer.class,
                "vanilla.basics.mob.Bomb");
        r.reg(EntityTornadoServer::new, EntityTornadoClient::new,
                EntityTornadoServer.class, "vanilla.basics.entity.Tornado");
        r.reg(EntityAlloyServer::new, EntityAlloyClient::new,
                EntityAlloyServer.class, "vanilla.basics.entity.Alloy");
        r.reg(EntityAnvilServer::new, EntityAnvilClient::new,
                EntityAnvilServer.class, "vanilla.basics.entity.Anvil");
        r.reg(EntityBellowsServer::new, EntityBellowsClient::new,
                EntityBellowsServer.class, "vanilla.basics.entity.Bellows");
        r.reg(EntityBloomeryServer::new, EntityBloomeryClient::new,
                EntityBloomeryServer.class, "vanilla.basics.entity.Bloomery");
        r.reg(EntityChestServer::new, EntityChestClient::new,
                EntityChestServer.class, "vanilla.basics.entity.Chest");
        r.reg(EntityForgeServer::new, EntityForgeClient::new,
                EntityForgeServer.class, "vanilla.basics.entity.Forge");
        r.reg(EntityFurnaceServer::new, EntityFurnaceClient::new,
                EntityFurnaceServer.class, "vanilla.basics.entity.Furnace");
        r.reg(EntityQuernServer::new, EntityQuernClient::new,
                EntityQuernServer.class, "vanilla.basics.entity.Quern");
        r.reg(EntityResearchTableServer::new, EntityResearchTableClient::new,
                EntityResearchTableServer.class,
                "vanilla.basics.entity.ResearchTable");
        r.reg(EntityFarmlandServer::new, EntityFarmlandClient::new,
                EntityFarmlandServer.class, "vanilla.basics.entity.Farmland");
    }
}
