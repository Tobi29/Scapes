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

package org.tobi29.scapes.server.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.engine.utils.RSAUtil;
import org.tobi29.scapes.engine.utils.io.ChecksumUtil;
import org.tobi29.scapes.engine.utils.io.tag.MultiTag;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.server.connection.PlayerConnection;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PlayerBans implements MultiTag.Writeable {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(PlayerBans.class);
    private final Set<Entry> entries =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

    public PlayerBans() {
    }

    public PlayerBans(TagStructure tagStructure) {
        tagStructure.getList("Entries").stream().map(Entry::read)
                .forEach(entries::add);
    }

    public Optional<String> matches(PlayerConnection player) {
        PublicKey key = player.key();
        Optional<InetAddress> address = player.address();
        String nickname = player.nickname();
        return entries.stream()
                .map(entry -> entry.matches(key, address, nickname))
                .filter(Optional::isPresent).map(Optional::get).findAny();
    }

    public void ban(PlayerConnection player, String id, String reason,
            boolean banKey, boolean banAddress, boolean banNickname) {
        Optional<PublicKey> key;
        if (banKey) {
            key = Optional.of(player.key());
        } else {
            key = Optional.empty();
        }
        Optional<String> address;
        if (banAddress) {
            address = player.address().map(InetAddress::getHostAddress);
        } else {
            address = Optional.empty();
        }
        Optional<String> nickname;
        if (banNickname) {
            nickname = Optional.of(player.nickname());
        } else {
            nickname = Optional.empty();
        }
        entries.add(new Entry(key, address, nickname, id, reason));
    }

    public void unban(String id) {
        entries.removeAll(entries.stream().filter(entry -> entry.id.equals(id))
                .collect(Collectors.toList()));
    }

    public Stream<Entry> entries() {
        return entries.stream();
    }

    @Override
    public TagStructure write() {
        TagStructure tagStructure = new TagStructure();
        tagStructure.setList("Entries", entries.stream().map(Entry::write)
                .collect(Collectors.toList()));
        return tagStructure;
    }

    public static class Entry implements MultiTag.Writeable {
        private final Optional<PublicKey> key;
        private final Optional<String> address, nickname;
        private final String id, reason;

        private Entry(Optional<PublicKey> key, Optional<String> address,
                Optional<String> nickname, String id, String reason) {
            this.key = key;
            this.address = address;
            this.nickname = nickname;
            this.id = id;
            this.reason = reason;
        }

        private static Entry read(TagStructure tagStructure) {
            Optional<PublicKey> key = Optional.empty();
            if (tagStructure.has("Key")) {
                try {
                    key = Optional.of(
                            RSAUtil.readPublic(tagStructure.getString("Key")));
                } catch (InvalidKeySpecException e) {
                    LOGGER.warn("Failed to read key: {}", e.toString());
                }
            }
            Optional<String> address = Optional.empty();
            if (tagStructure.has("Address")) {
                address = Optional.of(tagStructure.getString("Address"));
            }
            Optional<String> nickname = Optional.empty();
            if (tagStructure.has("Nickname")) {
                address = Optional.of(tagStructure.getString("Nickname"));
            }
            String id = tagStructure.getString("ID");
            String reason = tagStructure.getString("Reason");
            return new Entry(key, address, nickname, id, reason);
        }

        public Optional<String> matches(PublicKey key,
                Optional<InetAddress> address, String nickname) {
            if (this.key.isPresent() && this.key.get().equals(key)) {
                return Optional.of("Banned account: " + reason);
            }
            if (this.address.isPresent() && address.isPresent()) {
                try {
                    InetAddress playerAddress = address.get();
                    InetAddress[] addresses =
                            InetAddress.getAllByName(this.address.get());
                    for (InetAddress resolve : addresses) {
                        if (resolve.equals(playerAddress)) {
                            return Optional.of("Banned address: " + reason);
                        }
                    }
                } catch (UnknownHostException e) {
                    LOGGER.warn("Failed to resolve address: {}, {}",
                            this.address.get(), e.toString());
                }
            }
            if (this.nickname.isPresent() &&
                    this.nickname.get().equals(nickname)) {
                return Optional.of("Banned nickname: " + reason);
            }
            return Optional.empty();
        }

        @Override
        public String toString() {
            StringBuilder str = new StringBuilder(24);
            str.append(id).append(' ').append(reason).append(" (");
            boolean space = false;
            if (key.isPresent()) {
                space = true;
                str.append("Key: ").append(ChecksumUtil
                        .getChecksum(key.get().getEncoded()));
            }
            if (address.isPresent()) {
                if (space) {
                    str.append(' ');
                } else {
                    space = true;
                }
                str.append("Address: ").append(address.get());
            }
            if (nickname.isPresent()) {
                if (space) {
                    str.append(' ');
                }
                str.append("Nickname: ").append(nickname.get());
            }
            str.append(')');
            return str.toString();
        }

        @Override
        public TagStructure write() {
            TagStructure tagStructure = new TagStructure();
            if (key.isPresent()) {
                try {
                    tagStructure
                            .setString("Key", RSAUtil.writePublic(key.get()));
                } catch (InvalidKeySpecException e) {
                    LOGGER.warn("Failed to store key: {}", e.toString());
                }
            }
            address.ifPresent(
                    address -> tagStructure.setString("Address", address));
            nickname.ifPresent(
                    address -> tagStructure.setString("Nickname", address));
            tagStructure.setString("ID", id);
            tagStructure.setString("Reason", reason);
            return tagStructure;
        }
    }
}
