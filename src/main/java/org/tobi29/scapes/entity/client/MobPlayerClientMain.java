package org.tobi29.scapes.entity.client;

import org.tobi29.scapes.block.AABBElement;
import org.tobi29.scapes.block.BlockType;
import org.tobi29.scapes.block.TerrainTexture;
import org.tobi29.scapes.chunk.WorldClient;
import org.tobi29.scapes.chunk.terrain.TerrainClient;
import org.tobi29.scapes.client.connection.ClientConnection;
import org.tobi29.scapes.client.states.GameStateGameMP;
import org.tobi29.scapes.engine.gui.Gui;
import org.tobi29.scapes.engine.utils.Pool;
import org.tobi29.scapes.engine.utils.math.AABB;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.Frustum;
import org.tobi29.scapes.engine.utils.math.PointerPane;
import org.tobi29.scapes.engine.utils.math.vector.Vector2;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.engine.utils.math.vector.Vector3d;
import org.tobi29.scapes.entity.particle.ParticleBlock;
import org.tobi29.scapes.entity.particle.ParticleManager;

import java.util.Iterator;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public abstract class MobPlayerClientMain extends MobPlayerClient {
    protected final GameStateGameMP game;
    protected Gui currentGui;
    protected boolean flying;
    protected int swim;
    protected double gravitationMultiplier = 1.0, airFriction = 0.2,
            groundFriction = 1.6, wallFriction = 2.0, waterFriction = 8.0,
            stepHeight = 1.0;

    protected MobPlayerClientMain(WorldClient world, Vector3 pos, Vector3 speed,
            AABB aabb, double lives, double maxLives, Frustum viewField,
            Frustum hitField, String nickname) {
        super(world, pos, speed, aabb, lives, maxLives, viewField, hitField,
                nickname);
        game = world.game();
    }

    protected static Iterator<AABB> collisions(Pool<AABBElement> aabbs) {
        return aabbs.stream().filter(AABBElement::isSolid)
                .map(AABBElement::aabb).iterator();
    }

    protected void updateVelocity(double gravitation, double delta) {
        speed.div(1.0 + airFriction * delta);
        if (inWater) {
            speed.div(1.0 + waterFriction * delta);
        } else {
            if (ground) {
                speed.div(1.0 + groundFriction * delta * gravitation);
            }
            if (slidingWall) {
                speed.div(1.0 + wallFriction * delta);
            }
        }
        speed.plusZ(-gravitation * gravitationMultiplier * delta);
    }

    protected void move(AABB aabb, Pool<AABBElement> aabbs, double goX,
            double goY, double goZ) {
        boolean ground = false;
        boolean slidingWall = false;
        double lastGoZ = aabb.moveOutZ(collisions(aabbs), goZ);
        pos.plusZ(lastGoZ);
        aabb.add(0, 0, lastGoZ);
        if (lastGoZ - goZ > 0) {
            ground = true;
        }
        // Walk
        boolean walking = true;
        while (walking) {
            walking = false;
            if (goX != 0.0) {
                double lastGoX = aabb.moveOutX(collisions(aabbs), goX);
                if (lastGoX != 0.0) {
                    pos.plusX(lastGoX);
                    aabb.add(lastGoX, 0.0, 0.0);
                    goX -= lastGoX;
                    walking = true;
                }
            }
            if (goY != 0.0) {
                double lastGoY = aabb.moveOutY(collisions(aabbs), goY);
                if (lastGoY != 0.0) {
                    pos.plusY(lastGoY);
                    aabb.add(0.0, lastGoY, 0.0);
                    goY -= lastGoY;
                    walking = true;
                }
            }
        }
        // Check collision
        boolean slidingX = goX != 0.0;
        boolean slidingY = goY != 0.0;
        if (slidingX || slidingY) {
            if (stepHeight > 0.0 && (this.ground || inWater)) {
                // Step
                // Calculate step height
                AABB aabbStep = new AABB(aabb).add(goX, 0.0, 0.0);
                double stepX = aabbStep.moveOutZ(collisions(aabbs), stepHeight);
                aabbStep = new AABB(aabb).add(0.0, goY, 0.0);
                double stepY = aabbStep.moveOutZ(collisions(aabbs), stepHeight);
                double step = FastMath.max(stepX, stepY);
                aabbStep = new AABB(aabb).add(goX, goY, step);
                step += aabbStep.moveOutZ(collisions(aabbs), -step);
                // Check step height
                aabbStep.copy(aabb).add(0.0, 0.0, step);
                step = aabb.moveOutZ(collisions(aabbs), step);
                // Attempt walk at new height
                double lastGoX = aabbStep.moveOutX(collisions(aabbs), goX);
                aabbStep.add(lastGoX, 0.0, 0.0);
                double lastGoY = aabbStep.moveOutY(collisions(aabbs), goY);
                // Check if walk was successful
                if (lastGoX != 0.0 || lastGoY != 0.0) {
                    pos.plusX(lastGoX);
                    pos.plusY(lastGoY);
                    aabb.copy(aabbStep).add(0.0, lastGoY, 0.0);
                    pos.plusZ(step);
                } else {
                    // Collide
                    slidingWall = true;
                    if (slidingX) {
                        speed.setX(0.0);
                    }
                    if (slidingY) {
                        speed.setY(0.0);
                    }
                }
            } else {
                // Collide
                slidingWall = true;
                if (slidingX) {
                    speed.setX(0.0);
                }
                if (slidingY) {
                    speed.setY(0.0);
                }
            }
        }
        this.ground = ground;
        this.slidingWall = slidingWall;
    }

    protected void collide(AABB aabb, Pool<AABBElement> aabbs) {
        boolean inWater = false;
        boolean swimming;
        for (AABBElement element : aabbs) {
            if (aabb.overlay(element.aabb)) {
                if (element.collision.isLiquid()) {
                    inWater = true;
                }
            }
        }
        aabb.minZ = FastMath.mix(aabb.minZ, aabb.maxZ, 0.6);
        boolean water = false;
        for (AABBElement element : aabbs) {
            if (aabb.overlay(element.aabb)) {
                if (element.collision.isLiquid()) {
                    water = true;
                }
            }
        }
        if (water) {
            swim++;
            swimming = swim > 1;
        } else {
            swimming = false;
            swim = 0;
        }
        this.inWater = inWater;
        this.swimming = swimming;
    }

    public void updatePosition() {
        positionHandler
                .submitUpdate(entityID, pos.now(), speed.now(), rot.now(),
                        ground, slidingWall, inWater, swimming, true);
    }

    @Override
    public void onDamage(double damage) {
        game.client().world().scene().damageShake(damage);
    }

    @Override
    public void move(double delta) {
        updateVelocity(flying ? 0.0 : world.gravity(), delta);
        double goX = FastMath.clamp(speed.doubleX() * delta, -1.0, 1.0);
        double goY = FastMath.clamp(speed.doubleY() * delta, -1.0, 1.0);
        double goZ = FastMath.clamp(speed.doubleZ() * delta, -1.0, 1.0);
        if (flying) {
            pos.plus(new Vector3d(goX, goY, goZ));
        } else {
            AABB aabb = aabb();
            Pool<AABBElement> aabbs = world.terrain().collisions(
                    FastMath.floor(aabb.minX + FastMath.min(goX, 0.0)),
                    FastMath.floor(aabb.minY + FastMath.min(goY, 0.0)),
                    FastMath.floor(aabb.minZ + FastMath.min(goZ, 0.0)),
                    FastMath.floor(aabb.maxX + FastMath.max(goX, 0.0)),
                    FastMath.floor(aabb.maxY + FastMath.max(goY, 0.0)),
                    FastMath.floor(aabb.maxZ + FastMath.max(goZ, stepHeight)));
            move(aabb, aabbs, goX, goY, goZ);
            if (ground) {
                speed.setZ(speed.doubleZ() / (1.0 + 4.0 * delta));
            }
            headInWater = world.terrain().type(pos.intX(), pos.intY(),
                    FastMath.floor(pos.doubleZ() + 0.7)).isLiquid();
            collide(aabb, aabbs);
            aabbs.reset();
        }
        positionHandler
                .submitUpdate(entityID, pos.now(), speed.now(), rot.now(),
                        ground, slidingWall, inWater, swimming);
        double lookX = FastMath.cosTable(rot.doubleZ() * FastMath.PI / 180) *
                FastMath.cosTable(rot.doubleX() * FastMath.PI / 180) * 6;
        double lookY = FastMath.sinTable(rot.doubleZ() * FastMath.PI / 180) *
                FastMath.cosTable(rot.doubleX() * FastMath.PI / 180) * 6;
        double lookZ = FastMath.sinTable(rot.doubleX() * FastMath.PI / 180) * 6;
        Vector3 viewOffset = viewOffset();
        viewField.setView(pos.doubleX() + viewOffset.doubleX(),
                pos.doubleY() + viewOffset.doubleY(),
                pos.doubleZ() + viewOffset.doubleZ(), pos.doubleX() + lookX,
                pos.doubleY() + lookY, pos.doubleZ() + lookZ, 0, 0, 1);
        world.entities().filter(entity -> entity instanceof MobClient)
                .forEach(entity -> {
                    MobClient mob = (MobClient) entity;
                    if (viewField.inView(mob.aabb()) > 0) {
                        if (!world.checkBlocked(pos.intX(), pos.intY(),
                                pos.intZ(), entity.pos.intX(),
                                entity.pos.intY(), entity.pos.intZ())) {
                            onNotice(mob);
                        }
                    }
                });
        footStep -= delta;
        if (footStep <= 0.0) {
            footStep = 0.0;
            if (FastMath.max(FastMath.abs((Vector2) speed.now())) > 0.1) {
                int x = pos.intX(), y = pos.intY(), z =
                        FastMath.floor(pos.doubleZ() - 0.1);
                String footSteepSound = world.terrain().type(x, y, z)
                        .footStepSound(world.terrain().data(x, y, z));
                if (footSteepSound.isEmpty() && ground) {
                    z = FastMath.floor(pos.doubleZ() - 1.4);
                    footSteepSound = world.terrain().type(x, y, z)
                            .footStepSound(world.terrain().data(x, y, z));
                }
                if (!footSteepSound.isEmpty()) {
                    Random random = ThreadLocalRandom.current();
                    game.engine().sounds()
                            .playSound(footSteepSound, "sound.World",
                                    0.9f + random.nextFloat() * 0.2f, 1.0f);
                    footStep = 1.0 /
                            FastMath.clamp(FastMath.length(speed.now()), 1.0,
                                    4.0);
                }
            }
        }
        if (invincibleTicks > 0) {
            invincibleTicks--;
        }
    }

    protected void breakParticles(TerrainClient terrain, int amount) {
        PointerPane pane = block(6);
        if (pane != null) {
            BlockType type = terrain.type(pane.x, pane.y, pane.z);
            Optional<TerrainTexture> tex =
                    type.particleTexture(pane.face, terrain, pane.x, pane.y,
                            pane.z);
            if (tex.isPresent()) {
                TerrainTexture texture = tex.get();
                Vector3 blockPos = new Vector3d(pane.x, pane.y, pane.z);
                ParticleManager particleManager = world.particleManager();
                Random random = ThreadLocalRandom.current();
                for (int i = 0; i < amount; i++) {
                    particleManager.add(new ParticleBlock(particleManager,
                            blockPos.plus(new Vector3d(random.nextDouble(),
                                    random.nextDouble(), random.nextDouble())),
                            new Vector3d(-1.0 + random.nextDouble() * 2.0,
                                    -1.0 + random.nextDouble() * 2.0,
                                    random.nextDouble() * 2.0 + 1.0), texture,
                            random.nextFloat() * 360,
                            type.particleColorR(pane.face, terrain, pane.x,
                                    pane.y, pane.z),
                            type.particleColorG(pane.face, terrain, pane.x,
                                    pane.y, pane.z),
                            type.particleColorB(pane.face, terrain, pane.x,
                                    pane.y, pane.z), 1.0f));
                }
            }
        }
    }

    public synchronized void openGui(Gui gui) {
        if (currentGui != null) {
            closeGui();
        }
        game.add(gui);
        currentGui = gui;
        game.client().world().scene().setHudVisible(false);
    }

    public boolean hasGui() {
        return currentGui != null;
    }

    public synchronized void closeGui() {
        if (currentGui != null) {
            game.remove(currentGui);
            currentGui = null;
            game.client().world().scene().setHudVisible(true);
        }
    }

    public GameStateGameMP game() {
        return game;
    }

    public ClientConnection connection() {
        return game.client();
    }

    public interface Controller {
        Vector2 walk();

        Vector2 camera(double delta);

        boolean left();

        boolean right();

        boolean jump();

        boolean inventory();

        boolean menu();

        boolean chat();

        int hotbarLeft(int previous);

        int hotbarRight(int previous);
    }
}
