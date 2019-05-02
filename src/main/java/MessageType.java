public enum MessageType {
    PUBLIC(0),
    READ_RESPONSE(1),
    COMMENT(2),
    POST(3),
    UNKNOWN(-1);

    private final int id;

    MessageType(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
