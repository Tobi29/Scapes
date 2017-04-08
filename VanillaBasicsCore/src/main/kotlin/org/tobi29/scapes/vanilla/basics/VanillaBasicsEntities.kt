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

import org.tobi29.scapes.block.Registries
import org.tobi29.scapes.entity.EntityType
import org.tobi29.scapes.vanilla.basics.entity.client.*
import org.tobi29.scapes.vanilla.basics.entity.server.*

class VanillaBasicsEntities(reg: Registries.Registry<EntityType<*, *>>) {
    val player = reg("vanilla.basics.mob.Player") {
        EntityType<MobPlayerClientVB, Nothing>(it, ::MobPlayerClientVB,
                { _, _ -> throw UnsupportedOperationException() })
    }
    val blockBreak = reg("vanilla.basics.entity.BlockBreak") {
        EntityType(it, ::EntityBlockBreakClient, ::EntityBlockBreakServer)
    }
    val item = reg("vanilla.basics.mob.Item") {
        EntityType(it, ::MobItemClient, ::MobItemServer)
    }
    val flyingBlock = reg("vanilla.basics.mob.FlyingBlock") {
        EntityType(it, ::MobFlyingBlockClient, ::MobFlyingBlockServer)
    }
    val pig = reg("vanilla.basics.mob.Pig") {
        EntityType(it, ::MobPigClient, ::MobPigServer)
    }
    val zombie = reg("vanilla.basics.mob.Zombie") {
        EntityType(it, ::MobZombieClient, ::MobZombieServer)
    }
    val skeleton = reg("vanilla.basics.mob.Skeleton") {
        EntityType(it, ::MobSkeletonClient, ::MobSkeletonServer)
    }
    val bomb = reg("vanilla.basics.mob.Bomb") {
        EntityType(it, ::MobBombClient, ::MobBombServer)
    }
    val tornado = reg("vanilla.basics.entity.Tornado") {
        EntityType(it, ::EntityTornadoClient, ::EntityTornadoServer)
    }
    val alloy = reg("vanilla.basics.entity.Alloy") {
        EntityType(it, ::EntityAlloyClient, ::EntityAlloyServer)
    }
    val anvil = reg("vanilla.basics.entity.Anvil") {
        EntityType(it, ::EntityAnvilClient, ::EntityAnvilServer)
    }
    val bellows = reg("vanilla.basics.entity.Bellows") {
        EntityType(it, ::EntityBellowsClient, ::EntityBellowsServer)
    }
    val bloomery = reg("vanilla.basics.entity.Bloomery") {
        EntityType(it, ::EntityBloomeryClient, ::EntityBloomeryServer)
    }
    val chest = reg("vanilla.basics.entity.Chest") {
        EntityType(it, ::EntityChestClient, ::EntityChestServer)
    }
    val forge = reg("vanilla.basics.entity.Forge") {
        EntityType(it, ::EntityForgeClient, ::EntityForgeServer)
    }
    val furnace = reg("vanilla.basics.entity.Furnace") {
        EntityType(it, ::EntityFurnaceClient, ::EntityFurnaceServer)
    }
    val quern = reg("vanilla.basics.entity.Quern") {
        EntityType(it, ::EntityQuernClient, ::EntityQuernServer)
    }
    val researchTable = reg("vanilla.basics.entity.ResearchTable") {
        EntityType(it, ::EntityResearchTableClient, ::EntityResearchTableServer)
    }
    val farmland = reg("vanilla.basics.entity.Farmland") {
        EntityType(it, ::EntityFarmlandClient, ::EntityFarmlandServer)
    }
}
