package com.Smoke.Signals;

public enum MessageType {
    PUBLIC(0),
    READ_RESPONSE(1),
    COMMENT(2),
    POST(3),
    EDIT_MESSAGE(4),
    IDENTITY_REQUEST(5),
    IDENTITY_RESPONSE(6),
    TYPING(7),
    FILE(8),
    LEAVE(9),
    REMOVE(10),
    ADD(11),
    EDIT_COMMENT(13),
    DELETE_COMMENT(14),
    DELETE_POST(15),
    EDIT_COMMENT_WITH_IMAGE(17),
    UNKNOWN(-1);

    private final int id;

    MessageType(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
