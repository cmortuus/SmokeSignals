import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class User {
    static String userName;
    private static HashMap<String, Pubsub> rooms;

    //    TODO use ipfs hash for user id and then associate that id with the username and if they want to change their username than send a message to say that
    //    TODO eventaully change this from one large file to one file that is for your username or aliasis
    //    TODO change this so that usernames are designated by the first line of a room and each user has their own folder of rooms
    public User(String user) throws IOException {
        this.userName = user;
        rooms = new HashMap<>();

//       Create the file or open it
        File file = new File("users.txt");
        if (!file.exists())
            file.createNewFile();
        FileWriter fw = new FileWriter(file, true);

        try (Scanner scnr = new Scanner(new File("users.txt"))) {
            if (!scnr.hasNextLine()) {
                SecureRandom rand = new SecureRandom();
                int nums = 0;
                while (nums <= 100000)
                    nums = rand.nextInt(1000000);
                this.userName = user + '#' + nums;
                fw.append(this.userName);
                fw.append("\n");
                System.out.println(this.userName);
                fw.flush();

            } else {
                boolean check = false;
                String tempLine;
                while (scnr.hasNextLine()) {
                    tempLine = scnr.nextLine();
                    if ((tempLine.split("#")[0].equals(user))) {
                        this.userName = tempLine;
                        check = true;
                    }
                }

                if (!check) {
                    SecureRandom rand = new SecureRandom();
                    int nums = 0;
                    while (nums <= 100000)
                        nums = rand.nextInt(1000000);
                    this.userName = user + '#' + nums;
                    fw.append(this.userName);
                    fw.append("\n");
                    System.out.println(this.userName);
                    fw.flush();
                }
            }
        }
    }

    /**
     * Creates pubsub room and adds it to the dict(rooms)
     *
     * @param otherUser
     */
    public void createRoom(String otherUser) {
        try {
            ExecutorService executorService = Executors.newFixedThreadPool(Integer.MAX_VALUE);
            String roomName = turnUsersToRoom(new String[]{userName});
            rooms.put(roomName, new Pubsub(turnUsersToRoom(otherUser), true));
//            Add new user to the arraylist in pubsub and then send that to
//            rooms.get(roomName).users.put(otherUser, null);
//            Test the room
            executorService.submit(rooms.get(roomName));
            rooms.get(roomName).writeToPubsub("1123*1231*2312*3123", false);
            rooms.get(roomName).writeToPubsub("hello", false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Turn the usernames of two users into a room name
     *
     * @param otherUser
     * @return
     */
    private String turnUsersToRoom(String otherUser) {
        String[] s = new String[]{otherUser, userName};
        Arrays.sort(s);
        return String.join("", s).replace('#', 'z');
    }

    /**
     * Overload of the normal method for group chats where there are many people in the chat
     *
     * @param users
     * @return
     */
    private String turnUsersToRoom(String[] users) {
        String[] s = new String[users.length];
        s[0] = userName;
        int i = 0;
        for (String user : users)
            s[i++] = user;
        Arrays.sort(s);
        return String.join("", s).replace('#', 'z');
    }


}
