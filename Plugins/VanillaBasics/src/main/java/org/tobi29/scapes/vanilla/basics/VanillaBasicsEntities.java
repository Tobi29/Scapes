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
