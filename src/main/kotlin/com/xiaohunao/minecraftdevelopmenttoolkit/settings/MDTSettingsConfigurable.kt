package com.xiaohunao.minecraftdevelopmenttoolkit.settings

import com.intellij.openapi.options.Configurable
import com.intellij.ui.CollectionListModel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import com.xiaohunao.minecraftdevelopmenttoolkit.MyMessageBundle
import javax.swing.DefaultListCellRenderer
import javax.swing.JComponent
import javax.swing.ListSelectionModel

class MDTSettingsConfigurable : Configurable {

    private val settings = MDTSettings.getInstance()

    private lateinit var hostField: JBTextField
    private lateinit var portField: JBTextField
    private lateinit var tokenField: JBTextField
    private lateinit var reconnectCheck: JBCheckBox
    private lateinit var consoleBufferField: JBTextField

    private lateinit var recentModel: CollectionListModel<MDTSettings.RecentConnection>
    private lateinit var recentList: JBList<MDTSettings.RecentConnection>

    override fun getDisplayName() = MyMessageBundle.message("settings.display.name")

    override fun createComponent(): JComponent {
        recentModel = CollectionListModel(settings.state.recentConnections.map { it.copy() }.toMutableList())
        recentList = JBList(recentModel).apply {
            selectionMode = ListSelectionModel.SINGLE_SELECTION
            cellRenderer = DefaultListCellRenderer().apply {
                // Will show host:port
            }
        }

        return panel {
            group(MyMessageBundle.message("settings.group.connection")) {
                row(MyMessageBundle.message("settings.default.server.address")) {
                    hostField = textField()
                        .align(AlignX.FILL)
                        .component
                }
                row(MyMessageBundle.message("settings.default.port")) {
                    portField = intTextField()
                        .apply { component.columns = 8 }
                        .component
                }
                row(MyMessageBundle.message("settings.auth.token")) {
                    tokenField = textField()
                        .align(AlignX.FILL)
                        .component
                }
                row {
                    reconnectCheck = checkBox(MyMessageBundle.message("settings.reconnect.on.disconnect"))
                        .component
                }
            }

            group(MyMessageBundle.message("settings.group.recent.servers")) {
                row {
                    cell(recentList)
                        .align(AlignX.FILL)
                }
                row {
                    button(MyMessageBundle.message("settings.remove.selected")) {
                        val idx = recentList.selectedIndex
                        if (idx >= 0) {
                            recentModel.remove(idx)
                        }
                    }
                    button(MyMessageBundle.message("settings.clear.all")) {
                        recentModel.removeAll()
                    }
                }
            }

            group(MyMessageBundle.message("settings.group.advanced")) {
                row(MyMessageBundle.message("settings.console.buffer.size")) {
                    consoleBufferField = intTextField()
                        .apply { component.columns = 8 }
                        .component
                }
            }
        }
    }

    override fun isModified(): Boolean {
        return hostField.text != settings.state.serverHost ||
                portField.text.toIntOrNull() != settings.state.serverPort ||
                tokenField.text != settings.state.authToken ||
                reconnectCheck.isSelected != settings.state.reconnectOnDisconnect ||
                consoleBufferField.text.toIntOrNull() != settings.state.consoleBufferSize ||
                recentModel.items != settings.state.recentConnections
    }

    override fun apply() {
        settings.state.serverHost = hostField.text
        settings.state.serverPort = portField.text.toIntOrNull() ?: 8765
        settings.state.authToken = tokenField.text
        settings.state.reconnectOnDisconnect = reconnectCheck.isSelected
        settings.state.consoleBufferSize = consoleBufferField.text.toIntOrNull() ?: 5000
        settings.state.recentConnections = recentModel.items.map { it.copy() }.toMutableList()
    }

    override fun reset() {
        hostField.text = settings.state.serverHost
        portField.text = settings.state.serverPort.toString()
        tokenField.text = settings.state.authToken
        reconnectCheck.isSelected = settings.state.reconnectOnDisconnect
        consoleBufferField.text = settings.state.consoleBufferSize.toString()
        recentModel.removeAll()
        recentModel.add(settings.state.recentConnections.map { it.copy() })
    }
}
