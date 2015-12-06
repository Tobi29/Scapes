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
    protected void updateComponent(ScapesEngine engine) {
        super.updateComponent(engine);
        profiler.entries().forEach(
                entry -> get(entry.a.getSimpleName()).setValue(entry.b));
    }
}
