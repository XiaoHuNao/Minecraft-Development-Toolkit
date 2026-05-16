package com.xiaohunao.minecraftdevelopmenttoolkit.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.CollectionListModel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import com.xiaohunao.minecraftdevelopmenttoolkit.settings.MDTSettings
import com.xiaohunao.minecraftdevelopmenttoolkit.MyMessageBundle
import javax.swing.JComponent

class ConnectDialog(private val project: Project?) : DialogWrapper(project) {

    private lateinit var hostField: JBTextField
    private lateinit var portField: JBTextField
    private lateinit var tokenField: JBTextField

    val serverHost: String get() = hostField.text
    val serverPort: Int get() = portField.text.toIntOrNull() ?: 8765
    val authToken: String get() = tokenField.text

    private val recentModel = CollectionListModel(MDTSettings.getInstance().state.recentConnections)

    init {
        title = MyMessageBundle.message("connect.dialog.title")
        setOKButtonText(MyMessageBundle.message("connect.dialog.ok"))
        init()
    }

    override fun createCenterPanel(): JComponent {
        val recentList = JBList(recentModel).apply {
            selectionMode = javax.swing.ListSelectionModel.SINGLE_SELECTION
            addListSelectionListener { e ->
                if (!e.valueIsAdjusting) {
                    val conn = selectedValue ?: return@addListSelectionListener
                    hostField.text = conn.host
                    portField.text = conn.port.toString()
                }
            }
        }

        return panel {
            row(MyMessageBundle.message("connect.dialog.server.address")) {
                hostField = textField()
                    .align(AlignX.FILL)
                    .focused()
                    .component
            }
            row(MyMessageBundle.message("connect.dialog.port")) {
                portField = intTextField()
                    .apply { component.columns = 8 }
                    .component
            }
            row(MyMessageBundle.message("connect.dialog.auth.token")) {
                tokenField = textField()
                    .align(AlignX.FILL)
                    .component
            }
            group(MyMessageBundle.message("connect.dialog.recent.servers")) {
                row {
                    cell(recentList)
                        .align(AlignX.FILL)
                }
            }
        }
    }

    override fun doOKAction() {
        val serverHost = hostField.text
        val serverPort = portField.text.toIntOrNull() ?: 8765
        val authToken = tokenField.text

        MDTSettings.getInstance().apply {
            state.serverHost = serverHost
            state.serverPort = serverPort
        }
        MDTSettings.getInstance().addRecentConnection(serverHost, serverPort, authToken)
        super.doOKAction()
    }
}
