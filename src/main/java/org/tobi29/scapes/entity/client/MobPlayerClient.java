package org.tobi29.scapes.entity.client;

import org.tobi29.scapes.block.Inventory;
import org.tobi29.scapes.block.ItemStack;
import org.tobi29.scapes.chunk.WorldClient;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.math.AABB;
import org.tobi29.scapes.engine.utils.math.Frustum;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.entity.CreatureType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class MobPlayerClient extends MobLivingEquippedClient
        implements EntityContainerClient {
    protected final Inventory inventoryContainer, inventoryHold;
    protected final Map<String, Inventory> inventories =
            new ConcurrentHashMap<>();
    protected int inventorySelectLeft, inventorySelectRight = 9, healWait;
    protected String nickname;
    protected byte[] skin;

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

    @Override
    public ItemStack leftWeapon() {
        return inventoryContainer.item(inventorySelectLeft);
    }

    @Override
    public ItemStack rightWeapon() {
        return inventoryContainer.item(inventorySelectRight);
    }

    public int inventorySelectLeft() {
        return inventorySelectLeft;
    }

    public void setInventorySelectLeft(int select) {
        int c = 1;
        if (select < inventorySelectLeft) {
            c = -1;
        }
        select %= 10;
        if (select < 0) {
            select += 10;
        }
        if (select == inventorySelectRight) {
            select += c;
        }
        select %= 10;
        if (select < 0) {
            select += 10;
        }
        inventorySelectLeft = select;
    }

    public int inventorySelectRight() {
        return inventorySelectRight;
    }

    public void setInventorySelectRight(int select) {
        int c = 1;
        if (select < inventorySelectRight) {
            c = -1;
        }
        select %= 10;
        if (select < 0) {
            select += 10;
        }
        if (select == inventorySelectLeft) {
            select += c;
        }
        select %= 10;
        if (select < 0) {
            select += 10;
        }
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
        healWait = tagStructure.getInteger("HealWait");
        TagStructure inventoryTag = tagStructure.getStructure("Inventory");
        inventories.forEach((id, inventory) -> inventory
                .load(inventoryTag.getStructure(id)));
        if (tagStructure.has("Nickname")) {
            nickname = tagStructure.getString("Nickname");
        }
        if (tagStructure.has("SkinChecksum")) {
            skin = tagStructure.getByteArray("SkinChecksum");
        }
    }

    @Override
    public Inventory inventory(String id) {
        return inventories.get(id);
    }
}
