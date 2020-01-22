package com.Smoke.Signals;

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
//TODO when getting ipfs get it by going to some normal site for china and then edit the html to add in the IPFS library and use that to pull down the code
//TODO make public SocialMedia pages
//TODO complete all the todo statements

class Pubsub {

    private User yourself;
    private Account account;
    private Stream<Map<String, Object>> room;
    private String roomName;
    private IPFS ipfs;

    // username and hash
    private HashMap<Long, Peer> connectedPeers;

    // messages
    private boolean saveMessage;
    private ArrayList<Message> messages;
    private HashMap<Long, Message> messageLookup;

    // encryption
    private final SecretKey aesKey;
    private final String iv;
    private PrivateKey privateKey;
    private PublicKey publicKey;
    private HashMap<String, Pair<SecretKey, String>> secretKeys;

    boolean ready;

    Pubsub(User yourself, String roomName, boolean saveMessage) throws Exception {
        System.out.println("RoomName = " + roomName);
        this.yourself = yourself;
        account = yourself.getAccount();
        this.roomName = roomName;
        ipfs = new IPFS(new MultiAddress("/ip4/127.0.0.1/tcp/5001"));
        room = ipfs.pubsub.sub(roomName);
        connectedPeers = new HashMap<>();

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
        secretKeys = new HashMap<>();

        ready = false;
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
    private void doesEverything() {
        new Thread(() -> {
            try {
                if(saveMessage){
                    loadMessages();
                    printLastMessages(20);
                }
                // initiate handshake
                new Thread(() -> {
                    List<String> lastPeers = new ArrayList<>();
                    while (true) {
                        try {
                            List<String> currentPeers = ((LinkedHashMap<String, ArrayList<String>>)ipfs.pubsub.peers(roomName)).get("Strings");
                            if (!lastPeers.containsAll(currentPeers) || !currentPeers.containsAll(lastPeers)) {
                                List<String> leaves = new ArrayList<>(lastPeers);
                                leaves.removeAll(currentPeers);
                                for (String id : leaves) {
                                    Peer p = account.getPeer(id);
                                    if (p != null) onUserDisconnect(p);
                                }
                                lastPeers = currentPeers;
                                main.debug("peers: " + currentPeers);
                            }
                        } catch (IOException ignore) {}
                        if (!ready)
                            try {
                                main.debug("attempting to start a handshake");
                                ipfs.pubsub.pub(roomName, createOutgoingRsaText());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        try { Thread.sleep(10000);
                        } catch (InterruptedException ignore) {}
                    }
                }).start();

                ipfs.config.set("Pubsub.Router", "gossipsub");

                // write out each line of the stream to a file and check if they are one of the users
                room.forEach(stringObjectMap -> {
                    if (stringObjectMap.isEmpty()) return;
                    try {

                        byte[] decodedBytes = MyBase64.decode((String) stringObjectMap.get("data"));
                        String decodedString = new String(decodedBytes).replaceAll(" ", "+");
                        String sender = (String) stringObjectMap.get("from");

                        Pair<SecretKey, String> authorAesKey = secretKeys.get(sender);
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

                        //TODO: better message save stuff
                        //TODO: decide if the message should be stored in messages

                        // process the message according to its type
                        switch (message.getMessageType()) {
                            case PUBLIC: {
                                messages.add(message);
                                messageLookup.put(message.getMessageId(), message);
                                System.out.println(
                                        getTime(message.getTimestampLong()) + "  " +
                                        account.getPeer(message.getAuthorId()).getUsername() + "  " +
                                        message.getContent());
                                writeToPubsub(String.valueOf(message.getMessageId()), MessageType.RECEIVED);
                                break;
                            }

                            case READ_RESPONSE: {
                                messages.add(message);
                                messageLookup.put(message.getMessageId(), message);
                                Message msg = messageLookup.get(message.getMessageId());
                                if (msg != null) msg.editSeen(true);
                                break;
                            }

                            case EDIT_MESSAGE: {
                                messages.add(message);
                                messageLookup.put(message.getMessageId(), message);
                                Message msg = messageLookup.get(message.getMessageId());
                                if (msg == null) return;
//                                String oldContent = msg.getContent();
                                if (msg.getAuthorId() == message.getAuthorId())
                                    msg.editContent(message.getContent());
                                else
                                    yourself.getLogger().logWarning(msg.getAuthorId() + " tried to edit a message that was not theirs");
//                                String response = "<type=edit messageId="+msg.getMessageId()+" oldContent=\""+oldContent+"\" newContent=\""+msg.getContent()+"\">";
//                                sendMessage(response);
                                break;
                            }

                            case FILE: {
                                messages.add(message);
                                messageLookup.put(message.getMessageId(), message);
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
                                break;
                            }

                            case IDENTITY_PACKET: {
                                if (isJson(message.getContent())) {
                                    Peer peer = account.getPeer(message.getAuthorId());
                                    JSONObject payload = new JSONObject(message.getContent());
                                    boolean save = !peer.getFullUsername().equals(payload.getString("username")+'#'+payload.getString("discriminator"));
                                    peer.updateUsername(payload.getString("username"));
                                    peer.updateDiscriminator(payload.getString("discriminator"));
                                    peer.updateSocialMediaRoom(payload.getString("socialMediaRoom"));
                                    account.registerPeerId(payload.getString("peer-id"), peer);
                                    if (save) yourself.saveAccount();
                                    if (!connectedPeers.containsValue(peer)) {
                                        onUserConnect(peer);
                                    }
                                }
                                break;
                            }

                            case LOG_REQUEST: {
                                if (!isJson(message.getContent())) break;
                                JSONObject packet = new JSONObject(message.getContent());
                                if (!packet.has("ignore-if-absent") || !packet.has("data")) break;
                                boolean ignoreIfAbsent = packet.getBoolean("ignore-if-absent");
                                JSONObject data = packet.getJSONObject("data");
                                Map<String, Object> map = data.toMap();
                                if (!map.keySet().contains(String.valueOf(account.getUserId())) && !ignoreIfAbsent) {
                                    sendLogPacket(0, message.getAuthorId());
                                    break;
                                }
                                for (Map.Entry<String, Object> entry : map.entrySet()) {
                                    try {
                                        if (Long.parseLong(entry.getKey()) == account.getUserId()) {
                                            sendLogPacket((long) entry.getValue(), message.getAuthorId());
                                            break;
                                        }
                                    } catch (NumberFormatException nfe) {
                                        nfe.printStackTrace();
                                    }
                                }
                                break;
                            }

                            case LOG_PACKET: {
                                if (!isJson(message.getContent())) break;
                                JSONObject json = new JSONObject(message.getContent());
                                if (!json.has("caller") || !json.has("logs")) break;
                                if (json.getLong("caller") != account.getUserId()) break;
                                for (Object o : json.getJSONArray("logs")) {
                                    Message msg = new Message(new JSONObject(o.toString()));
                                    if (!messageLookup.containsKey(msg.getMessageId())) {
                                        messageLookup.put(msg.getMessageId(), msg);
                                        messages.add(msg);
                                    }
                                }
                                Collections.sort(messages);
                                saveMessages();
                                break;
                            }

                            case RECEIVED: {
                                messages.add(message);
                                messageLookup.put(message.getMessageId(), message);
                                if (messageLookup.containsKey(Long.valueOf(message.getContent())))
                                    if (messageLookup.get(Long.valueOf(message.getContent())).getAuthorId() == yourself.getAccount().getUserId())
                                        messageLookup.get(Long.valueOf(message.getContent())).setReceivedTrue(message.getAuthorId());
                                saveMessages();
                                break;
                            }

                            case IS_ONLINE: {
                                messages.add(message);
                                messageLookup.put(message.getMessageId(), message);
                                Peer peer = connectedPeers.get(message.getAuthorId());
                                peer.setLastTimeOnline(message.getTimestampLong());
                                peer.setOnline(false);
                                saveMessages();
                                break;
                            }

                            case IS_OFFLINE: {
                                messages.add(message);
                                messageLookup.put(message.getMessageId(), message);
                                Peer peer = connectedPeers.get(message.getAuthorId());
                                peer.setLastTimeOnline(message.getTimestampLong());
                                peer.setOnline(true);
                                saveMessages();
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
                                    yourself.getLogger().logSecurity("Tried to delete a message that was not theirs: " + message.reportingToString());
                                saveMessages();
                                break;
                            }

                            case COMMENT: {
                                messages.add(message);
                                messageLookup.put(message.getMessageId(), message);
                                Post postToAddCommentTo = SocialMediaFeed.posts.get(message.getMessageId());
                                postToAddCommentTo.addComment(new Post(message));
                                saveMessages();
                                break;
                            }

                            case DELETE_COMMENT: {
                                messages.add(message);
                                messageLookup.put(message.getMessageId(), message);
                                String[] splitContent = message.getContent().split("#");
                                Post post = SocialMediaFeed.posts.get(Long.parseLong(splitContent[0]));
                                if (message.getAuthorId() == post.getAuthorId())
                                    post.deleteComment(Long.parseLong(splitContent[1]));
                                else
                                    yourself.getLogger().logSecurity("Tried to delete a comment that was not theirs: " + message.reportingToString());
                                saveMessages();
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
                                    yourself.getLogger().logSecurity("Tried to edit a comment that was not theirs: " + message.reportingToString());
                                saveMessages();
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
                                    yourself.getLogger().logWarning(message.getAuthorId() + " tried to edit a comment that was not theirs");
                                saveMessages();
                                break;
                            }

                            case TYPING: {
                                System.out.println(message.getAuthorId() + " is typing...");
                                break;
                            }

                            case UNKNOWN: {
                                yourself.getLogger().logSecurity("Unknown message was sent: " + message.reportingToString());
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

    private void shakeHands(String sender, String message) {
        if (!MyBase64.isBase64(message)) return;
        try { // stage 1

            // Assume the message contains the public rsa key of a different peer.
            // Use their public rsa key to encrypt your secret aes key and send it to the other peer.
            main.debug("attempting handshake stage 1");
            PublicKey key = parseIncomingRsaText(message);
            if (key == null) throw new Exception();
            ipfs.pubsub.pub(roomName, createOutgoingAesText(key));
            main.debug("completed handshake stage 1");
        } catch (Exception ignore) { // stage 2

            // Assume the message contains a secret aes key of a different peer encrypted with your public rsa key.
            // Use their aes key to encrypt your aes key and send it to the other peer.
            main.debug("attempting handshake stage 2");
            try {
                Pair<SecretKey, String> pair = parseIncomingAesKey(message);
                if (pair == null) return;
                if (pair.getKey() == null) throw new Exception();
                secretKeys.put(sender, pair);
                ipfs.pubsub.pub(roomName, Encryption.encrypt(MyBase64.encode(aesKey.getEncoded()) + "|" + iv, pair.getKey(), pair.getValue()));
                if (!Arrays.equals(pair.getKey().getEncoded(), aesKey.getEncoded())) // check if you are performing a handshake with yourself
                    ready = true;
                main.debug("completed handshake stage 2");
                sendIdentityPacket();
                sendLogRequestPacket();
            } catch (Exception ignore2) { // stage 3

                // Assume the message contains a secret aes key of a different peer encrypted with your secret aes key.
                // Store their secret aes key for later reference.
                main.debug("attempting handshake stage 3");
                try {
                    String decrypted = Encryption.decrypt(message, aesKey, iv);
                    String[] joined = decrypted.split("\\|");
                    if (joined.length != 2 || !MyBase64.isBase64(joined[0])) return;
                    byte[] bytes = MyBase64.decode(joined[0]);
                    SecretKey key = new SecretKeySpec(bytes, "AES");
                    secretKeys.put(sender, new Pair<>(key, joined[1]));
                    ready = true;
                    main.debug("completed handshake stage 3");
                    sendIdentityPacket();
                    sendLogRequestPacket();
                } catch (Exception ignore3) {}
            }
        }
    }

    private void sendLogRequestPacket() {
        ArrayList<Long> knownIds = new ArrayList<>();
        Map<Long, Long> lastSynced = new HashMap<>();
        for (Peer peer : account.getPeers().values()) {
            if (peer.getJoinedRooms().contains(roomName))
                knownIds.add(peer.getUserId());
        }
        for (int i=messages.size()-1; i>=0; i--) {
            Message msg = messages.get(i);
            if (knownIds.contains(msg.getAuthorId())) {
                lastSynced.put(msg.getAuthorId(), msg.getTimestampLong());
                knownIds.remove(msg.getAuthorId());
            }
            if (knownIds.isEmpty()) break;
        }
        JSONObject packet = new JSONObject();
        packet.put("ignore-if-absent", false);
        packet.put("data", new JSONObject(lastSynced));
        writeToPubsub(packet.toString(), MessageType.LOG_REQUEST);
    }

    private void sendLogPacket(long timestamp, long callerId) {
        ArrayList<Message> logs = new ArrayList<>();
        for (int i=messages.size()-1; i>=0; i--) {
            Message msg = messages.get(i);
            if (msg.getTimestampLong() < timestamp) break;
            logs.add(msg);
        }
        JSONArray jsonLogs = new JSONArray();
        for (Message l : logs) jsonLogs.put(l.toJSONObject());
        writeToPubsub(new JSONObject().put("caller", callerId).put("logs", jsonLogs).toString(), MessageType.LOG_PACKET);
    }

    private void sendIdentityPacket() {
        JSONObject payload = new JSONObject();
        payload.put("username", account.getUsername())
                .put("discriminator", account.getDiscriminator())
                .put("peer-id", IPFSnonPubsub.ipfsID)
                .put("socialMediaRoom", Account.getSocialMediaRoomName());
        writeToPubsub(payload.toString(), MessageType.IDENTITY_PACKET);
    }

    /**
     * Called when a user joins the room and completes the handshake
     * @param peer the peer that connected
     */
    private void onUserConnect(Peer peer) {
        peer.addJoinedRoom(roomName);
        if (!connectedPeers.containsValue(peer))
            connectedPeers.put(peer.getUserId(), peer);
        if (this.getClass().equals(Pubsub.class))
            System.out.println("\n"+peer.getUsername()+" is now online\n");
    }

    /**
     * Called when a user leaves the room
     * @param peer the peer that left
     */
    private void onUserDisconnect(Peer peer) {
        connectedPeers.remove(peer.getUserId());
        if (this.getClass().equals(Pubsub.class))
            System.out.println("\n"+peer.getUsername()+" is now offline\n");
    }

    /**
     * Converts properly formatted JSON into the {@link Message} object
     *
     * @param json JSON String containing the message info
     * @return {@link Message} object
     */
    private Message parseMessage(String json) {
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

    private void sendFile(String filename) {
        try {
            writeToPubsub(IPFSnonPubsub.addFile(filename).hash.toString() + IPFSnonPubsub.fileKey, MessageType.FILE);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
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

    private void loadMessages() {
        messages = FileLoader.loadMessages(roomName);
        Collections.sort(messages);
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
        for (int i = Math.max(0, messages.size() - n); i < messages.size(); i++) {
            try {
                Message msg = messages.get(i);
                if (msg.getMessageType().equals(MessageType.PUBLIC))
                sb.append(msg.getMessageId()).append("  ").append(getTime(msg.getTimestampLong())).append("  ").append(account.getPeer(msg.getAuthorId()).getUsername()).append("  ").append(msg.getContent()).append("\n");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println(sb.toString());
    }

    private boolean isJson(String s) {
        try { new JSONObject(s);
        } catch (JSONException ex) {
            try { new JSONArray(s);
            } catch (JSONException ex1) {
                return false;
            }
        }
        return true;
    }

    private void closeApp() {
        writeToPubsub("", MessageType.IS_OFFLINE);
        saveMessages();
        messages.clear();
        messageLookup.clear();
    }

}