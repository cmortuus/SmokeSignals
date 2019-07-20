package com.Smoke.Signals;

import com.Smoke.Signals.account.Account;
import com.Smoke.Signals.account.Peer;
import io.ipfs.api.IPFS;
import io.ipfs.api.MerkleNode;
import io.ipfs.multiaddr.MultiAddress;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileOutputStream;
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
//TODO find a way to do num mutual friends
//TODO build in emoji support
//TODO complete all the todo statements
class Pubsub {
    User yourself;
    Account account;
    Stream<Map<String, Object>> room;
    String roomName;
    IPFS ipfs;

    // username and hash
    HashMap<String, String> users;
    ArrayList<OtherUser> usersInRoom;

    // messages
    boolean saveMessage;
    ArrayList<Message> messages;
    HashMap<Long, Message> messageLookup;

    // encryption
    SecretKey aesKey;
    String iv;
    PrivateKey privateKey;
    PublicKey publicKey;

    boolean ready;
    static final boolean DEBUG = true;

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

            ;
            ready = false;
        } catch (Exception e) {
            e.printStackTrace();
        }
        doesEverything();
    }

    /**
     * Sends a message to the connected chat.
     *
     * @param message text to be sent into the chat
     * @throws IllegalStateException if the method is called prior to connecting to the room
     */
    void sendMessage(String message) {
        if (!ready) throw new IllegalStateException("not connected to chat");
        writeToPubsub(message, MessageType.PUBLIC);
    }

    boolean isReady() {
        return ready;
    }

    /**
     * Threaded method that reads the messages out after they have been sent, unHashes them, decrypts them, and in the case of internal messages applies their message.
     * The handshake function was built into this one
     */
    void doesEverything() {
        new Thread(() -> {
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
                            } catch (IOException ignore) {
                            }
                        if (!ready)
                            try {
                                debug("attempting to start a handshake");
                                ipfs.pubsub.pub(roomName, createOutgoingRsaText());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        try {
                            Thread.sleep(10000);
                        } catch (InterruptedException ignore) {
                        }
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
                        String sender = stringObjectMap.toString().split(",", 2)[0].substring(6);

                        Pair<SecretKey, String> authorAesKey = yourself.getUserAesKey(sender);
                        if (authorAesKey == null) { // if we have not received an aes key from the author yet then perform a handshake
                            shakeHands(sender, decodedString);
                            return;
                        }

                        String decryptedMessage;
                        try {
                            decryptedMessage = Encryption.decrypt(decodedString, authorAesKey.getKey(), authorAesKey.getValue());
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
                                break;
                            }

                            case READ_RESPONSE: {
                                Message msg = messageLookup.get(message.getMessageId());
                                if (msg != null) msg.editSeen(true);
                                break;
                            }

                            case EDIT_MESSAGE: {
                                Message msg = messageLookup.get(message.getMessageId());
                                if (msg == null) return;
//                                String oldContent = msg.getContent();
                                if (msg.getAuthorId() == message.getAuthorId())
                                    msg.editContent(message.getContent());
                                else
                                    main.logging.logWarning(msg.getAuthorId() + " tried to edit a message that was not theirs");
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

                            case FILE: {
                                MerkleNode merkleNode = new MerkleNode(message.getContent());
                                try {
                                    if (merkleNode.name.isPresent())
                                        try (FileOutputStream fos = new FileOutputStream(merkleNode.name.get())) {
                                            fos.write(IPFSnonPubsub.getFile(merkleNode.hash));
                                        }
                                    else {
                                        int i = 0;
                                        while (!new File("Download" + i++).exists())
                                            System.out.print("");
                                        try (FileOutputStream fos = new FileOutputStream("download" + i)) {
                                            fos.write(IPFSnonPubsub.getFile(merkleNode.hash));
                                        }
                                    }
                                    IPFSnonPubsub.getFile(merkleNode.hash);
                                } catch (IOException e) {
                                    e.printStackTrace();
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
                                break;
                            }

                            case POST: {
                                messages.add(message);
                                messageLookup.put(message.getMessageId(), message);
                                Post post = new Post(message);
                                SocialMediaFeed.posts.put(post.getMessageId(), post);
                                saveMessages();
                                break;
                            }

                            case DELETE_POST: {
                                messages.add(message);
                                messageLookup.put(message.getMessageId(), message);
                                if (SocialMediaFeed.posts.get(Long.parseLong(message.getContent())).getAuthorId() == (message.getAuthorId()))
                                    SocialMediaFeed.posts.remove(Long.parseLong(message.getContent()));
                                else
                                    main.logging.logWarning(message.getAuthorId() + " tried to delete a message that was not theirs.");
                                break;
                            }

                            case COMMENT: {
                                messages.add(message);
                                messageLookup.put(message.getMessageId(), message);
                                Post postToAddCommentTo = SocialMediaFeed.posts.get(message.getMessageId());
                                postToAddCommentTo.addComment(new Post(message));
                            }

                            case DELETE_COMMENT: {
                                messages.add(message);
                                messageLookup.put(message.getMessageId(), message);
                                String[] splitContent = message.getContent().split("#");
                                Post post = SocialMediaFeed.posts.get(Long.parseLong(splitContent[0]));
                                if (message.getAuthorId() == post.getAuthorId())
                                    post.deleteComment(Long.parseLong(splitContent[1]));
                                else
                                    main.logging.logWarning(message.getAuthorId() + " tried to delete a comment that was not theirs");
                                break;
                            }

                            case EDIT_COMMENT: {
                                messages.add(message);
                                messageLookup.put(message.getMessageId(), message);
                                Post newComment = new Post(message);
                                Post post = SocialMediaFeed.posts.get(message.getMessageId());
                                if (message.getAuthorId() == post.getAuthorId())
                                    post.editComment(newComment.getPostContent(), newComment.getMessageId());
                                else
                                    main.logging.logWarning(message.getAuthorId() + " tried to edit a comment that was not theirs");
                                break;
                            }

                            case EDIT_COMMENT_WITH_IMAGE: {
                                messages.add(message);
                                messageLookup.put(message.getMessageId(), message);
                                Post newComment = new Post(message);
                                Post post = SocialMediaFeed.posts.get(message.getMessageId());
                                if (message.getAuthorId() == post.getAuthorId())
                                    post.editComment(newComment.getPostContent(), newComment.getMessageId(), newComment.getHashOfImage());
                                else
                                    main.logging.logWarning(message.getAuthorId() + " tried to edit a comment that was not theirs");
                                break;
                            }

                            case GET_PUBLIC_PAGE_NAME: {
                                messages.add(message);
                                messageLookup.put(message.getMessageId(), message);
                                for (String key : SocialMediaFeed.publicPages.keySet()) {
                                    if (key.equals(message.getContent())) {
                                        writeToPubsub(String.valueOf(message.getAuthorId()), String.valueOf(SocialMediaFeed.publicPages.get(key)), MessageType.RETURN_PUBLIC_PAGE_NAME);
                                    }
                                }
                            }

                            case RETURN_PUBLIC_PAGE_NAME: {
                                messages.add(message);
                                messageLookup.put(message.getMessageId(), message);
                                User.socialMediaFeed.followPublic(Long.parseLong(message.getContent()));
                            }

                            case TYPING: {
//                          TODO print in andoid that it is typing
                                System.out.println(message.getAuthorId() + " is typing...");
                                break;
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
        }).start();
    }

    void shakeHands(String sender, String message) {
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
                ipfs.pubsub.pub(roomName, Encryption.encrypt(MyBase64.encode(aesKey.getEncoded()) + "|" + iv, pair.getKey(), pair.getValue()));
                if (!Arrays.equals(pair.getKey().getEncoded(), aesKey.getEncoded())) // check if you are performing a handshake with yourself
                    ready = true;
                debug("completed handshake stage 2");
                writeToPubsub("", MessageType.UNKNOWN);
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
                    writeToPubsub("", MessageType.UNKNOWN);
                } catch (Exception ignore3) {
                }
            }
        }
    }

    /**
     * Converts properly formatted JSON into the {@link Message} object
     *
     * @param json JSON String containing the message info
     * @return {@link Message} object
     */
    Message parseMessage(String json) {
        if (!isJson(json)) throw new IllegalArgumentException("invalid JSON formatting");
        return new Message(new JSONObject(json));
    }

    /**
     * Turns the epoch time sent in the message to human readable time
     * At some point this will have to be related to current time. ie 20 min ago
     *
     * @param time The Epoch time that the message was sent at
     * @return The human readable time that the message was sent at
     */
    String getTime(String time) {
        return getTime(Long.parseLong(time));
    }

    /**
     * Turns the epoch time sent in the message to human readable time
     * At some point this will have to be related to current time. ie 20 min ago
     *
     * @param time The Epoch time that the message was sent at
     * @return The human readable time that the message was sent at
     */
    String getTime(long time) {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss");
        return sdf.format(new Date(time));
    }

    String createOutgoingRsaText() {
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
        String joined = MyBase64.encode(aesKey.getEncoded()) + "|" + iv;
        byte[] encrypted = Encryption.encryptWithRsa(joined, publicKey);
        return MyBase64.encode(encrypted);
    }

    /**
     * @param text the aes key that was recieved from another user
     * @return a pair containing the SecretKey and initVector or null if the contents could not be parsed out
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

    void sendFile(String filename) {
        writeToPubsub(IPFSnonPubsub.addFile(filename).hash.toString() + IPFSnonPubsub.fileKey, MessageType.FILE);
    }


    /**
     * Send a message into the room. [this method is for internal use]
     *
     * @param message text to be sent into the connected room
     * @param type    type of message (See {@link MessageType})
     */
    void writeToPubsub(String message, MessageType type) {
        if (!ready) throw new IllegalStateException("cannot send prior to handshake");

        try {
            long time = System.currentTimeMillis();
            long id = message.hashCode() + time;
            Message _message = new Message(id, time, type, false, account.getUserId(), message);
            String encPhrase = Encryption.encrypt(_message.toString(), aesKey, iv);
            ipfs.pubsub.pub(roomName, encPhrase);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Write the message to the specified room. [this method is for internal use]
     *
     * @param roomName the room to send the message to
     * @param message  text to be sent into the specified room
     * @param type     type of message (See {@link MessageType})
     */
    void writeToPubsub(String roomName, String message, MessageType type) {
        if (!ready) throw new IllegalStateException("cannot send prior to handshake");

        try {
            long time = System.currentTimeMillis();
            long id = message.hashCode() + time;
            Message _message = new Message(id, time, type, false, account.getUserId(), message);
            String encPhrase = Encryption.encrypt(_message.toString(), aesKey, iv);
            ipfs.pubsub.pub(roomName, encPhrase);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Edits the contents of an existing message
     *
     * @param messageID  the id of the message you are going to change
     * @param newContent what we are going to change the message to
     */
    void editMessage(long messageID, String newContent) {
        messageLookup.get(messageID).editContent(newContent);
    }

    private void loadMessages() {
        messages = FileLoader.loadMessages(roomName);
        Collections.sort(messages);
        for (Message m : messages)
            messageLookup.put(m.getMessageId(), m);
    }

    void saveMessages() {
        if (saveMessage) {
            try {
                FileLoader.saveMessages(messages, roomName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void printLastMessages(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = Math.max(0, messages.size() - n); i < messages.size(); i++) {
            try {
                Message msg = messages.get(i);
                sb.append(msg.getMessageId()).append("  ").append(getTime(msg.getTimestampLong())).append("  ").append(account.getPeer(msg.getAuthorId()).getUsername()).append("  ").append(msg.getContent()).append("\n");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println(sb.toString());
    }

    boolean isJson(String s) {
        try {
            new JSONObject(s);
        } catch (JSONException ex) {
            try {
                new JSONArray(s);
            } catch (JSONException ex1) {
                return false;
            }
        }
        return true;
    }

    void closeApp() {
        saveMessages();
        messages.clear();
        messageLookup.clear();
    }

    void debug(String message) {
        if (DEBUG) System.out.println("[DEBUG] " + message);
    }
}
