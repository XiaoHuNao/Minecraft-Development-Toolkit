package com.xiaohunao.mdt.protocol;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class ComponentNode {
    private String id;
    private String type;
    private ComponentProps props;
    private List<String> children;
    private List<Binding> bindings;
    private List<String> events;

    public ComponentNode(String id, String type) {
        this.id = id;
        this.type = type;
        this.props = new ComponentProps();
        this.children = new ArrayList<>();
        this.bindings = new ArrayList<>();
        this.events = new ArrayList<>();
    }

    public ComponentNode(String id, String type, ComponentProps props, List<String> children, List<Binding> bindings, List<String> events) {
        this.id = id;
        this.type = type;
        this.props = props != null ? props : new ComponentProps();
        this.children = children != null ? children : new ArrayList<>();
        this.bindings = bindings != null ? bindings : new ArrayList<>();
        this.events = events != null ? events : new ArrayList<>();
    }

    // Getters
    public String getId() { return id; }
    public String getType() { return type; }
    public ComponentProps getProps() { return props; }
    public List<String> getChildren() { return children; }
    public List<Binding> getBindings() { return bindings; }
    public List<String> getEvents() { return events; }

    // Setters (builder style)
    public ComponentNode setId(String id) { this.id = id; return this; }
    public ComponentNode setType(String type) { this.type = type; return this; }
    public ComponentNode setProps(ComponentProps props) { this.props = props; return this; }
    public ComponentNode addChild(String childId) { this.children.add(childId); return this; }
    public ComponentNode addBinding(Binding binding) { this.bindings.add(binding); return this; }
    public ComponentNode addEvent(String event) { this.events.add(event); return this; }
    public ComponentNode setChildren(List<String> children) { this.children = children; return this; }
    public ComponentNode setBindings(List<Binding> bindings) { this.bindings = bindings; return this; }
    public ComponentNode setEvents(List<String> events) { this.events = events; return this; }

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", id);
        obj.addProperty("type", type);
        obj.add("props", props.toJson());

        if (!children.isEmpty()) {
            JsonArray childrenArr = new JsonArray();
            children.forEach(childrenArr::add);
            obj.add("children", childrenArr);
        }

        if (!bindings.isEmpty()) {
            JsonArray bindingsArr = new JsonArray();
            bindings.forEach(b -> bindingsArr.add(b.toJson()));
            obj.add("bindings", bindingsArr);
        }

        if (!events.isEmpty()) {
            JsonArray eventsArr = new JsonArray();
            events.forEach(eventsArr::add);
            obj.add("events", eventsArr);
        }

        return obj;
    }

    public static ComponentNode fromJson(JsonObject obj) {
        ComponentNode node = new ComponentNode(
            obj.get("id").getAsString(),
            obj.get("type").getAsString()
        );

        if (obj.has("props") && obj.get("props").isJsonObject()) {
            node.setProps(ComponentProps.fromJson(obj.getAsJsonObject("props")));
        }

        if (obj.has("children")) {
            for (JsonElement el : obj.getAsJsonArray("children")) {
                node.addChild(el.getAsString());
            }
        }

        if (obj.has("bindings")) {
            for (JsonElement el : obj.getAsJsonArray("bindings")) {
                node.addBinding(Binding.fromJson(el.getAsJsonObject()));
            }
        }

        if (obj.has("events")) {
            for (JsonElement el : obj.getAsJsonArray("events")) {
                node.addEvent(el.getAsString());
            }
        }

        return node;
    }
}
