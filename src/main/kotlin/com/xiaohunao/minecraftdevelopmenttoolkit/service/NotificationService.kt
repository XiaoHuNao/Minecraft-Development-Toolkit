package com.xiaohunao.minecraftdevelopmenttoolkit.service

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.xiaohunao.mdt.protocol.Message
import com.xiaohunao.minecraftdevelopmenttoolkit.MyMessageBundle
import com.xiaohunao.minecraftdevelopmenttoolkit.ws.WebSocketClient

class NotificationService(
    private val wsClientProvider: () -> WebSocketClient?
) {

    fun handleNotification(message: Message) {
        val payload = message.payload
        val title = payload.get("title")?.asString ?: MyMessageBundle.message("notification.default.title")
        val content = payload.get("content")?.asString ?: ""
        val typeStr = payload.get("type")?.asString ?: "info"

        val notificationType = when (typeStr) {
            "warning", "warn" -> NotificationType.WARNING
            "error" -> NotificationType.ERROR
            else -> NotificationType.INFORMATION
        }

        ApplicationManager.getApplication().invokeLater {
            val notification = NotificationGroupManager.getInstance()
                .getNotificationGroup("MDT Notifications")
                .createNotification(title, content, notificationType)

            // Add action buttons if present
            if (payload.has("actions")) {
                val actionsArr = payload.getAsJsonArray("actions")
                for (actionEl in actionsArr) {
                    val actionObj = actionEl.asJsonObject
                    val actionId = actionObj.get("id").asString
                    val label = actionObj.get("label").asString

                    notification.addAction(
                        com.intellij.notification.NotificationAction.createSimple(label, Runnable {
                            // Send EVENT_FIRE back to server for this action
                            wsClientProvider()?.send(
                                com.xiaohunao.mdt.protocol.Message.createEventFire(
                                    message.tabId ?: "notification",
                                    actionId,
                                    "onNotificationAction",
                                    com.google.gson.JsonParser.parseString("\"$actionId\"")
                                )
                            )
                        })
                    )
                }
            }

            notification.notify(null)
        }
    }
}
