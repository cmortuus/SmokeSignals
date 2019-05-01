import java.sql.Timestamp;

/**
 * Details about an individual message
 */
public class Message {

    private long messageId;
    private long timestamp;
    private String author;
    private String content;
    private boolean seen;

    public Message(long messageId, long timestamp, String author, String content, boolean seen) {
        this.timestamp = timestamp;
        this.author = author;
        this.content = content;
        this.seen = seen;
        this.messageId = messageId;
    }

    public long getMessageId() {
        return messageId;
    }

    public long getReceivedLong() {
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

    public boolean hasBeenSeen() {
        return seen;
    }

    public void editContent(String newContent) {
        content = newContent;
    }

    public void editSeen(boolean newSeen) {
        seen = newSeen;
    }
}
