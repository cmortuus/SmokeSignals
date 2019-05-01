import java.util.ArrayList;

public class Post extends Message {
    ArrayList<Message> comments;

    Post(long postID, long timestamp, String author, String content, boolean seen){
        super(postID, timestamp, author, content, seen);
        comments = new ArrayList<>();
    }

}
