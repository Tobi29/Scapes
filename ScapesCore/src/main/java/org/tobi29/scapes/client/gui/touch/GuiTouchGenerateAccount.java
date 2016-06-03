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
package org.tobi29.scapes.client.gui.touch;

import java8.util.function.Function;
import org.tobi29.scapes.engine.GameState;
import org.tobi29.scapes.engine.gui.Gui;
import org.tobi29.scapes.engine.gui.GuiStyle;
import org.tobi29.scapes.engine.server.Account;
import org.tobi29.scapes.engine.utils.io.filesystem.FilePath;

public class GuiTouchGenerateAccount extends GuiTouchBusy {
    public GuiTouchGenerateAccount(GameState state, FilePath path,
            Function<Account, Gui> next, GuiStyle style) {
        super(state, style);
        setLabel("Creating account...");
        state.engine().taskExecutor().runTask(joiner -> {
            Account account = Account.generate(path);
            state.engine().guiStack().add("10-Menu", next.apply(account));
        }, "Generate-Account");
    }
}
