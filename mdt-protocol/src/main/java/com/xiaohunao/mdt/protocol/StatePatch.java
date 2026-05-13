package com.xiaohunao.mdt.protocol;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class StatePatch {
    private int baseVersion;
    private List<PatchOperation> patches;

    public StatePatch(int baseVersion) {
        this.baseVersion = baseVersion;
        this.patches = new ArrayList<>();
    }

    public int getBaseVersion() { return baseVersion; }
    public List<PatchOperation> getPatches() { return patches; }

    public StatePatch add(String op, String path, JsonElement value) {
        patches.add(new PatchOperation(op, path, value));
        return this;
    }

    public StatePatch replace(String path, JsonElement value) { return add("replace", path, value); }
    public StatePatch addOp(String path, JsonElement value) { return add("add", path, value); }
    public StatePatch remove(String path) { return add("remove", path, null); }

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("baseVersion", baseVersion);
        JsonArray arr = new JsonArray();
        for (PatchOperation op : patches) {
            arr.add(op.toJson());
        }
        obj.add("patch", arr);
        return obj;
    }

    public static StatePatch fromJson(JsonObject obj) {
        int baseVersion = obj.has("baseVersion") ? obj.get("baseVersion").getAsInt() : 1;
        StatePatch patch = new StatePatch(baseVersion);
        if (obj.has("patch")) {
            for (JsonElement el : obj.getAsJsonArray("patch")) {
                patch.patches.add(PatchOperation.fromJson(el.getAsJsonObject()));
            }
        }
        return patch;
    }

    public static class PatchOperation {
        private final String op;
        private final String path;
        private final JsonElement value;

        public PatchOperation(String op, String path, JsonElement value) {
            this.op = op;
            this.path = path;
            this.value = value;
        }

        public String getOp() { return op; }
        public String getPath() { return path; }
        public JsonElement getValue() { return value; }

        public JsonObject toJson() {
            JsonObject obj = new JsonObject();
            obj.addProperty("op", op);
            obj.addProperty("path", path);
            if (value != null) {
                obj.add("value", value);
            }
            return obj;
        }

        public static PatchOperation fromJson(JsonObject obj) {
            if (!obj.has("op") || !obj.has("path")) {
                throw new IllegalArgumentException("PatchOperation missing required field 'op' or 'path'");
            }
            return new PatchOperation(
                obj.get("op").getAsString(),
                obj.get("path").getAsString(),
                obj.has("value") ? obj.get("value") : null
            );
        }
    }
}
