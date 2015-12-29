package org.tobi29.scapes.entity.client;

import org.tobi29.scapes.block.Inventory;
import org.tobi29.scapes.chunk.WorldClient;
import org.tobi29.scapes.engine.utils.Checksum;
import org.tobi29.scapes.engine.utils.Streams;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.math.AABB;
import org.tobi29.scapes.engine.utils.math.Frustum;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.entity.CreatureType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class MobPlayerClient extends MobLivingEquippedClient {
    protected final Inventory inventoryContainer, inventoryHold;
    protected final Map<String, Inventory> inventories =
            new ConcurrentHashMap<>();
    protected int inventorySelectLeft, inventorySelectRight = 9;
    protected String nickname;
    protected Checksum skin;

    protected MobPlayerClient(WorldClient world, Vector3 pos, Vector3 speed,
            AABB aabb, double lives, double maxLives, Frustum viewField,
            Frustum hitField, String nickname) {
        super(world, pos, speed, aabb, lives, maxLives, viewField, hitField);
        this.nickname = nickname;
        inventoryContainer = new Inventory(registry, 40);
        inventoryHold = new Inventory(registry, 1);
        inventories.put("Container", inventoryContainer);
        inventories.put("Hold", inventoryHold);
    }

    public int inventorySelectLeft() {
        return inventorySelectLeft;
    }

    public void setInventorySelectLeft(int select) {
        inventorySelectLeft = select;
    }

    public int inventorySelectRight() {
        return inventorySelectRight;
    }

    public void setInventorySelectRight(int select) {
        inventorySelectRight = select;
    }

    public String nickname() {
        return nickname;
    }

    @Override
    public CreatureType creatureType() {
        return CreatureType.CREATURE;
    }

    @Override
    public void read(TagStructure tagStructure) {
        super.read(tagStructure);
        TagStructure inventoryTag = tagStructure.getStructure("Inventory");
        Streams.of(inventories.entrySet()).forEach(entry -> entry.getValue()
                .load(inventoryTag.getStructure(entry.getKey())));
        if (tagStructure.has("Nickname")) {
            nickname = tagStructure.getString("Nickname");
        }
        if (tagStructure.has("SkinChecksum")) {
            skin = new Checksum(tagStructure.getByteArray("SkinChecksum"));
        }
    }
}
