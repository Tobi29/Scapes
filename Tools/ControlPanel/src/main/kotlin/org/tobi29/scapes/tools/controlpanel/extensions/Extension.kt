package org.tobi29.scapes.tools.controlpanel.extensions

import org.tobi29.scapes.engine.server.ControlPanelProtocol
import org.tobi29.scapes.tools.controlpanel.ui.ControlPanelConnection

abstract class Extension(protected val composite: ControlPanelConnection,
                         protected val connection: ControlPanelProtocol)
