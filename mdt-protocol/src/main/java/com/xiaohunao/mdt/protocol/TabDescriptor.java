package com.xiaohunao.mdt.protocol;

import com.google.gson.JsonObject;

public class TabDescriptor {
    private String id;
    private String title;
    private String icon;
    private boolean closeable;
    private int order;

    public TabDescriptor(String id, String title) {
        this(id, title, "", true, 0);
    }

    public TabDescriptor(String id, String title, String icon, boolean closeable, int order) {
        this.id = id;
        this.title = title;
        this.icon = icon;
        this.closeable = closeable;
        this.order = order;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getIcon() { return icon; }
    public boolean isCloseable() { return closeable; }
    public int getOrder() { return order; }

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", id);
        obj.addProperty("title", title);
        obj.addProperty("icon", icon);
        obj.addProperty("closeable", closeable);
        obj.addProperty("order", order);
        return obj;
    }

    public static TabDescriptor fromJson(JsonObject obj) {
        return new TabDescriptor(
            obj.get("id").getAsString(),
            obj.get("title").getAsString(),
            obj.has("icon") ? obj.get("icon").getAsString() : "",
            obj.has("closeable") ? obj.get("closeable").getAsBoolean() : true,
            obj.has("order") ? obj.get("order").getAsInt() : 0
        );
    }

}
