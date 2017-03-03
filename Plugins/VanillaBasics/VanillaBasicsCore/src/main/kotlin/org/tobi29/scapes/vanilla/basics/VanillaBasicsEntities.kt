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

package org.tobi29.scapes.vanilla.basics

import org.tobi29.scapes.block.GameRegistry
import org.tobi29.scapes.entity.EntityType
import org.tobi29.scapes.vanilla.basics.entity.client.*
import org.tobi29.scapes.vanilla.basics.entity.server.*

internal fun registerEntities(registry: GameRegistry) {
    registry.get<EntityType>("Core", "Entity").run {
        reg("vanilla.basics.mob.Pig") {
            EntityType(it, { MobPigClient(it) }, { MobPigServer(it) })
        }
        reg("vanilla.basics.mob.Zombie") {
            EntityType(it, { MobZombieClient(it) }, { MobZombieServer(it) })
        }
        reg("vanilla.basics.mob.Skeleton") {
            EntityType(it, { MobSkeletonClient(it) }, { MobSkeletonServer(it) })
        }
        reg("vanilla.basics.mob.Bomb") {
            EntityType(it, { MobBombClient(it) }, { MobBombServer(it) })
        }
        reg("vanilla.basics.entity.Tornado") {
            EntityType(it, { EntityTornadoClient(it) },
                    { EntityTornadoServer(it) })
        }
        reg("vanilla.basics.entity.Alloy") {
            EntityType(it, { EntityAlloyClient(it) }, { EntityAlloyServer(it) })
        }
        reg("vanilla.basics.entity.Anvil") {
            EntityType(it, { EntityAnvilClient(it) }, { EntityAnvilServer(it) })
        }
        reg("vanilla.basics.entity.Bellows") {
            EntityType(it, { EntityBellowsClient(it) },
                    { EntityBellowsServer(it) })
        }
        reg("vanilla.basics.entity.Bloomery") {
            EntityType(it, { EntityBloomeryClient(it) },
                    { EntityBloomeryServer(it) })
        }
        reg("vanilla.basics.entity.Chest") {
            EntityType(it, { EntityChestClient(it) }, { EntityChestServer(it) })
        }
        reg("vanilla.basics.entity.Forge") {
            EntityType(it, { EntityForgeClient(it) }, { EntityForgeServer(it) })
        }
        reg("vanilla.basics.entity.Furnace") {
            EntityType(it, { EntityFurnaceClient(it) },
                    { EntityFurnaceServer(it) })
        }
        reg("vanilla.basics.entity.Quern") {
            EntityType(it, { EntityQuernClient(it) }, { EntityQuernServer(it) })
        }
        reg("vanilla.basics.entity.ResearchTable") {
            EntityType(it, { EntityResearchTableClient(it) },
                    { EntityResearchTableServer(it) })
        }
        reg("vanilla.basics.entity.Farmland") {
            EntityType(it, { EntityFarmlandClient(it) },
                    { EntityFarmlandServer(it) })
        }
    }
}
