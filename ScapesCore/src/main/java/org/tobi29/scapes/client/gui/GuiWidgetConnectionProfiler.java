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

package org.tobi29.scapes.client.gui;

import org.tobi29.scapes.client.connection.ConnectionProfiler;
import org.tobi29.scapes.engine.ScapesEngine;
import org.tobi29.scapes.engine.gui.GuiLayoutData;
import org.tobi29.scapes.engine.gui.debug.GuiWidgetDebugValues;

public class GuiWidgetConnectionProfiler extends GuiWidgetDebugValues {
    private final ConnectionProfiler profiler;

    public GuiWidgetConnectionProfiler(GuiLayoutData parent,
            ConnectionProfiler profiler) {
        super(parent);
        this.profiler = profiler;
    }

    @Override
    protected void updateComponent(ScapesEngine engine, double delta) {
        super.updateComponent(engine, delta);
        profiler.entries().forEach(
                entry -> get(entry.a.getSimpleName()).setValue(entry.b));
    }
}
