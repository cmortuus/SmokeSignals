package com.Smoke.Signals;

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
//TODO find a way to do num mutual friends
//TODO build in emoji support
//TODO when getting ipfs get it by going to some normal site for china and then edit the html to add in the IPFS library and use that to pull down the code
//TODO make public SocialMedia pages
//TODO complete all the todo statements
class Invite {

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
    boolean shuttingDown;

    Invite(User yourself, String roomName) throws Exception {
        System.out.println("RoomName = " + roomName);
        this.yourself = yourself;
        account = yourself.getAccount();
        this.roomName = roomName;
        ipfs = new IPFS(new MultiAddress("/ip4/127.0.0.1/tcp/5001"));
        room = ipfs.pubsub.sub(roomName);
        connectedPeers = new HashMap<>();

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
        shuttingDown = false;
        doesEverything();
    }

    boolean isReady() {
        return ready;
    }

    /**
     * Threaded method that reads the messages out after they have been sent, unHashes them, decrypts them, and in the case of internal messages applies their message.
     * The handshake function was built into this one
     */
    private void doesEverything()  {
        new Thread(() -> {
            try {

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
                                System.out.println(createOutgoingRsaText());
                                ipfs.pubsub.pub(roomName, createOutgoingRsaText());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        if (!roomName.equals(account.getFullUsername())) {
                            shuttingDown = true;
                        }
                        if (shuttingDown && connectedPeers.isEmpty()) {
                            yourself.removeInviteRoom(roomName);
                            return;
                        }
                        try { Thread.sleep(10000);
                        } catch (InterruptedException ignore) {}
                    }
                }).start();

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
                            case SEND_INVITE: {
                                messages.add(message);
                                messageLookup.put(message.getMessageId(), message);
                                JSONObject jsonObject = new JSONObject(message.getContent());
                                String roomname = jsonObject.getString("roomName");
                                String userName = jsonObject.getString("userName");
                                boolean saveMessages = jsonObject.getBoolean("saveUsername");
                                int numMutualFriends = 0;
                                ArrayList<Peer> peers = new ArrayList<>();
                                for (Object p : jsonObject.getJSONArray("peer-array"))
                                    peers.add(new Peer(new JSONObject(p.toString())));
                                Collection<Peer> myPeers = yourself.getAccount().getPeers().values();
                                for(Peer peer : peers)
                                    for(Peer myPeer : myPeers)
                                        if(peer.equals(myPeer))
                                            numMutualFriends++;
                                System.out.println("You got an invite from "
                                        + userName
                                        + " with "
                                        + numMutualFriends
                                        + "mutual friends"
                                        + " to join roomnae: "
                                        + roomname);
//                                TODO Check if they want to join the room
//                                For now assume they want to join
                                User.addToRooms(roomName, new Pubsub(yourself, roomName, saveMessages));
                                break;
                            }

                            case IDENTITY_PACKET: {
                                if (isJson(message.getContent())) {
                                    Peer peer = account.getPeer(message.getAuthorId());
                                    JSONObject payload = new JSONObject(message.getContent());
                                    boolean save = !peer.getFullUsername().equals(payload.getString("username")+'#'+payload.getString("discriminator"));
                                    peer.updateUsername(payload.getString("username"));
                                    peer.updateDiscriminator(payload.getString("discriminator"));
                                    account.registerPeerId(payload.getString("peer-id"), peer);
                                    if (save) yourself.saveAccount();
                                    if (!connectedPeers.containsValue(peer)) {
                                        onUserConnect(peer);
                                    }
                                }
                                break;
                            }

                            case UNKNOWN: {
                                yourself.getLogger().logSecurity("Unknown message was sent: " + message.reportingToString());
                                break;
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void shakeHands(String sender, String message) {
        if (shuttingDown) return;
        if (!MyBase64.isBase64(message)) return;

        //TODO: verify that this is someone we actually want to perform a handshake with
        try { // stage 1

            /*
            Assume the message contains the public rsa key of a different peer.
            Use their public rsa key to encrypt your secret aes key and send it to the other peer.
             */

            main.debug("attempting handshake stage 1");
            PublicKey key = parseIncomingRsaText(message);
            if (key == null) throw new Exception();
            //TODO: implement check to ensure no suspicious accounts are listening in
            ipfs.pubsub.pub(roomName, createOutgoingAesText(key));
            //TODO create user object and add it to the list for the room and for user
//          if (++numUsersFound == users.size() && ipfs.pubsub.peers(roomName).toString().split(",").length <= users.size()) {
            main.debug("completed handshake stage 1");
        } catch (Exception ignore) { // stage 2

            /*
            Assume the message contains a secret aes key of a different peer encrypted with your public rsa key.
            Use their aes key to encrypt your aes key and send it to the other peer.
             */

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
            } catch (Exception ignore2) { // stage 3

                /*
                Assume the message contains a secret aes key of a different peer encrypted with your secret aes key.
                Store their secret aes key for later reference.
                 */

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
                } catch (Exception ignore3) {}
            }
        }
    }

    private void sendIdentityPacket() {
        JSONObject payload = new JSONObject();
        payload.put("username", account.getUsername())
                .put("discriminator", account.getDiscriminator())
                .put("peer-id", IPFSnonPubsub.ipfsID);
        writeToPubsub(payload.toString(), MessageType.IDENTITY_PACKET);
    }

    private void onUserConnect(Peer peer) {
        if (!connectedPeers.containsValue(peer))
            connectedPeers.put(peer.getUserId(), peer);
        if (this.getClass().equals(Pubsub.class))
            System.out.println("\n"+peer.getUsername()+" is now online\n");
    }

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
        messages.clear();
        messageLookup.clear();
    }

}