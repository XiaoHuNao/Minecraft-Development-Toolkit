package com.xiaohunao.mdt.protocol;

import com.google.gson.JsonObject;

public class Binding {
    private String prop;
    private String path;
    private BindingDirection direction;

    public Binding() {
        this.direction = BindingDirection.ONE_WAY;
    }

    public Binding(String prop, String path) {
        this(prop, path, BindingDirection.ONE_WAY);
    }

    public Binding(String prop, String path, BindingDirection direction) {
        this.prop = prop;
        this.path = path;
        this.direction = direction;
    }

    // Getters and setters
    public String getProp() { return prop; }
    public void setProp(String prop) { this.prop = prop; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public BindingDirection getDirection() { return direction; }
    public void setDirection(BindingDirection direction) { this.direction = direction; }

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("prop", prop);
        obj.addProperty("path", path);
        obj.addProperty("direction", direction.name());
        return obj;
    }

    public static Binding fromJson(JsonObject obj) {
        return new Binding(
            obj.get("prop").getAsString(),
            obj.get("path").getAsString(),
            obj.has("direction") ? BindingDirection.valueOf(obj.get("direction").getAsString()) : BindingDirection.ONE_WAY
        );
    }
}
