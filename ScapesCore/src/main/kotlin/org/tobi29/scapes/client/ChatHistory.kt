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
package org.tobi29.scapes.client

import org.tobi29.scapes.engine.utils.EventDispatcher
import java.util.*

class ChatHistory {
    val events = EventDispatcher()
    private val lines = ArrayList<ChatLine>()

    @Synchronized fun addLine(text: String) {
        val lines = text.split(
                "\n".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()
        for (line in lines) {
            this.lines.add(0, ChatLine(line))
        }
        events.fire(ChatChangeEvent(this))
    }

    @Synchronized fun update() {
        val time = System.currentTimeMillis()
        val removals = lines.filter { line -> time - line.time > 10000 }
        if (!removals.isEmpty()) {
            lines.removeAll(removals)
            events.fire(ChatChangeEvent(this))
        }
    }

    @Synchronized fun lines(block: (String) -> Unit) {
        lines.asSequence().map { it.text }.forEach(block)
    }

    private class ChatLine(val text: String) {
        val time: Long

        init {
            time = System.currentTimeMillis()
        }
    }
}

class ChatChangeEvent(val chatHistory: ChatHistory)
