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

package org.tobi29.scapes.entity.client;

import org.tobi29.scapes.chunk.WorldClient;
import org.tobi29.scapes.engine.utils.Checksum;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.math.AABB;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.entity.CreatureType;

public abstract class MobPlayerClient extends MobLivingEquippedClient {
    protected int inventorySelectLeft, inventorySelectRight = 9;
    protected String nickname;
    protected Checksum skin;

    protected MobPlayerClient(WorldClient world, Vector3 pos, Vector3 speed,
            AABB aabb, double lives, double maxLives, String nickname) {
        super(world, pos, speed, aabb, lives, maxLives);
        this.nickname = nickname;
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
        if (tagStructure.has("Nickname")) {
            nickname = tagStructure.getString("Nickname");
        }
        if (tagStructure.has("SkinChecksum")) {
            skin = new Checksum(tagStructure.getStructure("SkinChecksum"));
        }
    }
}
