package org.tobi29.scapes.client.input.touch;

import java8.util.Optional;
import java8.util.stream.Stream;
import org.tobi29.scapes.engine.ScapesEngine;
import org.tobi29.scapes.engine.gui.GuiComponent;
import org.tobi29.scapes.engine.gui.GuiComponentEvent;
import org.tobi29.scapes.engine.gui.GuiController;
import org.tobi29.scapes.engine.gui.GuiCursor;
import org.tobi29.scapes.engine.input.ControllerBasic;
import org.tobi29.scapes.engine.input.ControllerTouch;
import org.tobi29.scapes.engine.utils.Pair;
import org.tobi29.scapes.engine.utils.Streams;
import org.tobi29.scapes.engine.utils.math.vector.MutableVector2;
import org.tobi29.scapes.engine.utils.math.vector.Vector2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GuiControllerTouch implements GuiController {
    private final ScapesEngine engine;
    private final ControllerTouch controller;
    private final Map<ControllerTouch.Tracker, Finger> fingers =
            new ConcurrentHashMap<>();
    private List<Pair<GuiCursor, ControllerBasic.PressEvent>> clicks =
            Collections.emptyList();

    public GuiControllerTouch(ScapesEngine engine, ControllerTouch controller) {
        this.engine = engine;
        this.controller = controller;
    }

    @Override
    public void update(double delta) {
        double ratio = 540.0 / engine.container().containerHeight();
        List<Pair<GuiCursor, ControllerBasic.PressEvent>> newClicks =
                new ArrayList<>();
        Map<ControllerTouch.Tracker, Finger> newFingers =
                new ConcurrentHashMap<>();
        controller.fingers().forEach(tracker -> {
            Finger fetch = fingers.get(tracker);
            if (fetch == null) {
                fetch = new Finger(tracker.pos());
                Finger finger = fetch;
                handleFinger(finger, ratio);
                finger.dragX = finger.cursor.guiX();
                finger.dragY = finger.cursor.guiY();
                engine.guiStack().fireEvent(
                        new GuiComponentEvent(finger.cursor.guiX(),
                                finger.cursor.guiY()),
                        (component, event, engine) -> true, engine)
                        .ifPresent(component -> {
                            if (!Streams.of(fingers.values())
                                    .filter(f -> f.dragging.isPresent())
                                    .filter(f -> f.dragging.get() == component)
                                    .findAny().isPresent()) {
                                finger.dragging = Optional.of(component);
                                component.gui().sendNewEvent(
                                        new GuiComponentEvent(
                                                finger.cursor.guiX(),
                                                finger.cursor.guiY()),
                                        component, component::pressLeft,
                                        engine);
                            }
                        });
                fingers.put(tracker, finger);
            } else {
                handleFinger(fetch, ratio);
            }
            newFingers.put(tracker, fetch);
        });
        Streams.of(fingers.keySet())
                .filter(tracker -> !newFingers.containsKey(tracker))
                .forEach(tracker -> {
                    Finger finger = fingers.remove(tracker);
                    if (finger.dragging.isPresent()) {
                        GuiComponent component = finger.dragging.get();
                        if (System.currentTimeMillis() - finger.start < 250) {
                            component.gui().sendNewEvent(
                                    new GuiComponentEvent(finger.cursor.guiX(),
                                            finger.cursor.guiY()), component,
                                    component::clickLeft, engine);
                        }
                        component.gui().sendNewEvent(
                                new GuiComponentEvent(finger.cursor.guiX(),
                                        finger.cursor.guiY()), component,
                                component::dropLeft, engine);
                    }
                });
        clicks = newClicks;
    }

    @Override
    public void focusTextField(TextFieldData data, boolean multiline) {
        engine.container().dialog("Input", data, multiline);
    }

    @Override
    public boolean processTextField(TextFieldData data, boolean multiline) {
        return true;
    }

    @Override
    public Stream<GuiCursor> cursors() {
        return Streams.of(fingers.values()).map(Finger::cursor);
    }

    @Override
    public Stream<Pair<GuiCursor, ControllerBasic.PressEvent>> clicks() {
        return Streams.of(clicks);
    }

    @Override
    public boolean captureCursor() {
        return false;
    }

    private void handleFinger(Finger finger, double ratio) {
        finger.cursor.set(finger.tracker.now(),
                finger.tracker.now().multiply(ratio));
        if (finger.dragging.isPresent()) {
            GuiComponent component = finger.dragging.get();
            double relativeX = finger.cursor.guiX() - finger.dragX;
            double relativeY = finger.cursor.guiY() - finger.dragY;
            finger.dragX = finger.cursor.guiX();
            finger.dragY = finger.cursor.guiY();
            component.gui().sendNewEvent(
                    new GuiComponentEvent(finger.cursor.guiX(),
                            finger.cursor.guiY(), relativeX, relativeY),
                    component, component::dragLeft, engine);
            Vector2 source = finger.source.multiply(ratio);
            engine.guiStack().fireRecursiveEvent(
                    new GuiComponentEvent(source.doubleX(), source.doubleY(),
                            relativeX, relativeY), GuiComponent::scroll,
                    engine);
        }
    }

    private static class Finger {
        private final MutableVector2 tracker;
        private final Vector2 source;
        private final long start;
        private final GuiCursor cursor = new GuiCursor(true);
        private Optional<GuiComponent> dragging = Optional.empty();
        private double dragX, dragY;

        private Finger(MutableVector2 tracker) {
            this.tracker = tracker;
            source = tracker.now();
            start = System.currentTimeMillis();
        }

        private GuiCursor cursor() {
            return cursor;
        }
    }
}
