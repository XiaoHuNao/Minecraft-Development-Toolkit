package com.xiaohunao.minecraftdevelopmenttoolkit.ws

import com.intellij.openapi.diagnostic.Logger
import com.xiaohunao.mdt.protocol.Message
import com.xiaohunao.mdt.protocol.MessageType

class MessageDispatcher {

    private val logger = Logger.getInstance(MessageDispatcher::class.java)

    private val helloHandlers = mutableListOf<(Message) -> Unit>()
    private val uiRenderHandlers = mutableListOf<(String, Message) -> Unit>()
    private val stateHandlers = mutableListOf<(String, Message) -> Unit>()
    private val stateSubscribeHandlers = mutableListOf<(String, Message) -> Unit>()
    private val tabHandlers = mutableListOf<(Message) -> Unit>()
    private val notificationHandlers = mutableListOf<(Message) -> Unit>()
    private val consoleHandlers = mutableListOf<(String, Message) -> Unit>()
    private val commandSuggestHandlers = mutableListOf<(Message) -> Unit>()

    fun onHello(handler: (Message) -> Unit) { helloHandlers.add(handler) }
    fun onUiRender(handler: (tabId: String, message: Message) -> Unit) { uiRenderHandlers.add(handler) }
    fun onStateUpdate(handler: (tabId: String, message: Message) -> Unit) { stateHandlers.add(handler) }
    fun onStateSubscribe(handler: (tabId: String, message: Message) -> Unit) { stateSubscribeHandlers.add(handler) }
    fun onTabChange(handler: (message: Message) -> Unit) { tabHandlers.add(handler) }
    fun onNotification(handler: (message: Message) -> Unit) { notificationHandlers.add(handler) }
    fun onConsoleAppend(handler: (tabId: String, message: Message) -> Unit) { consoleHandlers.add(handler) }
    fun onCommandSuggestResponse(handler: (Message) -> Unit) { commandSuggestHandlers.add(handler) }

    fun dispatch(message: Message) {
        val tabId = message.tabId

        when (message.type) {
            MessageType.HELLO -> {
                helloHandlers.forEach { it(message) }
            }
            MessageType.UI_RENDER -> {
                if (tabId != null) uiRenderHandlers.forEach { it(tabId, message) }
            }
            MessageType.STATE_SNAPSHOT, MessageType.STATE_DELTA -> {
                if (tabId != null) stateHandlers.forEach { it(tabId, message) }
            }
            MessageType.TAB_OPEN, MessageType.TAB_CLOSE, MessageType.TAB_UPDATE -> {
                tabHandlers.forEach { it(message) }
            }
            MessageType.NOTIFICATION -> {
                notificationHandlers.forEach { it(message) }
            }
            MessageType.CONSOLE_APPEND -> {
                if (tabId != null) consoleHandlers.forEach { it(tabId, message) }
            }
            MessageType.EVENT_ACK -> {
                logger.info("Event ACK received: ${message.requestId}")
            }
            MessageType.STATE_SUBSCRIBE -> {
                // CLIENT_TO_SERVER — plugin normally only sends these, never receives them.
                // Handle gracefully in case one arrives unexpectedly.
                if (tabId != null) stateSubscribeHandlers.forEach { it(tabId, message) }
            }
            MessageType.COMMAND_SUGGEST_RESPONSE -> {
                commandSuggestHandlers.forEach { it(message) }
            }
            MessageType.ERROR -> {
                logger.error("Server error: ${message.payload}")
            }
            else -> {
                logger.warn("Unhandled message type: ${message.type}")
            }
        }
    }
}
