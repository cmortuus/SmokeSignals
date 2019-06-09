import account.Account;
import account.Peer;
import io.ipfs.api.IPFS;
import io.ipfs.multiaddr.MultiAddress;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Stream;

//TODO when making the social media side of it allow people to add people to chats, but not add them to the social media feed
//TODO put a handshake in on each message so that if somebody misses a message than they all fill in the gaps
//TODO Have them keep trying to decrypt the aes keys until they have everyone in the chat and then if one of the keys does not work throw an error to have everyone remake and resend the keys
//TODO add voice features in as well
//TODO make it so you can delete a person from a chatroom if they delete the app
//TODO make sure that people can see the message when it is not writing to a file
//TODO send message when you go online and then when the app closes send message that you are offline when you come online everyone tells you if they are online everything else is assumed offline
//TODO write method to pull messages from the file after app has been closed
//TODO implement parental controls
//TODO work on social media part of it
//TODO when a comment is added resend the post and have that overwrite the original post in people's timeline
//TODO figure out how to do *Eco is typing* Probably just send a message ahead of time
//TODO find a way to do num mutual friends
//TODO build in emoji support
//TODO complete all the todo statements
public class Pubsub implements Runnable {

    private User yourself;
    private Account account;
    private Stream<Map<String, Object>> room;
    private String roomName;
    private IPFS ipfs;

    // username and hash
    private HashMap<String, String> users;
    private int numUsersFound;
    private ArrayList<OtherUser> usersInRoom;

    // messages
    private boolean saveMessage;
    private ArrayList<Message> messages;
    private HashMap<Long, Message> messageLookup;

    // encryption
    private SecretKey aesKey;
    private String iv;
    private PrivateKey privateKey;
    private PublicKey publicKey;

    private boolean ready;
    private static final boolean DEBUG = false;

    Pubsub(User yourself, String roomName, boolean saveMessage) {
        System.out.println("RoomName = " + roomName);
        try {
            this.yourself = yourself;
            account = yourself.getAccount();
            users = new HashMap<>();
            this.roomName = roomName;
            ipfs = new IPFS(new MultiAddress("/ip4/127.0.0.1/tcp/5001"));
            room = ipfs.pubsub.sub(roomName);
            usersInRoom = new ArrayList<>();

            // messages
            this.saveMessage = saveMessage;
            messages = new ArrayList<>();
            messageLookup = new HashMap<>();

            //encryption
            aesKey = Encryption.generateAESkey();
            byte[] b = new byte[16];
            new SecureRandom().nextBytes(b);
            iv = MyBase64.encode(b);

            KeyPair keypair = Encryption.generateKeys();
            privateKey = keypair.getPrivate();
            publicKey = keypair.getPublic();

            ready = false;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends a message to the connected chat.
     *
     * @param message text to be sent into the chat
     * @throws IllegalStateException if the method is called prior to connecting to the room
     */
    public void sendMessage(String message) {
        if (!ready) throw new IllegalStateException("not connected to chat");
        writeToPubsub(message, MessageType.PUBLIC);
    }

    public boolean isReady() {
        return ready;
    }

    /**
     * Threaded method that reads the messages out after they have been sent, unHashes them, decrypts them, and in the case of internal messages applies their message.
     * The handshake function was built into this one
     */
    @Override
    public void run() {

        try {
            loadMessages();
            printLastMessages(20);

            // initiate handshake
            new Thread(() -> {
                String lastPeers = "";
                while (true) {
                    if (DEBUG)
                    try {
                        String currentPeers = ipfs.pubsub.peers(roomName).toString();
                        if (!currentPeers.equals(lastPeers)) {
                            lastPeers = currentPeers;
                            debug("peers: " + currentPeers);
                        }
                    } catch (IOException ignore) {}
                    if (!ready)
                    try { debug("attempting to start a handshake");
                        ipfs.pubsub.pub(roomName, createOutgoingRsaText());
                    } catch (Exception e) { e.printStackTrace(); }
                    try { Thread.sleep(10000);
                    } catch (InterruptedException ignore) {}
                }
            }).start();

            // write out each line of the stream to a file and check if they are one of the users
            room.forEach(stringObjectMap -> {
                if (stringObjectMap.isEmpty()) return;
                try {

                    String base64Data = stringObjectMap.values().toString().split(",")[1].trim();
                    byte[] decodedBytes = MyBase64.decode(base64Data);
                    String decodedString = new String(decodedBytes).replaceAll(" ", "+");

                    //TODO: is there a better way to differentiate the sender?
                    String sender = stringObjectMap.toString().split(",",2)[0].substring(6);

                    Pair<SecretKey, String> authorAesKey = yourself.getUserAesKey(sender);
                    if (authorAesKey == null) { // if we have not received an aes key from the author yet then perform a handshake
                        shakeHands(sender, decodedString);
                        return;
                    }

                    String decryptedMessage;
                    try { decryptedMessage = Encryption.decrypt(decodedString, authorAesKey.getKey(), authorAesKey.getValue());
                    } catch (Exception e) {
                        shakeHands(sender, decodedString);
                        return;
                    }

                    if (!isJson(decryptedMessage)) return;
                    Message message = parseMessage(decryptedMessage);
                    if (account.getPeer(message.getAuthorId()).getDiscriminator().equals("000000"))
                        writeToPubsub(String.valueOf(message.getAuthorId()), MessageType.IDENTITY_REQUEST);

                    //TODO: better message save stuff
                    //TODO: decide if the message should be stored in messages
                    // process the message according to its type
                    switch (message.getMessageType()) {
                        case PUBLIC: {
                            messages.add(message);
                            messageLookup.put(message.getMessageId(), message);
                            saveMessages();

                            System.out.println(message.getMessageId() + "  " +
                                    getTime(message.getTimestampLong()) + "  " +
                                    account.getPeer(message.getAuthorId()).getUsername() + "  " +
                                    message.getContent());

                            if (message.getAuthorId() == account.getUserId()) {
                                if (message.getContent().startsWith("--edit")) {
                                    String[] split = message.getContent().split(" ", 3);
                                    if (split.length != 3) return;
                                    try {
                                        long msgId = Long.parseLong(split[1]);
                                        Message msg = messageLookup.get(msgId);
                                        if (msg == null || msg.getAuthorId() != message.getAuthorId()) return;
                                        editMessage(msg, split[2]);
                                    } catch (NumberFormatException ignore) {}
                                } else if (message.getContent().equals("--shutdown")) {
                                    sendMessage("[SYSTEM] shutting down in 5 seconds");
                                    try { Thread.sleep(5000);
                                    } catch (InterruptedException ignore) {}
                                    System.exit(0);
                                }
                            }
                            break;
                        }

                        case READ_RESPONSE: {
                            Message msg = messageLookup.get(message.getMessageId());
                            if (msg != null) msg.editSeen(true);
                            break;
                        }

                        case COMMENT: {
                            messages.add(message);
                            messageLookup.put(message.getMessageId(), message);
                            saveMessages();
                            SocialMediaFeed.posts.put(message.getAuthorId(), new Post(message));
                            break;
                        }

                        case POST: {
                            messages.add(message);
                            messageLookup.put(message.getMessageId(), message);
                            saveMessages();
                            //TODO: handle receiving a social media post
                            break;
                        }

                        case EDIT: {
                            Message msg = messageLookup.get(message.getMessageId());
                            if (msg == null) return;
//                                String oldContent = msg.getContent();
                            msg.editContent(message.getContent());
//                                String response = "<type=edit messageId="+msg.getMessageId()+" oldContent=\""+oldContent+"\" newContent=\""+msg.getContent()+"\">";
//                                sendMessage(response);
                            break;
                        }

                        case IDENTITY_REQUEST: {
                            if (Long.valueOf(message.getContent()) == account.getUserId()) {
                                JSONObject payload = new JSONObject();
                                payload.put("username", account.getUsername())
                                        .put("discriminator", account.getDiscriminator());
                                writeToPubsub(payload.toString(), MessageType.IDENTITY_RESPONSE);
                            }
                        }

                        case IDENTITY_RESPONSE: {
                            Peer peer = account.getPeer(message.getAuthorId());
                            if (isJson(message.getContent())) {
                                JSONObject payload = new JSONObject(message.getContent());
                                peer.updateUsername(payload.getString("username"));
                                peer.updateDiscriminator(payload.getString("discriminator"));
                                yourself.saveAccounts();
                            }
                        }

                        case UNKNOWN: {
                            //TODO: handle any unknown messages
                            break;
                        }
                    }

                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void shakeHands(String sender, String message) {
        if (!MyBase64.isBase64(message)) return;

        //TODO: verify that this is someone we actually want to perform a handshake with
        try { // stage 1

            /*
            Assume the message contains the public rsa key of a different peer.
            Use their public rsa key to encrypt your secret aes key and send it to the other peer.
             */

            debug("attempting handshake stage 1");
            PublicKey key = parseIncomingRsaText(message);
            if (key == null) throw new Exception();
            //TODO: implement check to ensure no suspicious accounts are listening in
            ipfs.pubsub.pub(roomName, createOutgoingAesText(key));
            //TODO create user object and add it to the list for the room and for user
//          if (++numUsersFound == users.size() && ipfs.pubsub.peers(roomName).toString().split(",").length <= users.size()) {
            debug("completed handshake stage 1");
        } catch (Exception ignore) { // stage 2

            /*
            Assume the message contains a secret aes key of a different peer encrypted with your public rsa key.
            Use their aes key to encrypt your aes key and send it to the other peer.
             */

            debug("attempting handshake stage 2");
            try {
                Pair<SecretKey, String> pair = parseIncomingAesKey(message);
                if (pair == null) return;
                if (pair.getKey() == null) throw new Exception();
                yourself.addSecretKey(sender, pair);
                ipfs.pubsub.pub(roomName, Encryption.encrypt(MyBase64.encode(aesKey.getEncoded())+"|"+iv, pair.getKey(), pair.getValue()));
                if (!Arrays.equals(pair.getKey().getEncoded(), aesKey.getEncoded())) // check if you are performing a handshake with yourself
                    ready = true;
                debug("completed handshake stage 2");
            } catch (Exception ignore2) { // stage 3

                /*
                Assume the message contains a secret aes key of a different peer encrypted with your secret aes key.
                Store their secret aes key for later reference.
                 */

                debug("attempting handshake stage 3");
                try {
                    String decrypted = Encryption.decrypt(message, aesKey, iv);
                    String[] joined = decrypted.split("\\|");
                    if (joined.length != 2 || !MyBase64.isBase64(joined[0])) return;
                    byte[] bytes = MyBase64.decode(joined[0]);
                    SecretKey key = new SecretKeySpec(bytes, "AES");
                    yourself.addSecretKey(sender, new Pair<>(key, joined[1]));
                    ready = true;
                    debug("completed handshake stage 3");
                } catch (Exception ignore3) {}
            }
        }
    }

    /**
     * Converts properly formatted JSON into the {@link Message} object
     *
     * @param json Array of all the parts of a decrypted message. index 0 = message id, 1 = timestamp, 2 = username, 3 = message content
     * @return {@link Message} object
     */
    private Message parseMessage(String json) {
        return new Message(new JSONObject(json));
    }

    /**
     * Turns the epoch time sent in the message to human readable time
     * At some point this will have to be related to current time. ie 20 min ago
     *
     * @param time The Epoch time that the message was sent at
     * @return The human readable time that the message was sent at
     */
    private String getTime(String time) {
        return getTime(Long.parseLong(time));
    }

    /**
     * Turns the epoch time sent in the message to human readable time
     * At some point this will have to be related to current time. ie 20 min ago
     *
     * @param time The Epoch time that the message was sent at
     * @return The human readable time that the message was sent at
     */
    private String getTime(long time) {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss");
        return sdf.format(new Date(time));
    }

    private String createOutgoingRsaText() {
        return MyBase64.encode(publicKey.getEncoded());
    }
    private PublicKey parseIncomingRsaText(String text)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        if (!MyBase64.isBase64(text)) return null;
        byte[] key = MyBase64.decode(text);
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(key));
    }
    private String createOutgoingAesText(PublicKey publicKey)
            throws BadPaddingException, InvalidKeyException, IllegalBlockSizeException {
        Objects.requireNonNull(publicKey, "public key cannot be null");
        String joined = MyBase64.encode(aesKey.getEncoded())+"|"+iv;
        byte[] encrypted = Encryption.encryptWithRsa(joined, publicKey);
        return MyBase64.encode(encrypted);
    }

    /**
     *
     * @param text
     * @return a pair containing the SecretKey and initVector or null if the contents could not be parsed out
     * @throws BadPaddingException
     * @throws InvalidKeyException
     * @throws IllegalBlockSizeException
     */
    private Pair<SecretKey, String> parseIncomingAesKey(String text)
            throws BadPaddingException, InvalidKeyException, IllegalBlockSizeException {
        if (!MyBase64.isBase64(text)) return null;
        byte[] bytes = MyBase64.decode(text);
        String joined = Encryption.decryptWithRsa(bytes, privateKey);
        String[] split = joined.split("\\|");
        if (split.length != 2) throw new IllegalArgumentException("invalid format");
        if (!MyBase64.isBase64(split[0])) throw new IllegalArgumentException("invalid format");
        SecretKey key = new SecretKeySpec(MyBase64.decode(split[0]), "AES");
        return new Pair<>(key, split[1]);
    }

    /**
     * Send a message into the room. [this method is for internal use]
     *
     * @param message   text to be sent into the connected room
     * @param type      type of message (See {@link MessageType})
     */
    protected void writeToPubsub(String message, MessageType type) {
        if (!ready) throw new IllegalStateException("cannot send prior to handshake");

        try {
            long time = System.currentTimeMillis();
            long id = message.hashCode()+time;
            Message _message = new Message(id, time, type, false, account.getUserId(), message);
            String encPhrase = Encryption.encrypt(_message.toString(), aesKey, iv);
            ipfs.pubsub.pub(roomName, encPhrase);
        } catch (Exception e) { e.printStackTrace(); }
    }

    /**
     * Write the message to the specified room. [this method is for internal use]
     *
     * @param roomName  the room to send the message to
     * @param message   text to be sent into the specified room
     * @param type      type of message (See {@link MessageType})
     */
    protected void writeToPubsub(String roomName, String message, MessageType type) {
        if (!ready) throw new IllegalStateException("cannot send prior to handshake");

        try {
            long time = System.currentTimeMillis();
            long id = message.hashCode()+time;
            Message _message = new Message(id, time, type, false, account.getUserId(), message);
            String encPhrase = Encryption.encrypt(_message.toString(), aesKey, iv);
            ipfs.pubsub.pub(roomName, encPhrase);
        } catch (Exception e) { e.printStackTrace(); }
    }

    /**
     * Edits the content of an existing message.
     * @param oldMessage message that you will be editing
     * @param newContent the new message content
     */
    protected void editMessage(Message oldMessage, String newContent) {
        if (!ready) throw new IllegalStateException("cannot send prior to handshake");

        try {
            long time = System.currentTimeMillis();
            String[] message = new String[]{
                    String.valueOf(oldMessage.getMessageId()),
                    String.valueOf(time),
                    account.getFullUsername(),
                    newContent+MessageType.EDIT.getId()
            };
            String encPhrase = Encryption.encrypt(String.join("*", message), aesKey, iv);
            ipfs.pubsub.pub(roomName, encPhrase);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadMessages() {
        messages = FileLoader.loadMessages(roomName);
        for (Message m : messages)
            messageLookup.put(m.getMessageId(), m);
    }

    private void saveMessages() {
        if (saveMessage) {
            try { FileLoader.saveMessages(messages, roomName);
            } catch (IOException e) { e.printStackTrace(); }
        }
    }

    private void printLastMessages(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = Math.max(0, messages.size()-n); i < messages.size(); i++) {
            try {
                Message msg = messages.get(i);
                sb.append(msg.getMessageId()).append("  ").append(getTime(msg.getTimestampLong())).append("  ").append(account.getPeer(msg.getAuthorId()).getUsername()).append("  ").append(msg.getContent()).append("\n");
            } catch (Exception e) { e.printStackTrace(); }
        }
        System.out.println(sb.toString());
    }

    private boolean isJson(String s) {
        try { new JSONObject(s);
        } catch (JSONException ex) {
            try { new JSONArray(s);
            } catch (JSONException ex1) { return false; }
        } return true;
    }

    private void closeApp() {
        //TODO: save messages to file
        messages.clear();
        messageLookup.clear();
    }

    private void debug(String message) {
        if (DEBUG) System.out.println("[DEBUG] " + message);
    }
}
