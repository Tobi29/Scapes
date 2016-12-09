package org.tobi29.scapes.android

import android.content.Context
import org.tobi29.scapes.client.SaveStorage
import org.tobi29.scapes.engine.utils.io.filesystem.FilePath
import org.tobi29.scapes.engine.utils.io.filesystem.deleteDir
import org.tobi29.scapes.engine.utils.io.filesystem.isDirectory
import org.tobi29.scapes.engine.utils.io.filesystem.isNotHidden
import org.tobi29.scapes.server.format.WorldSource


class AndroidSaveStorage(private val context: Context, private val path: FilePath) : SaveStorage {
    override fun list(): Sequence<String> {
        if (!org.tobi29.scapes.engine.utils.io.filesystem.exists(path)) {
            return emptySequence()
        }
        return org.tobi29.scapes.engine.utils.io.filesystem.list(path, {
            isDirectory(path) && isNotHidden(path)
        }).asSequence().map { it.fileName.toString() }
    }

    override fun exists(name: String): Boolean {
        return org.tobi29.scapes.engine.utils.io.filesystem.exists(
                path.resolve(name))
    }

    override fun get(name: String): WorldSource {
        return AndroidWorldSource(context, path.resolve(name))
    }

    override fun delete(name: String): Boolean {
        deleteDir(path.resolve(name))
        return true
    }
}