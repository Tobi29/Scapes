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

package org.tobi29.scapes.tools.controlpanel

import kotlinx.coroutines.experimental.launch
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.*
import org.tobi29.scapes.engine.server.*
import org.tobi29.scapes.engine.swt.util.framework.Document
import org.tobi29.scapes.engine.swt.util.framework.DocumentComposite
import org.tobi29.scapes.engine.swt.util.framework.MultiDocumentApplication
import org.tobi29.scapes.engine.swt.util.widgets.InputDialog
import org.tobi29.scapes.engine.swt.util.widgets.SmartMenuBar
import org.tobi29.scapes.engine.utils.io.IOException
import org.tobi29.scapes.engine.utils.io.toChannel
import org.tobi29.scapes.engine.utils.io.view
import org.tobi29.scapes.engine.utils.logging.KLogging
import org.tobi29.scapes.engine.utils.newEventDispatcher
import org.tobi29.scapes.engine.utils.tag.Tag
import org.tobi29.scapes.engine.utils.tag.TagMap
import org.tobi29.scapes.engine.utils.tag.toList
import org.tobi29.scapes.engine.utils.toArray
import org.tobi29.scapes.tools.controlpanel.ui.Certificate
import java.nio.channels.SelectionKey

class ConnectDocument(private val address: RemoteAddress,
                      private val password: String,
                      private val connections: ConnectionManager,
                      private val application: MultiDocumentApplication) : Document {
    private var document: Document = this

    override val title = address.address + ':' + address.port
    override val shortTitle = address.address

    init {
        val fail = { e: Exception ->
            launch(application.uiContext) {
                val composite = application.compositeFor(
                        document) ?: return@launch
                if (application.message(composite,
                        SWT.ICON_ERROR or SWT.YES or SWT.NO,
                        "Connection lost",
                        "Lost connection:\n" + e.message +
                                "\n\nTry to reconnect?") == SWT.YES) {
                    application.replaceTab(composite,
                            ReconnectDocument(application, address, password,
                                    connections))
                } else {
                    application.closeTab(composite)
                }
            }
        }
        connections.addConnection { worker, connection ->
            val ssl = SSLHandle()
            try {
                connect(worker, connection, ssl)
            } catch (e: SSLCertificateException) {
                launch(application.uiContext) {
                    val composite = application.compositeFor(
                            document) ?: return@launch
                    val certificates = e.certificates
                    for (certificate in certificates) {
                        val dialog = InputDialog(composite.shell,
                                "Untrusted certificate", "Disconnect")
                        dialog.add("Certificate details", {
                            Certificate(it, SWT.BORDER, certificate)
                        })
                        val ignore = dialog.add("", { Button(it, SWT.NONE) })
                        ignore.text = "Ignore"
                        ignore.layoutData = GridData(SWT.CENTER, SWT.CENTER,
                                true, false, 1, 1)
                        var ignored = false
                        ignore.addListener(SWT.Selection) { event ->
                            ignored = true
                            dialog.dismiss()
                        }
                        dialog.open()
                        if (!ignored) {
                            application.closeTab(composite)
                            return@launch
                        }
                    }

                    val sslForce = SSLHandle.fromCertificates(certificates)

                    connections.addConnection { worker, connection ->
                        try {
                            connect(worker, connection, sslForce)
                        } catch (e: IOException) {
                            fail(e)
                        }
                    }
                }
            } catch (e: IOException) {
                fail(e)
            }
        }
    }

    override fun forceClose() {
    }

    override fun destroy() {
    }

    override fun populate(composite: DocumentComposite,
                          menu: SmartMenuBar,
                          application: MultiDocumentApplication) {
        val connection = menu.menu("Connection")
        val connectionClose = MenuItem(connection, SWT.PUSH)
        connectionClose.text = "Close"
        connectionClose.addListener(SWT.Selection) {
            application.closeTab(composite)
        }
        composite.layout = GridLayout(1, false)
        val pane = Composite(composite, SWT.NONE)
        pane.layout = GridLayout(1, false)
        pane.layoutData = GridData(SWT.FILL, SWT.CENTER, true, true)
        val text = Label(pane, SWT.NONE)
        text.layoutData = GridData(SWT.CENTER, SWT.CENTER, true, false)
        val progress = ProgressBar(pane, SWT.INDETERMINATE)
        progress.layoutData = GridData(SWT.FILL, SWT.CENTER, true, false)
        text.text = "Connecting..."
    }

    private suspend fun connect(worker: ConnectionWorker,
                                connection: Connection,
                                ssl: SSLHandle) {
        val channel = connect(worker, address)
        try {
            channel.register(worker.selector, SelectionKey.OP_READ)
            val secureChannel = ssl.newSSLChannel(address,
                    channel.toChannel(), worker.connection.taskExecutor, true)
            val bundleChannel = PacketBundleChannel(secureChannel)
            val output = bundleChannel.outputStream
            output.put(ConnectionInfo.header().view)
            output.put(21)
            bundleChannel.queueBundle()
            val controlPanel = ControlPanelProtocol(worker, bundleChannel,
                    newEventDispatcher())
            controlPanel.addCommand("Commands-Send") { payload ->
                launch(application.uiContext) {
                    val composite = application.compositeFor(
                            document) ?: return@launch
                    payload["Commands"]?.toList()?.let {
                        val commands = it.asSequence().map(
                                Tag::toString).toArray()
                        document = ControlPanelDocument(application, address,
                                controlPanel, commands.toSet(),
                                { connection.requestClose() })
                        application.replaceTab(composite, document)
                    }
                }
            }
            controlPanel.send("Commands-List", TagMap())
            controlPanel.runClient(connection, "Control Panel",
                    ControlPanelProtocol.passwordAuthentication(
                            password))
            bundleChannel.flushAsync()
            secureChannel.requestClose()
            bundleChannel.finishAsync()
            secureChannel.finishAsync()
            launch(application.uiContext) {
                val composite = application.compositeFor(
                        document) ?: return@launch
                application.closeTab(composite)
            }
        } finally {
            try {
                channel.close()
            } catch (e: IOException) {
                logger.warn(e) { "Failed to close socket" }
            }
        }
    }

    companion object : KLogging()
}
