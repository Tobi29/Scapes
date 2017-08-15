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

package org.tobi29.scapes.client.states

import org.tobi29.scapes.client.ScapesClient
import org.tobi29.scapes.client.gui.GuiAccount
import org.tobi29.scapes.client.gui.GuiGenerateAccount
import org.tobi29.scapes.client.gui.GuiMainMenu
import org.tobi29.scapes.client.states.scenes.SceneMenu
import org.tobi29.scapes.connection.Account
import org.tobi29.scapes.engine.GameState
import org.tobi29.scapes.engine.ScapesEngine
import org.tobi29.scapes.engine.graphics.busyPipeline
import org.tobi29.scapes.engine.graphics.renderScene
import org.tobi29.scapes.engine.gui.Gui
import org.tobi29.scapes.engine.gui.GuiStyle
import org.tobi29.scapes.engine.resource.awaitDone
import org.tobi29.scapes.engine.utils.io.IOException
import org.tobi29.scapes.engine.utils.io.filesystem.FilePath
import org.tobi29.scapes.engine.utils.logging.KLogging

class GameStateMenu(engine: ScapesEngine) : GameState(engine) {
    private val scene = SceneMenu(engine)

    override fun dispose() {
        engine.sounds.stop("music")
    }

    override fun init() {
        val scapes = engine[ScapesClient.COMPONENT]
        val style = engine.guiStyle
        val file = scapes.home.resolve("Account.properties")
        val account = try {
            Account.read(file)
        } catch (e: IOException) {
            logger.error { "Failed to read account file: $e" }
            null
        }
        val menu = menu(account, file, style)
        menu.visible = false
        engine.guiStack.add("10-Menu", menu)
        switchPipeline { gl ->
            val busy = busyPipeline(gl)
            ;{
            val busyRender = busy()
            ;{ _ ->
            gl.clear(0.0f, 0.0f, 0.0f, 1.0f)
            busyRender()
        }
        }
        }
        switchPipelineWhenLoaded { gl ->
            val scene = renderScene(gl, scene)
            ;{
            val sceneRender = scene()
            engine.resources.awaitDone()
            menu.visible = true
            ;{ delta ->
            sceneRender(delta)
        }
        }
        }
    }

    override val isMouseGrabbed: Boolean
        get() = false

    override fun step(delta: Double) {
    }

    override fun renderStep(delta: Double) {
        scene.step(delta)
    }

    private fun menu(account: Account?,
                     path: FilePath,
                     style: GuiStyle): Gui {
        if (account == null) {
            return GuiGenerateAccount(this, path,
                    { newAccount -> menu(newAccount, style) }, style)
        }
        return menu(account, style)
    }

    private fun menu(account: Account,
                     style: GuiStyle): Gui {
        if (!account.valid()) {
            return GuiAccount(this, menu(style), account, style)
        }
        return menu(style)
    }

    private fun menu(style: GuiStyle): Gui {
        return GuiMainMenu(this, scene, style)
    }

    companion object : KLogging()
}
