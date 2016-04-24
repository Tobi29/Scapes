package org.tobi29.scapes.entity.model;

import org.tobi29.scapes.engine.ScapesEngine;

public class MobLivingModelHumanShared {
    public final Box body, head, legNormalLeft, legNormalRight, armNormalLeft,
            armNormalRight, legThinLeft, legThinRight, armThinLeft,
            armThinRight, bodyNoCull, headNoCull, legNormalLeftNoCull,
            legNormalRightNoCull, armNormalLeftNoCull, armNormalRightNoCull,
            legThinLeftNoCull, legThinRightNoCull, armThinLeftNoCull,
            armThinRightNoCull;

    public MobLivingModelHumanShared(ScapesEngine engine) {
        body = new Box(engine, 0.015625f, -4, -2, -6, 4, 2, 6, 0, 0);
        head = new Box(engine, 0.015625f, -4, -4, 0, 4, 4, 8, 0, 32);
        legNormalLeft = new Box(engine, 0.015625f, -2, -2, -10, 2, 2, 2, 24, 0);
        legNormalRight =
                new Box(engine, 0.015625f, -2, -2, -10, 2, 2, 2, 40, 0);
        armNormalLeft =
                new Box(engine, 0.015625f, -4, -2, -10, 0, 2, 2, 24, 16);
        armNormalRight =
                new Box(engine, 0.015625f, 0, -2, -10, 4, 2, 2, 40, 16);
        legThinLeft = new Box(engine, 0.015625f, -1, -1, -10, 1, 1, 2, 24, 0);
        legThinRight = new Box(engine, 0.015625f, -1, -1, -10, 1, 1, 2, 32, 0);
        armThinLeft = new Box(engine, 0.015625f, -2, -1, -10, 0, 1, 2, 24, 16);
        armThinRight = new Box(engine, 0.015625f, 0, -1, -10, 2, 1, 2, 32, 16);
        bodyNoCull =
                new Box(engine, 0.015625f, -4, -2, -6, 4, 2, 6, 0, 0, false);
        headNoCull =
                new Box(engine, 0.015625f, -4, -4, 0, 4, 4, 8, 0, 32, false);
        legNormalLeftNoCull =
                new Box(engine, 0.015625f, -2, -2, -10, 2, 2, 2, 24, 0, false);
        legNormalRightNoCull =
                new Box(engine, 0.015625f, -2, -2, -10, 2, 2, 2, 40, 0, false);
        armNormalLeftNoCull =
                new Box(engine, 0.015625f, -4, -2, -10, 0, 2, 2, 24, 16, false);
        armNormalRightNoCull =
                new Box(engine, 0.015625f, 0, -2, -10, 4, 2, 2, 40, 16, false);
        legThinLeftNoCull =
                new Box(engine, 0.015625f, -1, -1, -10, 1, 1, 2, 24, 0, false);
        legThinRightNoCull =
                new Box(engine, 0.015625f, -1, -1, -10, 1, 1, 2, 32, 0, false);
        armThinLeftNoCull =
                new Box(engine, 0.015625f, -2, -1, -10, 0, 1, 2, 24, 16, false);
        armThinRightNoCull =
                new Box(engine, 0.015625f, 0, -1, -10, 2, 1, 2, 32, 16, false);
    }
}
