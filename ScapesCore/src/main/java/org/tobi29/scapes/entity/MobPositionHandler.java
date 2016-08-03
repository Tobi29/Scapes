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
package org.tobi29.scapes.entity;

import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.vector.MutableVector3d;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.engine.utils.math.vector.Vector3d;
import org.tobi29.scapes.packets.*;

public class MobPositionHandler {
    private static final double POSITION_SEND_RELATIVE_OFFSET = 0.01,
            POSITION_SEND_ABSOLUTE_OFFSET = 1, SPEED_SEND_OFFSET = 0.01,
            DIRECTION_SEND_OFFSET = 12.25;
    private final PacketHandler packetHandler;
    private final PositionListener positionListener;
    private final SpeedListener speedListener;
    private final RotationListener rotationListener;
    private final StateListener stateListener;
    private final MutableVector3d sentPosRelative, sentPosAbsolute, sentSpeed =
            new MutableVector3d(), sentRot = new MutableVector3d();
    private boolean ground, slidingWall, inWater, swimming, init;

    public MobPositionHandler(Vector3 pos, PacketHandler packetHandler,
            PositionListener positionListener, SpeedListener speedListener,
            RotationListener rotationListener, StateListener stateListener) {
        sentPosRelative = new MutableVector3d(pos);
        sentPosAbsolute = new MutableVector3d(pos);
        this.packetHandler = packetHandler;
        this.positionListener = positionListener;
        this.speedListener = speedListener;
        this.rotationListener = rotationListener;
        this.stateListener = stateListener;
    }

    public void submitUpdate(int entityId, Vector3 pos, Vector3 speed,
            Vector3 rot, boolean ground, boolean slidingWall, boolean inWater,
            boolean swimming) {
        submitUpdate(entityId, pos, speed, rot, ground, slidingWall, inWater,
                swimming, false);
    }

    public void submitUpdate(int entityId, Vector3 pos, Vector3 speed,
            Vector3 rot, boolean ground, boolean slidingWall, boolean inWater,
            boolean swimming, boolean forced) {
        submitUpdate(entityId, pos, speed, rot, ground, slidingWall, inWater,
                swimming, forced, packetHandler);
    }

    public synchronized void submitUpdate(int entityID, Vector3 pos,
            Vector3 speed, Vector3 rot, boolean ground, boolean slidingWall,
            boolean inWater, boolean swimming, boolean forced,
            PacketHandler packetHandler) {
        if (entityID != 0) {
            if (!init) {
                init = true;
                forced = true;
            }
            Vector3 oldPos;
            if (forced) {
                oldPos = null;
            } else {
                oldPos = sentPosAbsolute.now();
            }
            if (forced || FastMath.max(
                    FastMath.abs(sentPosAbsolute.now().minus(pos))) >
                    POSITION_SEND_ABSOLUTE_OFFSET) {
                sendPos(entityID, pos, forced);
                sendSpeed(entityID, speed, forced);
            } else {
                if (FastMath
                        .max(FastMath.abs(sentPosRelative.now().minus(pos))) >
                        POSITION_SEND_RELATIVE_OFFSET) {
                    byte x = (byte) (FastMath
                            .clamp(pos.doubleX() - sentPosRelative.doubleX(),
                                    -1.0, 1.0) * 100.0);
                    byte y = (byte) (FastMath
                            .clamp(pos.doubleY() - sentPosRelative.doubleY(),
                                    -1.0, 1.0) * 100.0);
                    byte z = (byte) (FastMath
                            .clamp(pos.doubleZ() - sentPosRelative.doubleZ(),
                                    -1.0, 1.0) * 100.0);
                    sentPosRelative.plusX(x / 100.0);
                    sentPosRelative.plusY(y / 100.0);
                    sentPosRelative.plusZ(z / 100.0);
                    packetHandler.sendPacket(
                            new PacketMobMoveRelative(entityID, oldPos, x, y,
                                    z));
                }
                if (FastMath.abs(sentSpeed.doubleX()) > SPEED_SEND_OFFSET &&
                        FastMath.abs(speed.doubleX()) <= SPEED_SEND_OFFSET ||
                        FastMath.abs(sentSpeed.doubleY()) > SPEED_SEND_OFFSET &&
                                FastMath.abs(speed.doubleY()) <=
                                        SPEED_SEND_OFFSET ||
                        FastMath.abs(sentSpeed.doubleZ()) > SPEED_SEND_OFFSET &&
                                FastMath.abs(speed.doubleZ()) <=
                                        SPEED_SEND_OFFSET) {
                    sendSpeed(entityID, Vector3d.ZERO, false);
                } else if (FastMath.max(
                        FastMath.abs(sentSpeed.now().minus(speed))) >
                        SPEED_SEND_OFFSET) {
                    sendSpeed(entityID, speed, false);
                }
            }
            if (forced || FastMath.abs(
                    FastMath.angleDiff(sentRot.doubleX(), rot.doubleX())) >
                    DIRECTION_SEND_OFFSET || FastMath.abs(
                    FastMath.angleDiff(sentRot.doubleY(), rot.doubleY())) >
                    DIRECTION_SEND_OFFSET || FastMath.abs(
                    FastMath.angleDiff(sentRot.doubleZ(), rot.doubleZ())) >
                    DIRECTION_SEND_OFFSET) {
                sendRotation(entityID, rot, forced);
            }
            if (forced || this.ground != ground ||
                    this.slidingWall != slidingWall ||
                    this.inWater != inWater || this.swimming != swimming) {
                this.ground = ground;
                this.slidingWall = slidingWall;
                this.inWater = inWater;
                this.swimming = swimming;
                packetHandler.sendPacket(
                        new PacketMobChangeState(entityID, oldPos, ground,
                                slidingWall, inWater, swimming));
            }
        }
    }

    public void sendPos(int entityID, Vector3 pos, boolean forced) {
        sendPos(entityID, pos, forced, packetHandler);
    }

    public void sendPos(int entityID, Vector3 pos, boolean forced,
            PacketHandler packetHandler) {
        Vector3 oldPos;
        if (forced) {
            oldPos = null;
        } else {
            oldPos = sentPosAbsolute.now();
        }
        sentPosRelative.set(pos);
        sentPosAbsolute.set(pos);
        packetHandler.sendPacket(
                new PacketMobMoveAbsolute(entityID, oldPos, pos.doubleX(),
                        pos.doubleY(), pos.doubleZ()));
    }

    public void sendRotation(int entityID, Vector3 rot, boolean forced) {
        sendRotation(entityID, rot, forced, packetHandler);
    }

    public void sendRotation(int entityID, Vector3 rot, boolean forced,
            PacketHandler packetHandler) {
        sentRot.set(rot);
        packetHandler.sendPacket(new PacketMobChangeRot(entityID,
                forced ? null : sentPosAbsolute.now(), rot.floatX(),
                rot.floatY(), rot.floatZ()));
    }

    public void sendSpeed(int entityID, Vector3 speed, boolean forced) {
        sendSpeed(entityID, speed, forced, packetHandler);
    }

    public void sendSpeed(int entityID, Vector3 speed, boolean forced,
            PacketHandler packetHandler) {
        sentSpeed.set(speed);
        packetHandler.sendPacket(new PacketMobChangeSpeed(entityID,
                forced ? null : sentPosAbsolute.now(), speed.doubleX(),
                speed.doubleY(), speed.doubleZ()));
    }

    public void receiveMoveRelative(byte x, byte y, byte z) {
        double xx = x / 100.0;
        double yy = y / 100.0;
        double zz = z / 100.0;
        sentPosRelative.plusX(xx);
        sentPosRelative.plusY(yy);
        sentPosRelative.plusZ(zz);
        positionListener.change(sentPosRelative.now());
    }

    public void receiveMoveAbsolute(double x, double y, double z) {
        sentPosRelative.setX(x);
        sentPosRelative.setY(y);
        sentPosRelative.setZ(z);
        sentPosAbsolute.setX(x);
        sentPosAbsolute.setY(y);
        sentPosAbsolute.setZ(z);
        positionListener.change(sentPosAbsolute.now());
    }

    public void receiveRotation(double xRot, double yRot, double zRot) {
        sentRot.setX(xRot);
        sentRot.setY(yRot);
        sentRot.setZ(zRot);
        rotationListener.change(sentRot.now());
    }

    public void receiveSpeed(double xSpeed, double ySpeed, double zSpeed) {
        sentSpeed.setX(xSpeed);
        sentSpeed.setY(ySpeed);
        sentSpeed.setZ(zSpeed);
        speedListener.change(sentSpeed.now());
    }

    public void receiveState(boolean ground, boolean slidingWall,
            boolean inWater, boolean swimming) {
        this.ground = ground;
        this.slidingWall = slidingWall;
        this.inWater = inWater;
        this.swimming = swimming;
        stateListener.change(ground, slidingWall, inWater, swimming);
    }

    public interface PacketHandler {
        void sendPacket(PacketBoth packet);
    }

    public interface PositionListener {
        void change(Vector3 pos);
    }

    public interface SpeedListener {
        void change(Vector3 speed);
    }

    public interface RotationListener {
        void change(Vector3 rot);
    }

    public interface StateListener {
        void change(boolean ground, boolean slidingWall, boolean inWater,
                boolean swimming);
    }
}
