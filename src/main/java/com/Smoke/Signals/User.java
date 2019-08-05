package com.Smoke.Signals;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

class User {

    private HashMap<String, Pubsub> rooms;
    private HashMap<String, Pair<SecretKey, String>> secretKeys;
    static private ArrayList<OtherUser> otherUsers;

    private String username;
    private Account account;

    private SocialMediaFeed socialMediaFeed;
    static Logging loggingChannel;
    private boolean initialized;

    final String alphabet = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    final int N = alphabet.length();

    //    TODO use ipfs hash for user id and then associate that id with the username and if they want to change their username than send a message to say that
    //    TODO change this so that usernames are designated by the first line of a room and each user has their own folder of rooms

    /**
     * Creates an instance of a client. {@link User#initialize()} must be called next!
     *
     * @param user the username the client is connected to. (username#discriminator for existing accounts, username for new accounts)
     */
    User(String user) {
        initialized = false;
        username = user;
        account = null;
        rooms = new HashMap<>();
        secretKeys = new HashMap<>();
        otherUsers = new ArrayList<>();
    }

    /**
     * Initializes everything. This must be called before anything else!
     *
     * @throws IOException
     */
    void initialize() throws IOException {

        // ensure valid username#discrim; regen discrim if necessary
        if (!isValidUserFormat(username))
            username = username + '#' + Account.generateDiscriminator();
        if (main.DEBUG) System.out.println("[DEBUG] active username#discrim is " + username);

        // grab or generate account save data
        int index = username.lastIndexOf('#');
        account = FileLoader.getAccount(username.substring(0, index), username.substring(index + 1));

        // start logging broadcaster and social media receiver
        loggingChannel = new Logging(this);
        socialMediaFeed = new SocialMediaFeed(this, false);

        // reconnect to existing roomnames
        for (String roomname : account.getRoomnames())
            rooms.put(roomname, new Pubsub(this, roomname, true));

        initialized = true;
    }

    /**
     * @return true if the client has been initialized
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Retrieves the active account bound to the client.
     *
     * @return the {@link Account} or {@linkplain null} if the client has not been initialized
     */
    Account getAccount() {
        return account;
    }

    /**
     * Retrieves the logging channel bound to the client.
     *
     * @return the {@link Logging} or {@linkplain null} if the client has not been initialized
     */
    Logging getLogger() {
        if (!initialized) return null;
        return loggingChannel;
    }

    /**
     * Saves the account bound to the client if the client has been initialized.
     */
    void saveAccount() {
        if (!initialized) return;
        try {
            FileLoader.saveAccount(account);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void addSecretKey(String associatedUser, Pair<SecretKey, String> pair) {
        secretKeys.put(associatedUser, pair);
    }

    Pair<SecretKey, String> getUserAesKey(String user) {
        return secretKeys.get(user);
    }

    ArrayList<OtherUser> getOtherUsers() {
        return otherUsers;
    }

    /**
     * @param otherUser the username#discriminator of the user you wish to connect to
     * @return the created roomname or {@linkplain null} if the client has not been initialized
     */
    String joinRoom(String otherUser) {
        String roomName = generateRoomName();
        if (!initialized) return null;
        new Pubsub(this, otherUser, false).writeToPubsub(roomName + username, MessageType.SEND_INVITE);
        if (!isValidUserFormat(otherUser))
            throw new IllegalArgumentException("user does not fit the format username#discriminator");

//        String roomName = turnUsersToRoom(otherUser);
        if (rooms.containsKey(roomName)) throw new IllegalArgumentException("room already exists");
        account.addRoom(roomName);
        rooms.put(roomName, new Pubsub(this, roomName, true));
        saveAccount();
        return roomName;
    }
    /**
     * @param otherUser the username#discriminator of the user you wish to connect to
     * @return the created roomname or {@linkplain null} if the client has not been initialized
     */
   String joinRoom(String otherUser, String roomName) {
        if (!initialized) return null;
        if (!isValidUserFormat(otherUser))
            throw new IllegalArgumentException("user does not fit the format username#discriminator");

//        String roomName = turnUsersToRoom(otherUser);
        if (rooms.containsKey(roomName)) throw new IllegalArgumentException("room already exists");
        account.addRoom(roomName);
        rooms.put(roomName, new Pubsub(this, roomName, true));
        saveAccount();
        return roomName;
    }

    void sendToRoom(String roomName, String message) {
        if (!rooms.containsKey(roomName))
            throw new IllegalArgumentException("room does not exist");
        rooms.get(roomName).sendMessage(message);
    }

    boolean isRoomReady(String roomName) {
        if (!rooms.containsKey(roomName))
            throw new IllegalArgumentException("room does not exist");
        return rooms.get(roomName).isReady();
    }

    private String generateRoomName(){
        Random r = new Random();
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < 1024; i++)
           s.append(alphabet.charAt(r.nextInt(N)));
        return s.toString();
    }

    /**
     * Checks if the passed string is properly formatted as username#discriminator
     *
     * @param username name to check
     * @return true if the format is valid
     */
    private boolean isValidUserFormat(String username) {
        return username.matches("(.+#[0-9]+)$");
    }
}
