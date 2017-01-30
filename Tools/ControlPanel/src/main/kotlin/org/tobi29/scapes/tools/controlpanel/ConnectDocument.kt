package org.tobi29.scapes.tools.controlpanel

import org.eclipse.swt.SWT
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Label
import org.eclipse.swt.widgets.MenuItem
import org.eclipse.swt.widgets.ProgressBar
import org.tobi29.scapes.engine.server.*
import org.tobi29.scapes.engine.swt.util.framework.Document
import org.tobi29.scapes.engine.swt.util.framework.DocumentComposite
import org.tobi29.scapes.engine.swt.util.framework.MultiDocumentApplication
import org.tobi29.scapes.engine.swt.util.widgets.SmartMenuBar
import org.tobi29.scapes.engine.utils.filterMap
import org.tobi29.scapes.engine.utils.io.tag.structure
import org.tobi29.scapes.engine.utils.toArray
import java.io.IOException
import java.nio.channels.SelectionKey

class ConnectDocument(private val address: RemoteAddress,
                      private val password: String,
                      private val ssl: SSLHandle,
                      private val connections: ConnectionManager,
                      private val application: MultiDocumentApplication) : Document {
    private var document: Document = this

    override val title = address.address + ':' + address.port
    override val shortTitle = address.address

    init {
        val fail = { e: Exception ->
            application.accessAsync(document) { composite ->
                application.message(composite, SWT.ICON_ERROR,
                        "Unable to connect",
                        "Failed to connect:\n" + e.message)
                application.closeTab(composite)
            }
        }
        connections.addConnection { worker, connection ->
            val channel = try {
                connect(worker, address)
            } catch (e: Exception) {
                fail(e)
                return@addConnection
            }
            try {
                channel.register(worker.joiner.selector,
                        SelectionKey.OP_READ)
                val bundleChannel = PacketBundleChannel(address, channel,
                        connections.taskExecutor, ssl, true)
                val output = bundleChannel.outputStream
                output.put(ConnectionInfo.header())
                output.put(21)
                bundleChannel.queueBundle()
                val controlPanel = ControlPanelProtocol(worker, bundleChannel,
                        null)
                try {
                    controlPanel.addCommand("Commands-Send") { payload ->
                        application.accessAsync(document) { composite ->
                            payload.getList("Commands")?.let {
                                val commands = it.asSequence().filterMap<String>().toArray()
                                document = ControlPanelDocument(address,
                                        controlPanel, commands,
                                        { connection.requestClose() })
                                application.replaceTab(composite, document)
                            }
                        }
                    }
                    controlPanel.send("Commands-List", structure {})
                    controlPanel.disconnectHook { e ->
                        application.accessAsync(document) { composite ->
                            if (application.message(composite,
                                    SWT.ICON_ERROR or SWT.YES or SWT.NO,
                                    "Connection lost",
                                    "Lost connection:\n" + e.message +
                                            "\n\nTry to reconnect?") == SWT.YES) {
                                application.replaceTab(composite,
                                        ReconnectDocument(address, password,
                                                ssl, connections))
                            } else {
                                application.closeTab(composite)
                            }
                        }
                    }
                    controlPanel.runClient(connection, "Control Panel",
                            ControlPanelProtocol.passwordAuthentication(
                                    password))
                } finally {
                    application.accessAsync(document) { composite ->
                        application.closeTab(composite)
                    }
                }
            } catch (e: Exception) {
                fail(e)
            } finally {
                try {
                    channel.close()
                } catch (e: IOException) {
                }
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
}
