package org.tobi29.scapes.android

import org.tobi29.scapes.engine.android.ScapesEngineActivity
import org.tobi29.scapes.engine.android.ScapesEngineService

class ScapesActivity : ScapesEngineActivity() {
    override fun service(): Class<out ScapesEngineService> {
        return ScapesService::class.java
    }
}
