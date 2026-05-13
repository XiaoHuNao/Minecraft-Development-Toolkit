package com.xiaohunao.mdt.protocol;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.UUID;

public class Message {
    private static final Gson GSON = new GsonBuilder().create();

    private MessageType type;
    private String tabId;
    private String requestId;
    private JsonObject payload;
    private long timestamp;

    public Message(MessageType type) {
        this.type = type;
        this.timestamp = System.currentTimeMillis();
        this.payload = new JsonObject();
    }

    // Getters
    public MessageType getType() { return type; }
    public String getTabId() { return tabId; }
    public String getRequestId() { return requestId; }
    public JsonObject getPayload() { return payload; }
    public long getTimestamp() { return timestamp; }

    // Setters (builder style)
    public Message tabId(String tabId) { this.tabId = tabId; return this; }
    public Message requestId(String requestId) { this.requestId = requestId; return this; }
    public Message payload(JsonObject payload) { this.payload = payload; return this; }
    public Message timestamp(long timestamp) { this.timestamp = timestamp; return this; }

    // Factory methods
    public static Message createReady(String protocolVersion, String[] supportedComponents) {
        return createReady(protocolVersion, supportedComponents, "");
    }

    public static Message createReady(String protocolVersion, String[] supportedComponents, String authToken) {
        Message msg = new Message(MessageType.READY);
        msg.payload.addProperty("protocolVersion", protocolVersion);
        JsonObject payload = msg.payload;
        com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
        for (String comp : supportedComponents) arr.add(comp);
        payload.add("supportedComponents", arr);
        if (authToken != null && !authToken.isEmpty()) {
            payload.addProperty("authToken", authToken);
        }
        return msg;
    }

    public static Message createHello(String acceptedProtocol, java.util.List<TabDescriptor> tabs) {
        return createHello(acceptedProtocol, tabs, "");
    }

    public static Message createHello(String acceptedProtocol, java.util.List<TabDescriptor> tabs, String serverName) {
        Message msg = new Message(MessageType.HELLO);
        msg.payload.addProperty("acceptedProtocol", acceptedProtocol);
        if (serverName != null && !serverName.isEmpty()) {
            msg.payload.addProperty("serverName", serverName);
        }
        com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
        if (tabs != null) {
            for (TabDescriptor tab : tabs) arr.add(tab.toJson());
        }
        msg.payload.add("tabs", arr);
        return msg;
    }

    public static Message createUiRender(String tabId, java.util.List<ComponentNode> components, JsonObject state, int baseVersion) {
        Message msg = new Message(MessageType.UI_RENDER);
        msg.tabId = tabId;
        com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
        if (components != null) {
            for (ComponentNode node : components) arr.add(node.toJson());
        }
        msg.payload.add("components", arr);
        msg.payload.add("state", state != null ? state : new JsonObject());
        msg.payload.addProperty("baseVersion", baseVersion);
        return msg;
    }

    public static Message createStateSnapshot(String tabId, JsonObject state, int baseVersion) {
        Message msg = new Message(MessageType.STATE_SNAPSHOT);
        msg.tabId = tabId;
        msg.payload.add("state", state != null ? state : new JsonObject());
        msg.payload.addProperty("baseVersion", baseVersion);
        return msg;
    }

    public static Message createStateSubscribe(String tabId, String[] statePaths) {
        Message msg = new Message(MessageType.STATE_SUBSCRIBE);
        msg.tabId = tabId;
        com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
        for (String path : statePaths) arr.add(path);
        msg.payload.add("paths", arr);
        return msg;
    }

    public static Message createStateDelta(String tabId, StatePatch patch) {
        Message msg = new Message(MessageType.STATE_DELTA);
        msg.tabId = tabId;
        msg.payload = patch.toJson();
        return msg;
    }

    public static Message createTabOpen(TabDescriptor tab) {
        Message msg = new Message(MessageType.TAB_OPEN);
        msg.tabId = tab.getId();
        msg.payload = tab.toJson();
        return msg;
    }

    public static Message createTabClose(String tabId) {
        Message msg = new Message(MessageType.TAB_CLOSE);
        msg.tabId = tabId;
        return msg;
    }

    public static Message createTabUpdate(TabDescriptor tab) {
        Message msg = new Message(MessageType.TAB_UPDATE);
        msg.tabId = tab.getId();
        msg.payload = tab.toJson();
        return msg;
    }

    public static Message createEventFire(String tabId, String componentId, String eventType, JsonElement value) {
        Message msg = new Message(MessageType.EVENT_FIRE);
        msg.tabId = tabId;
        msg.requestId = UUID.randomUUID().toString();
        msg.payload.addProperty("componentId", componentId);
        msg.payload.addProperty("eventType", eventType);
        if (value != null) msg.payload.add("value", value);
        return msg;
    }

    public static Message createEventAck(String requestId, boolean success, String error) {
        Message msg = new Message(MessageType.EVENT_ACK);
        msg.requestId = requestId;
        msg.payload.addProperty("success", success);
        if (error != null) msg.payload.addProperty("error", error);
        return msg;
    }

    public static Message createNotification(String title, String content, String type) {
        Message msg = new Message(MessageType.NOTIFICATION);
        msg.payload.addProperty("title", title);
        msg.payload.addProperty("content", content);
        msg.payload.addProperty("type", type);
        return msg;
    }

    public static Message createConsoleAppend(String tabId, java.util.List<JsonObject> lines) {
        Message msg = new Message(MessageType.CONSOLE_APPEND);
        msg.tabId = tabId;
        com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
        if (lines != null) {
            for (JsonObject line : lines) arr.add(line);
        }
        msg.payload.add("lines", arr);
        return msg;
    }

    public static Message createPing() {
        Message msg = new Message(MessageType.PING);
        msg.payload.addProperty("timestamp", System.currentTimeMillis());
        return msg;
    }

    public static Message createPong(long pingTimestamp) {
        Message msg = new Message(MessageType.PONG);
        msg.payload.addProperty("timestamp", pingTimestamp);
        return msg;
    }

    public static Message createError(String code, String message) {
        Message msg = new Message(MessageType.ERROR);
        msg.payload.addProperty("code", code);
        msg.payload.addProperty("message", message);
        return msg;
    }

    // Serialization
    public String toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("type", type.name());
        if (tabId != null) obj.addProperty("tabId", tabId);
        if (requestId != null) obj.addProperty("requestId", requestId);
        obj.add("payload", payload != null ? payload : new JsonObject());
        obj.addProperty("timestamp", timestamp);
        return GSON.toJson(obj);
    }

    public static Message fromJson(String json) {
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        String typeStr = obj.has("type") ? obj.get("type").getAsString() : null;
        MessageType type;
        try {
            type = MessageType.valueOf(typeStr);
        } catch (IllegalArgumentException | NullPointerException e) {
            return Message.createError("UNKNOWN_TYPE", "Unknown message type: " + typeStr);
        }
        Message msg = new Message(type);
        if (obj.has("tabId") && !obj.get("tabId").isJsonNull()) msg.tabId = obj.get("tabId").getAsString();
        if (obj.has("requestId") && !obj.get("requestId").isJsonNull()) msg.requestId = obj.get("requestId").getAsString();
        if (obj.has("payload") && obj.get("payload").isJsonObject()) {
            msg.payload = obj.getAsJsonObject("payload");
        }
        if (obj.has("timestamp")) msg.timestamp = obj.get("timestamp").getAsLong();
        return msg;
    }
}
