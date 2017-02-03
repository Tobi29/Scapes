/*
 * Copyright 2012-2017 Tobi29
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

package scapes.plugin.tobi29.vanilla.basics

import org.tobi29.scapes.block.GameRegistry
import org.tobi29.scapes.chunk.WorldClient
import org.tobi29.scapes.chunk.WorldServer
import org.tobi29.scapes.entity.client.EntityClient
import org.tobi29.scapes.entity.server.EntityServer
import scapes.plugin.tobi29.vanilla.basics.entity.client.*
import scapes.plugin.tobi29.vanilla.basics.entity.server.*

internal fun registerEntities(registry: GameRegistry) {
    registry.getAsymSupplier<WorldServer, EntityServer, WorldClient, EntityClient>(
            "Core", "Entity").run {
        reg({ MobPigServer(it) }, { MobPigClient(it) },
                MobPigServer::class.java, "vanilla.basics.mob.Pig")
        reg({ MobZombieServer(it) }, { MobZombieClient(it) },
                MobZombieServer::class.java, "vanilla.basics.mob.Zombie")
        reg({ MobSkeletonServer(it) }, { MobSkeletonClient(it) },
                MobSkeletonServer::class.java, "vanilla.basics.mob.Skeleton")
        reg({ MobBombServer(it) }, { MobBombClient(it) },
                MobBombServer::class.java, "vanilla.basics.mob.Bomb")
        reg({ EntityTornadoServer(it) }, { EntityTornadoClient(it) },
                EntityTornadoServer::class.java,
                "vanilla.basics.entity.Tornado")
        reg({ EntityAlloyServer(it) }, { EntityAlloyClient(it) },
                EntityAlloyServer::class.java, "vanilla.basics.entity.Alloy")
        reg({ EntityAnvilServer(it) }, { EntityAnvilClient(it) },
                EntityAnvilServer::class.java, "vanilla.basics.entity.Anvil")
        reg({ EntityBellowsServer(it) }, { EntityBellowsClient(it) },
                EntityBellowsServer::class.java,
                "vanilla.basics.entity.Bellows")
        reg({ EntityBloomeryServer(it) }, { EntityBloomeryClient(it) },
                EntityBloomeryServer::class.java,
                "vanilla.basics.entity.Bloomery")
        reg({ EntityChestServer(it) }, { EntityChestClient(it) },
                EntityChestServer::class.java, "vanilla.basics.entity.Chest")
        reg({ EntityForgeServer(it) }, { EntityForgeClient(it) },
                EntityForgeServer::class.java, "vanilla.basics.entity.Forge")
        reg({ EntityFurnaceServer(it) }, { EntityFurnaceClient(it) },
                EntityFurnaceServer::class.java,
                "vanilla.basics.entity.Furnace")
        reg({ EntityQuernServer(it) }, { EntityQuernClient(it) },
                EntityQuernServer::class.java, "vanilla.basics.entity.Quern")
        reg({ EntityResearchTableServer(it) },
                { EntityResearchTableClient(it) },
                EntityResearchTableServer::class.java,
                "vanilla.basics.entity.ResearchTable")
        reg({ EntityFarmlandServer(it) }, { EntityFarmlandClient(it) },
                EntityFarmlandServer::class.java,
                "vanilla.basics.entity.Farmland")
    }
}