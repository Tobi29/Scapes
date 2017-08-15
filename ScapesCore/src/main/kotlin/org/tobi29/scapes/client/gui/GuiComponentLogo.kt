/*
 * Copyright 2012-2017 Tobi29
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
package org.tobi29.scapes.client.gui

import org.threeten.bp.Month
import org.tobi29.scapes.engine.chrono.timeZoneLocal
import org.tobi29.scapes.engine.gui.*
import org.tobi29.scapes.engine.sound.CLICK
import org.tobi29.scapes.engine.utils.math.round
import org.tobi29.scapes.engine.utils.math.threadLocalRandom
import org.tobi29.scapes.engine.utils.systemClock

class GuiComponentLogo(parent: GuiLayoutData,
                       height: Int,
                       textSize: Int) : GuiComponentGroup(
        parent) {
    private val splash: GuiComponentText

    init {
        val textX = height - 8
        val textY = textSize shr 2
        add(0.0, 0.0, height.toDouble(), height.toDouble()) {
            GuiComponentIcon(it, engine.graphics.textures["Scapes:image/Icon"])
        }
        add(textX.toDouble(), textY.toDouble(), -1.0, textSize.toDouble()) {
            GuiComponentText(it, "Scapes", 1.0f, 1.0f, 1.0f, 1.0f)
        }
        splash = add(textX.toDouble(),
                (textY + round(textSize * 1.2)).toDouble(), -1.0,
                ((textSize shl 1) / 3).toDouble()) {
            GuiComponentText(it, splash(), 1.0f, 1.0f, 0.0f, 1.0f)
        }
        on(GuiEvent.CLICK_LEFT) { event ->
            engine.sounds.playSound(CLICK, "sound.GUI", 1.0, 1.0)
            splash.text = splash()
        }
    }

    companion object {
        private val SPLASHES = arrayOf("Hi!", "Shut up!", "<3", ";)", "9001",
                "Minecraft", "Please\nstand back!", "You\nsuck!", "Crap!",
                "Holy\nsheet!", "<- Pickaxe", "Whatever", "Minceraft", "OpenGL",
                "OpenAL", "Sepacs", "<Insert bad\npun here>",
                "Orange\nSmoke\nBlue\nCup", "Prepare!", "Hm...",
                "Fatal\nerror!", "java.util.\nRandom\nfor ya",
                "Java:\n" + System.getProperty("java.version"),
                "Hello,\n" + System.getProperty("user.name"))

        private fun splash(): String {
            val text: String
            val (date, _) = timeZoneLocal.encode(systemClock()).first()
            if (date.day == 1 && date.month == Month.APRIL) {
                text = "COMIC\nSANS!!!"
            } else if (date.day == 29 && date.month == Month.FEBRUARY) {
                text = "Best day\never!"
            } else if (date.day == 24 && date.month == Month.DECEMBER) {
                text = "Merry\nChristmas!"
            } else if (date.day == 1 && date.month == Month.JANUARY) {
                text = "Happy new\nyear!"
            } else if (date.day == 3 && date.month == Month.MARCH) {
                text = "${String(CharArray(1024, { ' ' }))}Aspect ratio!?!"
            } else {
                val random = threadLocalRandom()
                text = SPLASHES[random.nextInt(SPLASHES.size)]
            }
            return text
        }
    }
}
