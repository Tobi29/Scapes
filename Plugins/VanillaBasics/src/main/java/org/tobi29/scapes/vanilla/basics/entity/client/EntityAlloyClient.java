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

package org.tobi29.scapes.vanilla.basics.entity.client;

import org.tobi29.scapes.block.Inventory;
import org.tobi29.scapes.chunk.WorldClient;
import org.tobi29.scapes.engine.gui.Gui;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.engine.utils.math.vector.Vector3d;
import org.tobi29.scapes.entity.client.MobPlayerClientMain;
import org.tobi29.scapes.vanilla.basics.gui.GuiAlloyInventory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EntityAlloyClient extends EntityAbstractContainerClient {
    protected final Map<String, Double> metals = new ConcurrentHashMap<>();
    protected String result = "";
    protected float temperature;

    public EntityAlloyClient(WorldClient world) {
        this(world, Vector3d.ZERO);
    }

    public EntityAlloyClient(WorldClient world, Vector3 pos) {
        super(world, pos, new Inventory(world.registry(), 2));
    }

    @Override
    public Gui gui(MobPlayerClientMain player) {
        return new GuiAlloyInventory(this, player);
    }

    @Override
    public void read(TagStructure tagStructure) {
        super.read(tagStructure);
        metals.clear();
        for (Map.Entry<String, Object> entry : tagStructure
                .getStructure("Metals").getTagEntrySet()) {
            metals.put(entry.getKey(), (Double) entry.getValue());
        }
        result = tagStructure.getString("Result");
        temperature = tagStructure.getFloat("Temperature");
    }

    public Map<String, Double> metals() {
        return metals;
    }

    public String result() {
        return result;
    }
}
