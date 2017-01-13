package org.tobi29.scapes.android

import org.tobi29.scapes.client.ScapesClient
import org.tobi29.scapes.engine.Game
import org.tobi29.scapes.engine.ScapesEngine
import org.tobi29.scapes.engine.android.ScapesEngineService
import org.tobi29.scapes.engine.utils.io.filesystem.FilePath

class ScapesService : ScapesEngineService() {
    override fun onCreateEngine(home: FilePath): (ScapesEngine) -> Game {
        return {
            ScapesClient(it) { AndroidSaveStorage(this, home.resolve("saves")) }
        }
    }
}