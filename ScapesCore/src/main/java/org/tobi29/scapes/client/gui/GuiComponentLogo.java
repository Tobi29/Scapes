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
package org.tobi29.scapes.client.gui;

import org.threeten.bp.LocalDate;
import org.threeten.bp.Month;
import org.tobi29.scapes.engine.gui.GuiComponent;
import org.tobi29.scapes.engine.gui.GuiComponentIcon;
import org.tobi29.scapes.engine.gui.GuiComponentText;
import org.tobi29.scapes.engine.gui.GuiLayoutData;
import org.tobi29.scapes.engine.utils.math.FastMath;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class GuiComponentLogo extends GuiComponent {
    private static final String[] SPLASHES =
            {"Hi!", "Shut up!", "<3", ";)", "9001", "Minecraft",
                    "Please\nstand back!", "You\nsuck!", "Crap!",
                    "Holy\nsheet!", "<- Pickaxe", "Whatever", "Minceraft",
                    "OpenGL", "OpenAL", "Sepacs", "<Insert bad\npun here>",
                    "Orange\nSmoke\nBlue\nCup", "Prepare!", "Hm...",
                    "Fatal\nerror!", "java.util.\nRandom\nfor ya",
                    "Java:\n" + System.getProperty("java.version"),
                    "Hello,\n" + System.getProperty("user.name")};
    private final GuiComponentText splash;

    public GuiComponentLogo(GuiLayoutData parent, int height, int textSize) {
        super(parent);
        int textX = height - 8;
        int textY = textSize >> 2;
        addSub(0, 0, height, height,
                p -> new GuiComponentIcon(p, "Scapes:image/Icon"));
        addSub(textX, textY, -1, textSize,
                p -> new GuiComponentText(p, "Scapes", 1.0f, 1.0f, 1.0f, 1.0f));
        splash = addSub(textX, textY + FastMath.round(textSize * 1.2), -1,
                (textSize << 1) / 3,
                p -> new GuiComponentText(p, splash(), 1.0f, 1.0f, 0.0f, 1.0f));
        onClick((event, engine) -> {
            engine.sounds()
                    .playSound("Engine:sound/Click.ogg", "sound.GUI", 1.0f,
                            1.0f);
            splash.setText(splash());
        });
    }

    private static String splash() {
        String text;
        LocalDate date = LocalDate.now();
        if (date.getDayOfMonth() == 1 && date.getMonth() == Month.APRIL) {
            text = "COMIC\nSANS!!!";
        } else if (date.getDayOfMonth() == 29 &&
                date.getMonth() == Month.FEBRUARY) {
            text = "Best day\never!";
        } else if (date.getDayOfMonth() == 24 &&
                date.getMonth() == Month.DECEMBER) {
            text = "Merry\nChristmas!";
        } else if (date.getDayOfMonth() == 1 &&
                date.getMonth() == Month.JANUARY) {
            text = "Happy new\nyear!";
        } else if (date.getDayOfMonth() == 3 &&
                date.getMonth() == Month.MARCH) {
            char[] array = new char[1024];
            Arrays.fill(array, ' ');
            text = new String(array) + "Aspect ratio!?!";
        } else {
            Random random = ThreadLocalRandom.current();
            text = SPLASHES[random.nextInt(SPLASHES.length)];
        }
        return text;
    }
}
