import account.Account;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


class User {

    private HashMap<String, Pubsub> rooms;
    private HashMap<String, Pair<SecretKey, String>> secretKeys;
    private ArrayList<OtherUser> otherUsers;
    private ExecutorService executorService;

    private ArrayList<Account> accounts;
    private Account yourAccount;

    //    TODO use ipfs hash for user id and then associate that id with the username and if they want to change their username than send a message to say that
    //    TODO eventually change this from one large file to one file that is for your username or aliases
    //    TODO change this so that usernames are designated by the first line of a room and each user has their own folder of rooms
    User(String user) {
        try {
            rooms = new HashMap<>();
            secretKeys = new HashMap<>();
            otherUsers = new ArrayList<>();
            executorService = Executors.newFixedThreadPool(Integer.MAX_VALUE);
            accounts = FileLoader.loadAccounts("accounts.txt");

            for (Account account : accounts) {
                if (account.getUsername().equals(user)) {
                    yourAccount = account;
                    return;
                }
            }

            yourAccount = new Account(user);
            accounts.add(yourAccount);
            FileLoader.saveAccounts(accounts, "accounts.txt");
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public Account getAccount() {
        return yourAccount;
    }

    public void saveAccounts() {
        try {
            FileLoader.saveAccounts(accounts, "accounts.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    public String joinRoom(String otherUser) {
        if (!isValidUserFormat(otherUser))
            throw new IllegalArgumentException("user does not fit the format username#discriminator");
        String roomName = turnUsersToRoom(otherUser);
        yourAccount.addRoom(roomName);
        rooms.put(roomName, new Pubsub(this, roomName, true));
        executorService.submit(rooms.get(roomName));
        return roomName;
    }

    public void sendToRoom(String roomName, String message) {
        if (!rooms.containsKey(roomName))
            throw new IllegalArgumentException("room does not exist");
        rooms.get(roomName).sendMessage(message);
    }

    public boolean isRoomReady(String roomName) {
        if (!rooms.containsKey(roomName))
            throw new IllegalArgumentException("room does not exist");
        return rooms.get(roomName).isReady();
    }

    /**
     * Turn the usernames of two users into a room name
     *
     * @param otherUser The username that is being added to yours to create the roomname
     * @return the new roomname
     */
    private String turnUsersToRoom(String otherUser) {
        String[] s = new String[]{otherUser, yourAccount.getFullUsername()};
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
        s[0] = yourAccount.getFullUsername();
        int i = 0;
        for (String user : users)
            s[i++] = user;
        Arrays.sort(s);
        return String.join("", s).replace('#', 'z');
    }

    /**
     * Checks if the passed string is properly formatted as username#discriminator
     *
     * @param username name to check
     * @return true if the format is valid
     */
    static boolean isValidUserFormat(String username) {
        return username.matches("(.+#[0-9]+)");
    }
}
