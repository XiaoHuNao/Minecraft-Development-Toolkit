package com.xiaohunao.mdt.protocol;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.LinkedHashMap;
import java.util.Map;

public class ComponentProps {
    private final Map<String, Object> props = new LinkedHashMap<>();

    public ComponentProps() {}

    public ComponentProps set(String key, Object value) {
        props.put(key, value);
        return this;
    }

    /** Alias for set() — convenient for Map-style usage */
    public void put(String key, Object value) {
        props.put(key, value);
    }

    public String getString(String key) { return getString(key, null); }
    public String getString(String key, String defaultValue) {
        Object v = props.get(key);
        return v != null ? v.toString() : defaultValue;
    }

    public int getInt(String key) { return getInt(key, 0); }
    public int getInt(String key, int defaultValue) {
        Object v = props.get(key);
        return v instanceof Number ? ((Number) v).intValue() : defaultValue;
    }

    public boolean getBoolean(String key) { return getBoolean(key, false); }
    public boolean getBoolean(String key, boolean defaultValue) {
        Object v = props.get(key);
        return v instanceof Boolean ? (Boolean) v : defaultValue;
    }

    public double getDouble(String key) { return getDouble(key, 0.0); }
    public double getDouble(String key, double defaultValue) {
        Object v = props.get(key);
        return v instanceof Number ? ((Number) v).doubleValue() : defaultValue;
    }

    public boolean has(String key) { return props.containsKey(key); }
    public Map<String, Object> asMap() { return new LinkedHashMap<>(props); }

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        for (Map.Entry<String, Object> entry : props.entrySet()) {
            Object value = entry.getValue();
            if (value == null) {
                obj.add(entry.getKey(), null);
            } else if (value instanceof Number) {
                obj.addProperty(entry.getKey(), (Number) value);
            } else if (value instanceof Boolean) {
                obj.addProperty(entry.getKey(), (Boolean) value);
            } else {
                obj.addProperty(entry.getKey(), value.toString());
            }
        }
        return obj;
    }

    public static ComponentProps fromJson(JsonObject obj) {
        ComponentProps props = new ComponentProps();
        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            JsonElement el = entry.getValue();
            if (el.isJsonNull()) {
                props.set(entry.getKey(), null);
            } else if (el.isJsonPrimitive()) {
                JsonPrimitive prim = el.getAsJsonPrimitive();
                if (prim.isBoolean()) {
                    props.set(entry.getKey(), prim.getAsBoolean());
                } else if (prim.isNumber()) {
                    props.set(entry.getKey(), prim.getAsNumber());
                } else {
                    props.set(entry.getKey(), prim.getAsString());
                }
            } else {
                props.set(entry.getKey(), el.toString());
            }
        }
        return props;
    }
}
