
import javax.crypto.SecretKey;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


class User {

    private String userName;
    private HashMap<String, Pubsub> rooms;
    private HashMap<String, Pair<SecretKey, String>> secretKeys;
    private ArrayList<OtherUser> otherUsers;
    private ExecutorService executorService;

    //    TODO use ipfs hash for user id and then associate that id with the username and if they want to change their username than send a message to say that
    //    TODO eventually change this from one large file to one file that is for your username or aliases
    //    TODO change this so that usernames are designated by the first line of a room and each user has their own folder of rooms
    User(String user) throws IOException {

        userName = user;
        rooms = new HashMap<>();
        secretKeys = new HashMap<>();
        otherUsers = new ArrayList<>();
        executorService = Executors.newFixedThreadPool(Integer.MAX_VALUE);

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

    public String getUserName() {
        return userName;
    }

    public void addSecretKey(String associatedUser, Pair<SecretKey, String> pair) {
        secretKeys.put(associatedUser, pair);
    }

    public Pair<SecretKey, String> getUserAesKey(String user) {
        return secretKeys.get(user);
    }

    public ArrayList<OtherUser> getOtherUsers() {
        return otherUsers;
    }

    /**
     * Creates pubsub room and adds it to the dict(rooms)
     *
     * @param otherUser The username of the other person who is in the room with you This is needed to create the roomname
     */
    void createRoom(String otherUser) {
        if (isValidUserFormat(otherUser)) {
            try {
                String roomName = turnUsersToRoom(otherUser);
                rooms.put(roomName, new Pubsub(this, roomName, true));
                executorService.submit(rooms.get(roomName));
                while (true) {
                    rooms.get(roomName).sendMessage("1123*1231*2312*3123");
                    rooms.get(roomName).sendMessage("hello");
                    Thread.sleep(100);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }else{
            System.out.println("The username was of an invalid format");
        }
    }


    /**
     * Creates pubsub room and adds it to the dict(rooms)
     *
     * @param otherUser The username of the other person who is in the room with you
     */
    void createRoom(String[] otherUser) {
        try {
            String roomName = turnUsersToRoom(otherUser);
            rooms.put(roomName, new Pubsub(this, roomName, true));
//            Add new user to the arraylist in pubsub and then send that to
//            rooms.get(roomName).users.put(otherUser, null);
//            Test the room
            executorService.submit(rooms.get(roomName));
                rooms.get(roomName).sendMessage("1123*1231*2312*3123");
                rooms.get(roomName).sendMessage("hello");
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String joinExistingRoom(String otherUser) {
        if (!isValidUserFormat(otherUser))
            throw new IllegalArgumentException("user does not fit the format username#discriminator");
        String roomName = turnUsersToRoom(otherUser);
        rooms.put(roomName, new Pubsub(this, roomName, true));
        executorService.submit(rooms.get(roomName));
        return roomName;
    }

    public void sendToRoom(String roomName, String message) {
        if (!rooms.containsKey(roomName))
            throw new IllegalArgumentException("room does not exist");
        rooms.get(roomName).sendMessage(message);
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
    static boolean isValidUserFormat(String username) {
        return username.matches("(.+#[0-9]+)");
    }
}
