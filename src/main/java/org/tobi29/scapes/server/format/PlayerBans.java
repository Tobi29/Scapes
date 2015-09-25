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
import org.tobi29.scapes.engine.utils.io.tag.MultiTag;
import org.tobi29.scapes.engine.utils.io.tag.TagStructure;
import org.tobi29.scapes.server.connection.PlayerConnection;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
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
        String id = player.id();
        Optional<InetAddress> address = player.address();
        String nickname = player.nickname();
        return entries.stream()
                .map(entry -> entry.matches(id, address, nickname))
                .filter(Optional::isPresent).map(Optional::get).findAny();
    }

    public Stream<Entry> matchesID(Pattern pattern) {
        return entries.stream().filter(entry -> entry.matchesID(pattern));
    }

    public Stream<Entry> matchesNickname(Pattern pattern) {
        return entries.stream().filter(entry -> entry.matchesNickname(pattern));
    }

    public void ban(PlayerConnection player, String reason, boolean banID,
            boolean banAddress, boolean banNickname) {
        Optional<String> id;
        if (banID) {
            id = Optional.of(player.id());
        } else {
            id = Optional.empty();
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
        entries.add(new Entry(id, address, nickname, reason));
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
        private final Optional<String> id, address, nickname;
        private final String reason;

        private Entry(Optional<String> id, Optional<String> address,
                Optional<String> nickname, String reason) {
            this.id = id;
            this.address = address;
            this.nickname = nickname;
            this.reason = reason;
        }

        private static Entry read(TagStructure tagStructure) {
            Optional<String> id = Optional.empty();
            if (tagStructure.has("ID")) {
                id = Optional.of(tagStructure.getString("ID"));
            }
            Optional<String> address = Optional.empty();
            if (tagStructure.has("Address")) {
                address = Optional.of(tagStructure.getString("Address"));
            }
            Optional<String> nickname = Optional.empty();
            if (tagStructure.has("Nickname")) {
                nickname = Optional.of(tagStructure.getString("Nickname"));
            }
            String reason = tagStructure.getString("Reason");
            return new Entry(id, address, nickname, reason);
        }

        public Optional<String> matches(String id,
                Optional<InetAddress> address, String nickname) {
            if (this.id.isPresent() && this.id.get().equals(id)) {
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

        public boolean matchesNickname(Pattern pattern) {
            return nickname.isPresent() &&
                    pattern.matcher(nickname.get()).matches();
        }

        public boolean matchesID(Pattern pattern) {
            return id.isPresent() && pattern.matcher(id.get()).matches();
        }

        @Override
        public String toString() {
            StringBuilder str = new StringBuilder(24);
            if (id.isPresent()) {
                str.append("ID: ").append(id.get()).append(' ');
            }
            if (address.isPresent()) {
                str.append("Address: ").append(address.get()).append(' ');
            }
            if (nickname.isPresent()) {
                str.append("Nickname: ").append(nickname.get()).append(' ');
            }
            str.append(' ').append(reason);
            return str.toString();
        }

        @Override
        public TagStructure write() {
            TagStructure tagStructure = new TagStructure();
            id.ifPresent(id -> tagStructure.setString("ID", id));
            address.ifPresent(
                    address -> tagStructure.setString("Address", address));
            nickname.ifPresent(
                    nickname -> tagStructure.setString("Nickname", nickname));
            tagStructure.setString("Reason", reason);
            return tagStructure;
        }
    }
}
