import javax.crypto.SecretKey;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


class User {
    static String userName;
    private static HashMap<String, Pubsub> rooms;
    static ArrayList<SecretKey> secretKeys;
    static ArrayList<PublicKey> publicKeys;
    static ArrayList<OtherUser> otherUsers;
    //    TODO use ipfs hash for user id and then associate that id with the username and if they want to change their username than send a message to say that
    //    TODO eventually change this from one large file to one file that is for your username or aliases
    //    TODO change this so that usernames are designated by the first line of a room and each user has their own folder of rooms
    User(String user) throws IOException {
        userName = user;
        rooms = new HashMap<>();
        publicKeys = new ArrayList<>();
        secretKeys = new ArrayList<>();
        otherUsers = new ArrayList<OtherUser>();

        // Create the file or open it
        File file = new File("users.txt");
        FileWriter fw = new FileWriter(file, true);

        try (Scanner scnr = new Scanner(new File("users.txt"))) {
            if (!scnr.hasNextLine()) { // the file is empty

                // generate the random discriminator between max and min
                int min = 100000;
                int max = 1000000;
                int nums = new SecureRandom().nextInt((max - min) + 1) + min;

                userName = user + '#' + nums;
                fw.append(userName).append("\n");
                System.out.println(userName);
                fw.close();

            } else { // the file has content already stored in it

                // check if the file contains the specified username
                String tempLine;
                while (scnr.hasNextLine()) {
                    tempLine = scnr.nextLine();
                    if (tempLine.split("#",2)[0].equals(user)) {
                        userName = tempLine;
                        fw.close();
                        return;
                    }
                }

                // generate the random discriminator between max and min
                int min = 100000;
                int max = 1000000;
                int nums = new SecureRandom().nextInt((max - min) + 1) + min;

                userName = user + '#' + nums;
                fw.append(userName).append("\n");
                System.out.println(userName);
                fw.close();
            }
        }
    }

    /**
     * Creates pubsub room and adds it to the dict(rooms)
     *
     * @param otherUser The username of the other person who is in the room with you This is needed to create the roomname
     */
    void createRoom(String otherUser) {

        if (!isValidUserFormat(otherUser)) {
            //TODO: Deal with situation when the input does not contain a valid username#discriminator
        }

        try {
            ExecutorService executorService = Executors.newFixedThreadPool(Integer.MAX_VALUE);
            String roomName = turnUsersToRoom(userName);
            rooms.put(roomName, new Pubsub(turnUsersToRoom(otherUser), true));
//            Add new user to the arraylist in pubsub and then send that to
//            rooms.get(roomName).users.put(otherUser, null);
//            Test the room
            executorService.submit(rooms.get(roomName));
            rooms.get(roomName).writeToPubsub("1123*1231*2312*3123", 0);
            rooms.get(roomName).writeToPubsub("hello", 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Creates pubsub room and adds it to the dict(rooms)
     *
     * @param otherUser The username of the other person who is in the room with you
     */
    void createRoom(String[] otherUser) {
        try {
            ExecutorService executorService = Executors.newFixedThreadPool(Integer.MAX_VALUE);
            String roomName = turnUsersToRoom(userName);
            rooms.put(roomName, new Pubsub(turnUsersToRoom(otherUser), true));
//            Add new user to the arraylist in pubsub and then send that to
//            rooms.get(roomName).users.put(otherUser, null);
//            Test the room
            executorService.submit(rooms.get(roomName));
            rooms.get(roomName).writeToPubsub("1123*1231*2312*3123", 0);
            rooms.get(roomName).writeToPubsub("hello", 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Turn the usernames of two users into a room name
     *
     * @param otherUser The username that is being added to yours to create the roomname
     * @return the new roomname
     */
    private String turnUsersToRoom(String otherUser) {
        String[] s = new String[]{otherUser, userName};
        Arrays.sort(s);
        return String.join("", s).replace('#', 'z');
    }

    /**
     * Overload of the normal method for group chats where there are many people in the chat
     *
     * @param users A list of users with which will be added to yours to create the roomname
     * @return the new username
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

    /**
     * Checks if the passed string is properly formatted as username#discriminator
     * @param username name to check
     * @return true if the format is valid
     */
    private boolean isValidUserFormat(String username) {
        return username.matches("(.+#[0-9]+)");
    }
}
