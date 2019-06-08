import java.util.ArrayList;
public class Post {
    ArrayList<Message> comments;
    ArrayList<String> likes;
    ArrayList<String> views;

    Post(Message message){
        comments = new ArrayList<>();
    }
}
