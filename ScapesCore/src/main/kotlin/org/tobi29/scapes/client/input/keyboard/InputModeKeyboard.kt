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

package org.tobi29.scapes.client.input.keyboard

import kotlinx.coroutines.experimental.CoroutineName
import kotlinx.coroutines.experimental.launch
import org.tobi29.scapes.Debug
import org.tobi29.scapes.client.ScapesClient
import org.tobi29.scapes.client.gui.GuiControlsDefault
import org.tobi29.scapes.client.input.InputModeScapes
import org.tobi29.scapes.client.states.GameStateGameMP
import org.tobi29.scapes.engine.GameState
import org.tobi29.scapes.engine.ScapesEngine
import org.tobi29.scapes.engine.gui.Gui
import org.tobi29.scapes.engine.gui.GuiController
import org.tobi29.scapes.engine.gui.GuiControllerMouse
import org.tobi29.scapes.engine.gui.PressEvent
import org.tobi29.scapes.engine.input.ControllerDefault
import org.tobi29.scapes.engine.input.ControllerKey
import org.tobi29.scapes.engine.input.ControllerKeyReference
import org.tobi29.scapes.engine.input.isDown
import org.tobi29.scapes.engine.utils.EventDispatcher
import org.tobi29.scapes.engine.utils.graphics.encodePNG
import org.tobi29.scapes.engine.utils.io.IOException
import org.tobi29.scapes.engine.utils.io.filesystem.write
import org.tobi29.scapes.engine.utils.logging.KLogging
import org.tobi29.scapes.engine.utils.math.vector.Vector2d
import org.tobi29.scapes.engine.utils.math.vector.times
import org.tobi29.scapes.engine.utils.tag.MutableTagMap
import org.tobi29.scapes.engine.utils.tag.mapMut
import org.tobi29.scapes.engine.utils.tag.toDouble
import org.tobi29.scapes.engine.utils.tag.toTag
import org.tobi29.scapes.entity.client.MobPlayerClientMain
import kotlin.collections.set

class InputModeKeyboard(engine: ScapesEngine,
                        val controller: ControllerDefault,
                        configMap: MutableTagMap) : InputModeScapes {
    val events: EventDispatcher
    private val tagMap: MutableTagMap
    private val guiController: GuiControllerMouse
    private val walkForward: ControllerKeyReference?
    private val walkBackward: ControllerKeyReference?
    private val walkLeft: ControllerKeyReference?
    private val walkRight: ControllerKeyReference?
    private val walkSprint: ControllerKeyReference?
    private val jump: ControllerKeyReference?
    private val left: ControllerKeyReference?
    private val right: ControllerKeyReference?

    override val requiresCameraSmoothing get() = false

    init {
        tagMap = configMap.mapMut("Default")
        defaultConfig(tagMap)

        val miscTag = tagMap.mapMut("Misc")
        val miscScrollTag = miscTag.mapMut("Scroll")
        val scrollSensitivity = miscScrollTag["Sensitivity"]?.toDouble() ?: 0.0

        guiController = GuiControllerMouse(engine, controller,
                scrollSensitivity)

        val movementTag = tagMap.mapMut("Movement")
        walkForward = ControllerKeyReference.valueOf(
                movementTag["Forward"].toString())
        walkBackward = ControllerKeyReference.valueOf(
                movementTag["Backward"].toString())
        walkLeft = ControllerKeyReference.valueOf(
                movementTag["Left"].toString())
        walkRight = ControllerKeyReference.valueOf(
                movementTag["Right"].toString())
        walkSprint = ControllerKeyReference.valueOf(
                movementTag["Sprint"].toString())
        jump = ControllerKeyReference.valueOf(
                movementTag["Jump"].toString())

        val cameraTag = tagMap.mapMut("Camera")
        val cameraSensitivity = cameraTag["Sensitivity"]?.toDouble() ?: 0.0

        val actionTag = tagMap.mapMut("Action")
        left = ControllerKeyReference.valueOf(actionTag["Left"].toString())
        right = ControllerKeyReference.valueOf(
                actionTag["Right"].toString())

        val menuTag = tagMap.mapMut("Menu")
        val inventory = ControllerKeyReference.valueOf(
                menuTag["Inventory"].toString())
        val menu = ControllerKeyReference.valueOf(menuTag["Menu"].toString())
        val chat = ControllerKeyReference.valueOf(menuTag["Chat"].toString())

        val hotbarTag = tagMap.mapMut("Hotbar")
        val hotbarAddRight = ControllerKeyReference.valueOf(
                hotbarTag["AddRight"].toString())
        val hotbarSubtractRight = ControllerKeyReference.valueOf(
                hotbarTag["SubtractRight"].toString())
        val hotbarAddLeft = ControllerKeyReference.valueOf(
                hotbarTag["AddLeft"].toString())
        val hotbarSubtractLeft = ControllerKeyReference.valueOf(
                hotbarTag["SubtractLeft"].toString())
        val hotbarAdd = ControllerKeyReference.valueOf(
                hotbarTag["Add"].toString())
        val hotbarSubtract = ControllerKeyReference.valueOf(
                hotbarTag["Subtract"].toString())
        val hotbarRight = Array(10) {
            ControllerKeyReference.valueOf(hotbarTag["${it}Right"].toString())
        }
        val hotbarLeft = Array(10) {
            ControllerKeyReference.valueOf(hotbarTag["${it}Left"].toString())
        }
        val hotbar = Array(10) {
            ControllerKeyReference.valueOf(hotbarTag["$it"].toString())
        }

        events = EventDispatcher(engine.events) {
            listen<ControllerDefault.MouseDeltaEvent>(
                    { it.controller == controller }) { event ->
                val dir = event.delta * cameraSensitivity
                events.fire(MobPlayerClientMain.InputDirectionEvent(dir))
            }
            val references = arrayOf(menu, inventory, chat, hotbarAddRight,
                    hotbarSubtractRight, hotbarAddLeft, hotbarSubtractLeft,
                    hotbarAdd, hotbarSubtract, *hotbarRight, *hotbarLeft,
                    *hotbar)
            listen<PressEvent>(
                    { it.controller == guiController }) { event ->
                if (event.muted) {
                    return@listen
                }
                val pressed = ControllerKeyReference.isPressed(event.key,
                        controller, *references)
                when (pressed) {
                    menu -> {
                        if (MobPlayerClientMain.MenuOpenEvent().apply {
                            events.fire(this)
                        }.success) {
                            event.muted = true
                            return@listen
                        }
                    }
                    inventory -> {
                        if (MobPlayerClientMain.MenuInventoryEvent().apply {
                            events.fire(this)
                        }.success) {
                            event.muted = true
                            return@listen
                        }
                    }
                    chat -> {
                        if (MobPlayerClientMain.MenuChatEvent().apply {
                            events.fire(this)
                        }.success) {
                            event.muted = true
                            return@listen
                        }
                    }
                    hotbarAddRight -> {
                        if (MobPlayerClientMain.HotbarChangeRightEvent(
                                1).apply {
                            events.fire(this)
                        }.success) {
                            event.muted = true
                            return@listen
                        }
                    }
                    hotbarSubtractRight -> {
                        if (MobPlayerClientMain.HotbarChangeRightEvent(
                                -1).apply {
                            events.fire(this)
                        }.success) {
                            event.muted = true
                            return@listen
                        }
                    }
                    hotbarAddLeft -> {
                        if (MobPlayerClientMain.HotbarChangeLeftEvent(1).apply {
                            events.fire(this)
                        }.success) {
                            event.muted = true
                            return@listen
                        }
                    }
                    hotbarSubtractLeft -> {
                        if (MobPlayerClientMain.HotbarChangeLeftEvent(
                                -1).apply {
                            events.fire(this)
                        }.success) {
                            event.muted = true
                            return@listen
                        }
                    }
                    hotbarAdd -> {
                        if (MobPlayerClientMain.HotbarChangeRightEvent(
                                1).apply {
                            events.fire(this)
                        }.success or MobPlayerClientMain.HotbarChangeLeftEvent(
                                1).apply {
                            events.fire(this)
                        }.success) {
                            event.muted = true
                            return@listen
                        }
                    }
                    hotbarSubtract -> {
                        if (MobPlayerClientMain.HotbarChangeRightEvent(
                                -1).apply {
                            events.fire(this)
                        }.success or MobPlayerClientMain.HotbarChangeLeftEvent(
                                -1).apply {
                            events.fire(this)
                        }.success) {
                            event.muted = true
                            return@listen
                        }
                    }
                    else -> {
                        for (i in 0..9) {
                            when (pressed) {
                                hotbarRight[i] -> {
                                    if (MobPlayerClientMain.HotbarSetRightEvent(
                                            i).apply {
                                        events.fire(this)
                                    }.success) {
                                        event.muted = true
                                        return@listen
                                    }
                                }
                                hotbarLeft[i] -> {
                                    if (MobPlayerClientMain.HotbarSetLeftEvent(
                                            i).apply {
                                        events.fire(this)
                                    }.success) {
                                        event.muted = true
                                        return@listen
                                    }
                                }
                                hotbar[i] -> {
                                    if (MobPlayerClientMain.HotbarSetRightEvent(
                                            i).apply {
                                        events.fire(this)
                                    }.success or MobPlayerClientMain.HotbarSetLeftEvent(
                                            i).apply {
                                        events.fire(this)
                                    }.success) {
                                        event.muted = true
                                        return@listen
                                    }
                                }
                            }
                        }
                    }
                }
                if (event.key == ControllerKey.KEY_F1) {
                    val state = engine.state
                    if (state is GameStateGameMP) {
                        state.setHudVisible(!state.hud.visible)
                        event.muted = true
                        return@listen
                    }
                }
                if (event.key == ControllerKey.KEY_F2) {
                    engine.graphics.requestScreenshot { image ->
                        launch(engine.taskExecutor + CoroutineName(
                                "Write-Screenshot")) {
                            val scapes = engine[ScapesClient.COMPONENT]
                            val path = scapes.home.resolve(
                                    "screenshots").resolve(
                                    "${System.currentTimeMillis()}.png")
                            try {
                                write(path) { encodePNG(image, it, 9, false) }
                            } catch (e: IOException) {
                                logger.error { "Error saving screenshot: $e" }
                            }
                        }
                    }
                    event.muted = true
                    return@listen
                }
                if (event.key == ControllerKey.KEY_F3) {
                    val shift = controller.isDown(ControllerKey.KEY_SHIFT_LEFT)
                    val control = controller.isDown(
                            ControllerKey.KEY_CONTROL_LEFT)
                    if (shift && control) {
                        throw DebugCrashException()
                    } else if (Debug.enabled()) {
                        if (shift) {
                            engine.profiler.visible = !engine.profiler.visible
                        } else if (control) {
                            engine.performance.visible = !engine.performance.visible
                        } else {
                            engine.debugValues.visible = !engine.debugValues.visible
                        }
                        event.muted = true
                        return@listen
                    }
                }
                if (Debug.enabled() && event.key == ControllerKey.KEY_F6) {
                    val state = engine.state
                    if (state is GameStateGameMP) {
                        state.debugWidget.visible = !state.debugWidget.visible
                        event.muted = true
                        return@listen
                    }
                }
            }
        }
    }

    private fun defaultConfig(tagMap: MutableTagMap) {
        val movementTag = tagMap.mapMut("Movement")
        check("Forward", ControllerKey.KEY_W, movementTag)
        check("Backward", ControllerKey.KEY_S, movementTag)
        check("Left", ControllerKey.KEY_A, movementTag)
        check("Right", ControllerKey.KEY_D, movementTag)
        check("Sprint", ControllerKey.KEY_SHIFT_LEFT, movementTag)
        check("Jump", ControllerKey.KEY_SPACE, movementTag)

        val cameraTag = tagMap.mapMut("Camera")
        check("Sensitivity", 0.6, cameraTag)

        val actionTag = tagMap.mapMut("Action")
        check("Left", ControllerKey.BUTTON_0, actionTag)
        check("Right", ControllerKey.BUTTON_1, actionTag)

        val menuTag = tagMap.mapMut("Menu")
        check("Inventory", ControllerKey.KEY_E, menuTag)
        check("Chat", ControllerKey.KEY_R, menuTag)
        check("Menu", ControllerKey.KEY_ESCAPE, menuTag)

        val hotbarTag = tagMap.mapMut("Hotbar")
        check("AddRight", ControllerKey.SCROLL_DOWN, hotbarTag)
        check("SubtractRight", ControllerKey.SCROLL_UP, hotbarTag)
        check("AddLeft",
                ControllerKeyReference(ControllerKey.SCROLL_DOWN,
                        ControllerKey.KEY_CONTROL_LEFT), hotbarTag)
        check("SubtractLeft",
                ControllerKeyReference(ControllerKey.SCROLL_UP,
                        ControllerKey.KEY_CONTROL_LEFT), hotbarTag)
        check("Add",
                ControllerKeyReference(ControllerKey.SCROLL_DOWN,
                        ControllerKey.KEY_ALT_LEFT), hotbarTag)
        check("Subtract",
                ControllerKeyReference(ControllerKey.SCROLL_UP,
                        ControllerKey.KEY_ALT_LEFT), hotbarTag)
        for (i in 0..9) {
            val key = numberKey((i + 1) % 10)
            check("${i}Right", key, hotbarTag)
            check("${i}Left",
                    ControllerKeyReference(key, ControllerKey.KEY_CONTROL_LEFT),
                    hotbarTag)
            check("$i", ControllerKeyReference(key, ControllerKey.KEY_ALT_LEFT),
                    hotbarTag)
        }

        val miscTag = tagMap.mapMut("Misc")

        val miscScrollTag = miscTag.mapMut("Scroll")
        check("Sensitivity", 1.0, miscScrollTag)
    }

    private fun check(id: String,
                      def: ControllerKey,
                      tagMap: MutableTagMap) {
        if (!tagMap.containsKey(id)) {
            tagMap[id] = def.toString().toTag()
        }
    }

    private fun check(id: String,
                      def: ControllerKeyReference,
                      tagMap: MutableTagMap) {
        if (!tagMap.containsKey(id)) {
            tagMap[id] = def.toString().toTag()
        }
    }

    private fun check(id: String,
                      def: Double,
                      tagMap: MutableTagMap) {
        if (!tagMap.containsKey(id)) {
            tagMap[id] = def.toTag()
        }
    }

    override fun enabled() {
        events.enable()
    }

    override fun disabled() {
        events.disable()
    }

    override fun poll(delta: Double): Boolean {
        controller.poll(events)
        return controller.isActive
    }

    override fun createControlsGUI(state: GameState,
                                   prev: Gui): Gui {
        return GuiControlsDefault(state, prev,
                state.engine[ScapesClient.COMPONENT], tagMap, controller,
                prev.style)
    }

    override fun walk(): Vector2d {
        var x = 0.0
        var y = 0.0
        if (walkForward.isDown(controller)) {
            y += 1.0
        }
        if (walkBackward.isDown(controller)) {
            y -= 1.0
        }
        if (walkLeft.isDown(controller)) {
            x -= 1.0
        }
        if (walkRight.isDown(controller)) {
            x += 1.0
        }
        if (!walkSprint.isDown(controller)) {
            x *= 0.4
            y *= 0.4
        }
        return Vector2d(x, y)
    }

    override fun hitDirection(): Vector2d {
        return Vector2d.ZERO
    }

    override fun left(): Boolean {
        return left.isDown(controller)
    }

    override fun right(): Boolean {
        return right.isDown(controller)
    }

    override fun jump(): Boolean {
        return jump.isDown(controller)
    }

    override fun guiController(): GuiController {
        return guiController
    }

    override fun toString(): String {
        return "Keyboard + Mouse"
    }

    companion object : KLogging() {
        private fun numberKey(i: Int) = ControllerKey.valueOf(
                "KEY_$i") ?: throw IllegalArgumentException(
                "Invalid number key: $i")
    }

    private class DebugCrashException : Exception("Debug crash report")
}
