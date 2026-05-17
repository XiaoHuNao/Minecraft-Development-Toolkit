package com.xiaohunao.mdt.protocol;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Brigadier 命令树的序列化节点。
 */
public class CommandTreeNode {

    private String name;
    private String type;       // "root" | "literal" | "argument"
    private String tooltip;    // 参数说明或悬停提示
    private List<CommandTreeNode> children = new ArrayList<>();

    public CommandTreeNode() {}

    public CommandTreeNode(String name, String type, String tooltip) {
        this.name = name;
        this.type = type;
        this.tooltip = tooltip;
    }

    // Getters / Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTooltip() { return tooltip; }
    public void setTooltip(String tooltip) { this.tooltip = tooltip; }

    public List<CommandTreeNode> getChildren() { return children; }
    public void setChildren(List<CommandTreeNode> children) { this.children = children; }

    public void addChild(CommandTreeNode child) {
        this.children.add(child);
    }

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("name", name != null ? name : "");
        obj.addProperty("type", type != null ? type : "literal");
        if (tooltip != null && !tooltip.isEmpty()) {
            obj.addProperty("tooltip", tooltip);
        }
        JsonArray arr = new JsonArray();
        for (CommandTreeNode child : children) {
            arr.add(child.toJson());
        }
        obj.add("children", arr);
        return obj;
    }

    public static CommandTreeNode fromJson(JsonObject obj) {
        CommandTreeNode node = new CommandTreeNode();
        node.name = obj.has("name") ? obj.get("name").getAsString() : "";
        node.type = obj.has("type") ? obj.get("type").getAsString() : "literal";
        node.tooltip = obj.has("tooltip") ? obj.get("tooltip").getAsString() : null;
        if (obj.has("children") && obj.get("children").isJsonArray()) {
            JsonArray arr = obj.getAsJsonArray("children");
            for (int i = 0; i < arr.size(); i++) {
                node.children.add(fromJson(arr.get(i).getAsJsonObject()));
            }
        }
        return node;
    }
}
