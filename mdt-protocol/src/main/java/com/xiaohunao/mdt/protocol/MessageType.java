package com.xiaohunao.mdt.protocol;

public enum MessageType {
    READY(Direction.CLIENT_TO_SERVER),
    HELLO(Direction.SERVER_TO_CLIENT),
    UI_RENDER(Direction.SERVER_TO_CLIENT),
    STATE_SNAPSHOT(Direction.SERVER_TO_CLIENT),
    STATE_DELTA(Direction.SERVER_TO_CLIENT),
    TAB_OPEN(Direction.SERVER_TO_CLIENT),
    TAB_CLOSE(Direction.SERVER_TO_CLIENT),
    TAB_UPDATE(Direction.SERVER_TO_CLIENT),
    NOTIFICATION(Direction.SERVER_TO_CLIENT),
    CONSOLE_APPEND(Direction.SERVER_TO_CLIENT),
    COMMAND_INPUT(Direction.CLIENT_TO_SERVER),
    COMMAND_TREE(Direction.SERVER_TO_CLIENT),
    COMMAND_SUGGEST_REQUEST(Direction.CLIENT_TO_SERVER),
    COMMAND_SUGGEST_RESPONSE(Direction.SERVER_TO_CLIENT),
    EVENT_FIRE(Direction.CLIENT_TO_SERVER),
    EVENT_ACK(Direction.SERVER_TO_CLIENT),
    STATE_SUBSCRIBE(Direction.CLIENT_TO_SERVER),
    PING(Direction.BIDIRECTIONAL),
    PONG(Direction.BIDIRECTIONAL),
    ERROR(Direction.BIDIRECTIONAL);

    private final Direction direction;

    MessageType(Direction direction) {
        this.direction = direction;
    }

    public Direction getDirection() {
        return direction;
    }

    public enum Direction {
        CLIENT_TO_SERVER,
        SERVER_TO_CLIENT,
        BIDIRECTIONAL
    }
}
