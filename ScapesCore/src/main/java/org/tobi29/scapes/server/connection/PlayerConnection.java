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
package org.tobi29.scapes.server.connection;

import java8.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.Debug;
import org.tobi29.scapes.block.GameRegistry;
import org.tobi29.scapes.chunk.WorldServer;
import org.tobi29.scapes.connection.PlayConnection;
import org.tobi29.scapes.engine.server.Account;
import org.tobi29.scapes.engine.server.Connection;
import org.tobi29.scapes.engine.server.ConnectionCloseException;
import org.tobi29.scapes.engine.utils.io.IORunnable;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.entity.server.MobPlayerServer;
import org.tobi29.scapes.entity.skin.ServerSkin;
import org.tobi29.scapes.packets.Packet;
import org.tobi29.scapes.packets.PacketChat;
import org.tobi29.scapes.packets.PacketDisconnect;
import org.tobi29.scapes.packets.PacketSetWorld;
import org.tobi29.scapes.server.MessageLevel;
import org.tobi29.scapes.server.PlayerEntry;
import org.tobi29.scapes.server.command.Command;
import org.tobi29.scapes.server.extension.event.PlayerAuthenticateEvent;
import org.tobi29.scapes.server.format.PlayerStatistics;

import java.io.IOException;

public abstract class PlayerConnection
        implements Connection, PlayConnection, Command.Executor {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(PlayerConnection.class);
    protected final ServerConnection server;
    protected final GameRegistry registry;
    protected MobPlayerServer entity;
    protected ServerSkin skin;
    protected String id, nickname = "_Error_";
    protected boolean added;
    protected int loadingRadius, permissionLevel;

    protected PlayerConnection(ServerConnection server) {
        this.server = server;
        registry = server.plugins().registry();
    }

    public MobPlayerServer mob() {
        return entity;
    }

    public ServerSkin skin() {
        return skin;
    }

    public String id() {
        return id;
    }

    public String nickname() {
        return nickname;
    }

    public ServerConnection server() {
        return server;
    }

    public synchronized void setWorld() {
        setWorld(null);
    }

    public synchronized void setWorld(WorldServer world) {
        setWorld(world, null);
    }

    public synchronized void setWorld(WorldServer world, Vector3 pos) {
        removeEntity();
        PlayerEntry player = server.server().player(id);
        permissionLevel = player.permissions();
        Optional<MobPlayerServer> newEntity =
                player.createEntity(this, Optional.ofNullable(world));
        if (!newEntity.isPresent()) {
            disconnect("Unable to spawn in world");
        }
        MobPlayerServer entity = newEntity.get();
        if (pos != null) {
            entity.setPos(pos);
        }
        this.entity = entity;
        entity.world().addEntity(entity);
        task(() -> transmit(new PacketSetWorld(entity.world(), entity)));
        entity.world().addPlayer(entity);
    }

    protected synchronized void save() {
        server.server().save(id, entity, permissionLevel);
    }

    protected Optional<String> generateResponse(boolean challengeMatch) {
        if (!server.doesAllowJoin()) {
            return Optional.of("Server not public!");
        }
        if (!challengeMatch) {
            return Optional.of("Invalid protected key!");
        }
        if (!server.server().playerExists(id)) {
            if (!server.doesAllowCreation()) {
                return Optional
                        .of("This server does not allow account creation!");
            }
        }
        Optional<String> nicknameCheck = Account.isNameValid(nickname);
        if (nicknameCheck.isPresent()) {
            return nicknameCheck;
        }
        PlayerAuthenticateEvent event = new PlayerAuthenticateEvent(this);
        server.server().extensions().fireEvent(event);
        if (!event.success()) {
            return Optional.of(event.reason());
        }
        return Optional.empty();
    }

    public int loadingRadius() {
        return loadingRadius;
    }

    protected void sendPacket(Packet packet) throws IOException {
        MobPlayerServer entity = this.entity;
        if (entity == null) {
            return;
        }
        Vector3 pos3d = packet.pos();
        if (pos3d != null) {
            WorldServer world = entity.world();
            if (world != null && !world.getTerrain()
                    .isBlockSendable(entity, pos3d.intX(), pos3d.intY(),
                            pos3d.intZ(), packet.isChunkContent())) {
                return;
            }
            double range = packet.range();
            if (range > 0.0) {
                if (FastMath.pointDistanceSqr(pos3d, entity.pos()) >
                        range * range) {
                    return;
                }
            }
        }
        transmit(packet);
    }

    protected abstract void task(IORunnable runnable);

    protected abstract void transmit(Packet packet) throws IOException;

    @Override
    public synchronized void close() throws IOException {
        if (added) {
            server.removePlayer(this);
            added = false;
        }
        removeEntity();
    }

    protected void removeEntity() {
        MobPlayerServer entity = this.entity;
        if (entity != null) {
            entity.world().deleteEntity(entity);
            entity.world().removePlayer(entity);
            save();
        }
    }

    @Override
    public Optional<String> playerName() {
        return Optional.of(nickname);
    }

    @Override
    public String name() {
        return nickname;
    }

    @Override
    public boolean message(String message, MessageLevel level) {
        if (level.level() < MessageLevel.CHAT.level()) {
            return false;
        }
        send(new PacketChat(message));
        return true;
    }

    @Override
    public int permissionLevel() {
        if (Debug.enabled()) {
            return 10;
        }
        return permissionLevel;
    }

    public void setPermissionLevel(int permissionLevel) {
        this.permissionLevel = permissionLevel;
    }

    public void disconnect(String reason) {
        task(() -> {
            sendPacket(new PacketDisconnect(reason));
            throw new ConnectionCloseException(reason);
        });
        removeEntity();
    }
}
