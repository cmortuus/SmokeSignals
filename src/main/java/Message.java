import org.json.JSONObject;

import java.sql.Timestamp;
import java.util.Objects;

/**
 * Details about an individual message
 */
public class Message {

    private long messageId;
    private long timestamp;
    private MessageType type;
    private boolean seen;
    private String author;
    private String content;

    /**
     * Creates a new message object from the provided input.
     *
     * @param messageId (long)        the message's unique identifier
     * @param timestamp (long)        the timestamp of the message's creation time
     * @param type      (MessageType) what type of message this is
     * @param seen      (boolean)     whether or not the message is flagged as seen
     * @param author    (String)      the username#discriminator of the author of the message
     * @param content   (String)      the text represented by this message
     */
    public Message(long messageId, long timestamp, MessageType type, boolean seen, String author, String content) {
        this.messageId = messageId;
        this.timestamp = timestamp;
        this.author = author;
        this.content = content;
        this.type = type;
        this.seen = seen;
    }

    /**
     * Creates a new message object from the json representation of the message
     *
     * @param json JSON text formatted as a message object
     */
    public Message(String json) {
        JSONObject _message = new JSONObject(json);
        this.messageId = _message.getLong("messageId");
        this.timestamp = _message.getLong("timestamp");
        this.type = MessageType.valueOf(_message.getString("type"));
        this.seen = _message.getBoolean("seen");
        this.author = _message.getString("author");
        this.content = _message.getString("content");
    }

    public long getMessageId() {
        return messageId;
    }

    public long getTimestampLong() {
        return timestamp;
    }

    public Timestamp getTimestamp() {
        return new Timestamp(timestamp);
    }

    public String getAuthor() {
        return author;
    }

    public String getContent() {
        return content;
    }

    public MessageType getMessageType() {
        return type;
    }

    public boolean hasBeenSeen() {
        return seen;
    }

    public void editContent(String newContent) {
        content = newContent;
    }

    public void editSeen(boolean newSeen) {
        seen = newSeen;
    }

    @Override public String toString() {
        JSONObject _message = new JSONObject();
        _message.put("messageId", messageId)
                .put("timestamp", timestamp)
                .put("type", type)
                .put("seen", seen)
                .put("author", author)
                .put("content", content);
        return _message.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Message)) return false;
        Message message = (Message) o;
        return messageId == message.messageId &&
                timestamp == message.timestamp &&
                seen == message.seen &&
                Objects.equals(author, message.author) &&
                Objects.equals(content, message.content) &&
                type == message.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageId, timestamp, author, content, type, seen);
    }
}
