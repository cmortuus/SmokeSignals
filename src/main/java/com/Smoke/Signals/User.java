package com.Smoke.Signals;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Random;

class User {

    private static HashMap<String, Pubsub> rooms;

    private String username;
    private Account account;

    private SocialMediaFeed socialMediaFeed;
    private Logging loggingChannel;
    private HashMap<String, Invite> inviteRooms;
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
        rooms = new HashMap<>();
        username = user;
        account = null;
        socialMediaFeed = null;
        initialized = false;
        inviteRooms = new HashMap<>();
    }

    /**
     * Initializes everything. This must be called before anything else!
     *
     * @throws Exception
     */
    void initialize() throws Exception {

        // ensure valid username#discrim; regen discrim if necessary
        if (!isValidUserFormat(username))
            username = username + '#' + Account.generateDiscriminator();
        if (main.DEBUG) System.out.println("[DEBUG] active username#discrim is " + username);

        // grab or generate account save data
        int index = username.lastIndexOf('#');
        account = FileLoader.getAccount(username.substring(0, index), username.substring(index+1));

        // start logging broadcaster and social media receiver
        loggingChannel = new Logging(this);
        socialMediaFeed = new SocialMediaFeed(this, generateRoomName(), false);
        startNewInviteRoom();

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
        try { FileLoader.saveAccount(account);
        } catch (IOException e) { e.printStackTrace(); }
    }

    void sendInvite(String otherUser) throws Exception {
        if (!initialized) return;
        if (!isValidUserFormat(otherUser))
            throw new IllegalArgumentException("user does not fit the format username#discriminator");
        String roomname = generateRoomName();
        Pubsub room;
        if (rooms.containsValue(otherUser)) {
            room = rooms.get(otherUser);
        } else {
            room = new Pubsub(this, otherUser, false);
            rooms.put(otherUser, room);
        }
        room.writeToPubsub(roomname + username, MessageType.SEND_INVITE);
        saveAccount();
    }

    void sendInvite(String otherUser, String roomname, boolean saveMessages) throws Exception {
        if (!initialized) return;
        if (!isValidUserFormat(otherUser))
            throw new IllegalArgumentException("user does not fit the format username#discriminator");
        Pubsub room;
        if (rooms.containsValue(otherUser)) {
            room = rooms.get(otherUser);
        } else {
            room = new Pubsub(this, otherUser, false);
            rooms.put(otherUser, room);
        }
        JSONArray peersArray = new JSONArray();
        for (Peer p : account.getPeers().values())
            peersArray.put(p.toJSONObject());

        JSONObject jsonObject = new JSONObject().put("roomName", roomname).put("userName", username).put("peers", peersArray).put("saveMessages", saveMessages);
        room.writeToPubsub(jsonObject.toString(), MessageType.SEND_INVITE);
        saveAccount();
    }

    /**
     * @param otherUser the username#discriminator of the user you wish to connect to
     * @return the created roomname or {@linkplain null} if the client has not been initialized
     */
   String joinRoom(String otherUser, String roomName) throws Exception {
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

    static void addToRooms(String roomName, Pubsub room){
       rooms.put(roomName, room);
    }

    void removeInviteRoom(String roomname) {
       main.debug("removed invite room "+roomname);
       inviteRooms.remove(roomname);
    }

    void startNewInviteRoom() throws Exception {
       String name = account.getUsername()+account.getDiscriminator();
       if (!inviteRooms.containsKey(name)) {
           inviteRooms.put(name, new Invite(this, name));
           main.debug("started new invite room "+name);
       }
    }

//    /**
//     * Turn the usernames of two users into a room name
//     *
//     * @param otherUser The username that is being added to yours to create the roomname
//     * @return the new roomname
//     */
//    private String turnUsersToRoom(String otherUser) {
//        String[] s = new String[]{otherUser, account.getFullUsername()};
//        Arrays.sort(s);
//        return String.join("", s).replace('#', 'z');
//    }
//
//    /**
//     * Overload of the normal method for group chats where there are many people in the chat
//     *
//     * @param users A list of users with which will be added to yours to create the roomname
//     * @return the new username
//     */
//    private String turnUsersToRoom(String[] users) {
//        String[] s = new String[users.length];
//        s[0] = account.getFullUsername();
//        int i = 0;
//        for (String user : users)
//            s[i++] = user;
//        Arrays.sort(s);
//        return String.join("", s).replace('#', 'z');
//    }

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
