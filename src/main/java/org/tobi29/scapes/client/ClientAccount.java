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

package org.tobi29.scapes.client;

import org.tobi29.scapes.connection.AccountUtil;
import org.tobi29.scapes.engine.utils.io.ChecksumUtil;

import java.util.UUID;

public class ClientAccount {
    private UUID uuid;
    private String password, nickname;

    public ClientAccount(UUID uuid, String password, String nickname) {
        this.uuid = uuid;
        this.password = password;
        this.nickname = nickname;
    }

    public String getID(UUID server) {
        return ChecksumUtil.getChecksum(uuid.toString() + server);
    }

    public String getKey(UUID server) {
        return ChecksumUtil.getChecksum(password + server);
    }

    public UUID getUUID() {
        return uuid;
    }

    public void setUUID(UUID uuid) {
        this.uuid = uuid;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public boolean isValid() {
        return AccountUtil.isNameValid(nickname) == null && !password.isEmpty();
    }
}
