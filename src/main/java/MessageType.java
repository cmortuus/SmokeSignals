public enum MessageType {
    PUBLIC(0),
    READ_RESPONSE(1),
    COMMENT(2),
    POST(3),
    EDIT(4),
    IDENTITY_REQUEST(5),
    IDENTITY_RESPONSE(6),
    TYPING(7),
    FILE(8),
    UNKNOWN(-1);

    private final int id;

    MessageType(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
