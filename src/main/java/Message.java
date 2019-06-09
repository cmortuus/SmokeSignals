import org.json.JSONObject;

import java.sql.Timestamp;
import java.util.Objects;

/**
 * Details about an individual message
 */
public class Message implements Comparable<Message> {

    private long messageId;
    private long timestamp;
    private MessageType type;
    private boolean seen;
    private long authorId;
    private String content;

    /**
     * Creates a new message object from the provided input.
     *
     * @param messageId (long)        the message's unique identifier
     * @param timestamp (long)        the timestamp of the message's creation time
     * @param type      (MessageType) what type of message this is
     * @param seen      (boolean)     whether or not the message is flagged as seen
     * @param authorId  (long)        the userid of the author of the message
     * @param content   (String)      the text represented by this message
     */
    public Message(long messageId, long timestamp, MessageType type, boolean seen, long authorId, String content) {
        this.messageId = messageId;
        this.timestamp = timestamp;
        this.authorId = authorId;
        this.content = content;
        this.type = type;
        this.seen = seen;
    }

    /**
     * Creates a new message object from the json representation of the message
     *
     * @param json JSON text formatted as a message object
     */
    public Message(JSONObject json) {
        if (!json.has("messageId") || !json.has("timestamp") || !json.has("type") || !json.has("seen") || !json.has("authorId") || !json.has("content"))
            throw new IllegalArgumentException("missing fields");
        this.messageId = json.getLong("messageId");
        this.timestamp = json.getLong("timestamp");
        this.type = MessageType.valueOf(json.getString("type"));
        this.seen = json.getBoolean("seen");
        this.authorId = json.getLong("authorId");
        this.content = json.getString("content");
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

    public long getAuthorId() {
        return authorId;
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

    public JSONObject toJSONObject() {
        return new JSONObject()
                .put("messageId", messageId)
                .put("timestamp", timestamp)
                .put("type", type)
                .put("seen", seen)
                .put("authorId", authorId)
                .put("content", content);
    }

    @Override public String toString() {
        JSONObject _message = new JSONObject();
        _message.put("messageId", messageId)
                .put("timestamp", timestamp)
                .put("type", type)
                .put("seen", seen)
                .put("authorId", authorId)
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
                Objects.equals(authorId, message.authorId) &&
                Objects.equals(content, message.content) &&
                type == message.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageId, timestamp, authorId, content, type, seen);
    }

    @Override
    public int compareTo(Message o) {
        if (this.timestamp == o.timestamp) return 0;
        return Long.compare(timestamp, o.timestamp);
    }
}
