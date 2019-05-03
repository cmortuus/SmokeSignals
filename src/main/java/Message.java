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
     * Creates a new message object from the string representation of the message
     *
     * @param messageData (String) message-id|timestamp|type-id|seen|author|content
     * @throws IllegalArgumentException if the string is improperly formatted
     */
    public Message(String messageData) {
//        if (!messageData.matches("([0-9]+\\|[0-9]+\\|[0-9]+\\|(true|false)\\|.+\\|.+)"))
//            throw new IllegalArgumentException("does not follow the format \"message-id|timestamp|type-id|seen|author#discriminator|content\"");
        if (messageData.isEmpty()) throw new IllegalArgumentException("does not follow the format \"message-id|timestamp|type-id|seen|author#discriminator|content\"");
        String[] split = messageData.split("\\|",6);
        if (split.length != 6) throw new IllegalArgumentException("does not follow the format \"message-id|timestamp|type-id|seen|author#discriminator|content\"");
        try { messageId = Long.parseLong(split[0]);
        } catch (NumberFormatException nfe) { throw new IllegalArgumentException("cannot parse (long) message id from \""+split[0]+"\""); }
        try { timestamp = Long.parseLong(split[1]);
        } catch (NumberFormatException nfe) { throw new IllegalArgumentException("cannot parse (long) timestamp from \""+split[1]+"\""); }
        try { type = MessageType.values()[Integer.parseInt(split[2])];
        } catch (NumberFormatException nfe) { throw new IllegalArgumentException("cannot parse (int) message type from \""+split[2]+"\""); }
        if (split[3].toLowerCase().equals("true")) seen = true;
        else if (split[3].toLowerCase().equals("false")) seen = false;
        else throw new IllegalArgumentException("cannot parse (long) message id from \""+split[3]+"\"");
        author = split[4];
        content = split[5];
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
        return messageId+"|"+timestamp+"|"+type.getId()+"|"+seen+"|"+author+"|"+content;
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
