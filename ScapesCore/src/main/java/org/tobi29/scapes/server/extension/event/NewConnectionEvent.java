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

package org.tobi29.scapes.server.extension.event;

import java8.util.Objects;
import org.tobi29.scapes.server.extension.ServerEvent;

import java.nio.channels.SocketChannel;

public class NewConnectionEvent extends ServerEvent {
    private final SocketChannel channel;
    private boolean success = true;
    private String reason = "No reason given";

    public NewConnectionEvent(SocketChannel channel) {
        this.channel = channel;
    }

    public SocketChannel channel() {
        return channel;
    }

    public boolean success() {
        return success;
    }

    public String reason() {
        return reason;
    }

    public void deny(String reason) {
        Objects.requireNonNull(reason);
        success = false;
        this.reason = reason;
    }

    public void allow() {
        success = true;
    }
}
