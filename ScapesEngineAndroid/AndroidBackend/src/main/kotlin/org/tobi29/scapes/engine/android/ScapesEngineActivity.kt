package org.tobi29.scapes.engine.android

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.provider.OpenableColumns
import mu.KLogging
import org.tobi29.scapes.engine.gui.GuiAction
import org.tobi29.scapes.engine.input.FileType
import org.tobi29.scapes.engine.utils.Sync
import org.tobi29.scapes.engine.utils.io.BufferedReadChannelStream
import org.tobi29.scapes.engine.utils.io.ReadableByteStream
import java.io.IOException
import java.nio.channels.Channels
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

abstract class ScapesEngineActivity : Activity() {
    private val handler = Handler()
    private val sync = Sync(60.0, 5000000000L, false, "Rendering")
    private var serviceIntent: Intent? = null
    private var fileConsumer: Function2<String, ReadableByteStream, Unit>? = null
    lateinit var view: ScapesEngineView
    private var service: ScapesEngineService? = null
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName,
                                        service: IBinder) {
            val engine = (service as ScapesEngineService.ScapesBinder).get()
            engine.activity(this@ScapesEngineActivity)
            this@ScapesEngineActivity.service = engine
        }

        override fun onServiceDisconnected(name: ComponentName) {
            service = null
        }
    }

    protected abstract fun service(): Class<out ScapesEngineService>

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        view = ScapesEngineView(this)
        setContentView(view)
        view.setRenderer(object : GLSurfaceView.Renderer {
            override fun onSurfaceCreated(gl: GL10,
                                          config: EGLConfig) {
                service?.let { service ->
                    service.engine?.graphics?.reset()
                    sync.init()
                }
            }

            override fun onSurfaceChanged(gl: GL10,
                                          width: Int,
                                          height: Int) {
                service?.let { service ->
                    service.setResolution(width, height)
                }
            }

            override fun onDrawFrame(gl: GL10) {
                service?.let { service ->
                    service.render(sync.delta())
                    sync.tick()
                }
            }
        })
        serviceIntent = Intent(this, service())
        startService(serviceIntent)
        bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
    }

    public override fun onResume() {
        super.onResume()
        view.onResume()
        bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
    }

    public override fun onPause() {
        super.onPause()
        view.onPause()
        unbindService(connection)
    }

    override fun onBackPressed() {
        val engine = this.service ?: return
        engine.engine?.guiStack?.fireAction(GuiAction.BACK)
    }

    override fun onActivityResult(requestCode: Int,
                                  resultCode: Int,
                                  data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            10 -> if (resultCode == Activity.RESULT_OK) {
                fileConsumer?.let { consumer ->
                    val file = data.data
                    if (file == null) {
                        val clipData = data.clipData
                        if (clipData != null) {
                            val count = clipData.itemCount
                            for (i in 0..count - 1) {
                                acceptFile(consumer,
                                        clipData.getItemAt(i).uri)
                            }
                        }
                    } else {
                        acceptFile(consumer, file)
                    }
                    fileConsumer = null
                }
            }
        }
    }

    private fun acceptFile(consumer: (String, ReadableByteStream) -> Unit,
                           file: Uri) {
        try {
            contentResolver.openInputStream(file)?.use { stream ->
                contentResolver.query(file, null, null, null,
                        null)?.use { cursor ->
                    cursor.moveToFirst()
                    val name = cursor.getString(
                            cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                    consumer.invoke(name,
                            BufferedReadChannelStream(
                                    Channels.newChannel(stream)))
                }
            }
        } catch (e: IOException) {
            logger.error(e) { "Failed to apply picked file" }
        }

    }

    fun openFileDialog(type: FileType,
                       multiple: Boolean,
                       result: (String, ReadableByteStream) -> Unit) {
        handler.post {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, multiple)
            intent.type = "*/*"
            if (type == FileType.IMAGE) {
                val types = arrayOf("image/png")
                intent.putExtra(Intent.EXTRA_MIME_TYPES, types)
            } else if (type == FileType.MUSIC) {
                val types = arrayOf("audio/mpeg", "audio/x-wav",
                        "application/ogg")
                intent.putExtra(Intent.EXTRA_MIME_TYPES, types)
            }
            fileConsumer = result
            startActivityForResult(intent, 10)
        }
    }

    companion object : KLogging()
}
