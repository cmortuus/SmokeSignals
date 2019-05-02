import io.ipfs.api.IPFS;
import io.ipfs.multiaddr.MultiAddress;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Stream;

//TODO Make it so that you can edit texts after you send them
//  TODO send message that ends in a 0. This message will include a time stamp and the hashcode of the message. Each message will have to be stored in the hashmap with a timestamp
//  TODO give each message a hash and reference it by the hash of the message. Every message sent might have a hash that I just need to find
//  TODO use this to edit the message, maybe keep a history maybe don't
//TODO when making the social media side of it allow people to add people to chats, but not add them to the social media feed
//TODO add handshake for seen messages and write the messages that the user sends to the log file
//TODO put a handshake in on each message so that if somebody misses a message than they all fill in the gaps
//TODO Have them keep trying to decrypt the aes keys until they have everyone in the chat and then if one of the keys does not work throw an error to have everyone remake and resend the keys
//TODO add voice features in as well
//TODO make it so that it dynamically chooses to use one aes key per person or per room depending on number of people in room. If you have an aes key for each person you have to send a message for each person.
//TODO make it so you can delete a person from a chatroom if they delete the app
//TODO make sure that people can see the message when it is not writing to a file
//TODO send message when you go online and then when the app closes send message that you are offline when you come online everyone tells you if they are online everything else is assumed offline
//TODO write method to pull messages from the file after app has been closed
//TODO implament parental controls
//TODO work on socaial media part of it
//TODO when a comment is added resend the post and have that overwrite the orraginal post in people's timeline
//TODO figure out how to do *Eco is typing* Probably just send a message ahead of time
//TODO find a way to do num mutual friends
//TODO build in emoji support
public class Pubsub implements Runnable {

    private User yourself;
    private boolean saveMessage;
    private Stream<Map<String, Object>> room;
    private String roomName;
    private IPFS ipfs;
    //    Username and hash
    private HashMap<String, String> users;
    private PrivateKey privateKey;
    private PublicKey publicKey;
    private SecretKey aesKey;
    private int numUsersFound;
    private ArrayList<Message> messages;
    private ArrayList<OtherUser> usersInRoom;
    private boolean ready;

    Pubsub(User yourself, String roomName, Boolean saveMessage) {
        System.out.println("RoomName = " + roomName);
        try {
            this.yourself = yourself;
            this.saveMessage = saveMessage;
            users = new HashMap<>();
            this.roomName = roomName;
            ipfs = new IPFS(new MultiAddress("/ip4/127.0.0.1/tcp/5001"));
            room = ipfs.pubsub.sub(roomName);
//          Encryption keys
            KeyPair keypair = Encryption.generateKeys();
            publicKey = keypair.getPublic();
            privateKey = keypair.getPrivate();
            aesKey = Encryption.generateAESkey();
            messages = new ArrayList<>();
            usersInRoom = new ArrayList<>();
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

    /**
     * Threaded method that reads the messages out after they have been sent, unHashes them, decrypts them, and in the case of internal messages applies their message.
     * The handshake function was built into this one
     */
    @Override
    public void run() {
//        Needs two try catches because the try statement that buffered writer is in does not account for the IOException that FileWriter will throw

        try {
            File file = new File(roomName);
            if(!file.exists()) file.createNewFile();
            try (FileWriter fw = new FileWriter(file, true)) {

                // initialize
                try { ipfs.pubsub.pub(roomName, createOutgoingRsaText());
                } catch (Exception e) { e.printStackTrace(); }

                // write out each line of the stream to a file and check if they are one of the users
                room.forEach(stringObjectMap -> {
                    try {

                        //TODO: do something with this
//                        Thread sendUsername = new Thread(() -> {
//                            try {
//                                while (numUsersFound < users.size()) {
//                                    writeToPubsub("username", MessageType.UNKNOWN);
//                                    Thread.sleep(100);
//                                }
//                            } catch (InterruptedException e) {
//                                e.printStackTrace();
//                            }
//                        });
//                        sendUsername.run();

                        String base64Data = stringObjectMap.values().toString().split(",")[1].trim();
                        byte[] decodedBytes = Base64.getDecoder().decode(base64Data);
//                        For some reason it removes the +s and adds spaces which throw errors because that is not a thing in an aes string
                        String decodedString = new String(decodedBytes).replaceAll(" ", "+");

                        //TODO: is there a better way to differentiate the sender?
                        String sender = stringObjectMap.toString().split(",",2)[0].substring(6);

                        SecretKey authorAesKey = yourself.getUserAesKey(sender);
                        if (authorAesKey == null) { // if we have not received an aes key from the author yet then perform a handshake
                            if (!isBase64(decodedString)) return;
                            //TODO: verify that this is someone we actually want to perform a handshake with
                            try { // stage 1
                                System.out.println("[DEBUG] attempting handshake stage 1");
                                PublicKey key = parseIncomingRsaText(decodedString);
                                if (key == null) throw new Exception();
                                //TODO: implement check to ensure no suspicious accounts are listening in
                                ipfs.pubsub.pub(roomName, createOutgoingAesText(key));
//                                    //TODO create user object and add it to the list for the room and for user
//                                    if (++numUsersFound == users.size() && ipfs.pubsub.peers(roomName).toString().split(",").length <= users.size()) {
                                System.out.println("[DEBUG] completed handshake stage 1");
                            } catch (Exception ignore) { // stage 2
                                System.out.println("[DEBUG] attempting handshake stage 2");
                                try {
                                    SecretKey key = parseIncomingAesKey(decodedString);
                                    if (key == null) throw new Exception();
                                    yourself.addSecretKey(sender, key);
                                    ipfs.pubsub.pub(roomName, Encryption.encrypt(Base64.getEncoder().encodeToString(aesKey.getEncoded()), key));
                                    ready = true;
                                    System.out.println("[DEBUG] completed handshake stage 2");
                                } catch (Exception ignore2) { // stage 3
                                    System.out.println("[DEBUG] attempting handshake stage 3");
                                    try {
                                        String decrypted = Encryption.decrypt(decodedString, aesKey);
                                        if (decrypted == null || !isBase64(decrypted)) return;
                                        byte[] bytes = Base64.getDecoder().decode(decrypted);
                                        SecretKey key = new SecretKeySpec(bytes, 0, bytes.length, "AES");
                                        yourself.addSecretKey(sender, key);
                                        System.out.println("[DEBUG] completed handshake stage 3");
                                    } catch (Exception ignore3) {}
                                }
                            }

                            return;
                        }

                        String decryptedMessage = Encryption.decrypt(decodedString, authorAesKey);
                        if (decryptedMessage == null) return; // abort if the message could not be decrypted
                        String[] unparsedMessage = decryptedMessage.split("\\*", 4);
                        if (unparsedMessage.length != 4) return; // abort if the message is not formatted correctly

                        String stringyMessage = unparsedMessage[0]+","+
                                getTime(unparsedMessage[1])+","+
                                unparsedMessage[2].split("#",3)[0]+","+
                                unparsedMessage[3].substring(0, unparsedMessage[3].length()-1);
                        System.out.println(String.join("  ", stringyMessage.split(",", 4)));

                        Message message = addMessage(unparsedMessage);

                        // process the message according to its type
                        switch (message.getMessageType()) {
                            case PUBLIC:
                                fw.write(stringyMessage + "\n");
                                fw.flush();
                                break;

                            case READ_RESPONSE:
                                if (!decryptedMessage.equals(IPFSnonPubsub.ipfsID)) break;
                                setAsSeen(Long.parseLong(unparsedMessage[1]));
                                break;

                            case COMMENT:
                                SocialMediaFeed.posts.put(Long.parseLong(message.getAuthor().split("#")[0]), new Post(unparsedMessage));
                                break;

                            case POST:
                                //TODO: handle receiving a social media post
                                break;

                            case UNKNOWN:
                                //TODO: handle any unknown messages
                                break;
                        }

                    } catch (IOException | NullPointerException e) {
                        e.printStackTrace();
                    }
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Adds message to memory where we are temp storing it. Sending it to file is elsewhere
     *
     * @param decryptedMessage Array of all the parts of a decrypted message. index 0 = message id, 1 = timestamp, 2 = username, 3 = message content
     * @return created message
     */
    private Message addMessage(String[] decryptedMessage) {
        if (decryptedMessage.length != 4)
            throw new IllegalArgumentException("decryptedMessage is length " + decryptedMessage.length + " when it should be length 4");

        long messageId, timestamp;
        MessageType type;
        String username = decryptedMessage[2];
        String content = decryptedMessage[3];
        // strip trailing identifier from content
        content = content.substring(0, content.length() - 1);

        try {
            messageId = Long.parseLong(decryptedMessage[0]);
        } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException("cannot parse message id (long) from \"" + decryptedMessage[0] + "\"");
        }
        try {
            timestamp = Long.parseLong(decryptedMessage[1]);
        } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException("cannot parse timestamp long from \"" + decryptedMessage[1] + "\"");
        }
        try {
            type = MessageType.values()[Integer.parseInt(decryptedMessage[3].substring(decryptedMessage[3].length()-1))];
        } catch (NumberFormatException nfe) {
            type = MessageType.UNKNOWN;
        }
        Message message = new Message(messageId, timestamp, username, content, type,false);
        messages.add(message);
        return message;
    }

    /**
     * Turns the epoch time sent in the message to human readable time
     * At some point this will have to be related to current time. ie 20 min ago
     *
     * @param time The Epoch time that the message was sent at
     * @return The human readable time that the message was sent at
     */
    private String getTime(String time) {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss");
        return sdf.format(new Date(Long.parseLong(time)));
    }

    /**
     * Encrypt the aes key and then send it to each person's private room individually to reduce clutter on massive servers when someone new joins
     * Also if the it fails to encrypt the rsa key than run the method again after recreating the aes keys
     */
    @Deprecated
    private void sendAESkeyEnc() {
        try {
            String aesEnc = new String(Encryption.encryptAESwithRSA(publicKey, aesKey));
            writeToPubsub(aesEnc, MessageType.UNKNOWN);
//            If the rsa keys fail it just recreates the keys and tries to send the aes keys again
        } catch (IllegalStateException e) {
            try {
                e.printStackTrace();
                KeyPair keypair = Encryption.generateKeys();
                publicKey = keypair.getPublic();
                privateKey = keypair.getPrivate();
                sendAESkeyEnc();
            } catch (NoSuchAlgorithmException err) {
                err.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String createOutgoingRsaText() {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }
    private PublicKey parseIncomingRsaText(String text) throws NoSuchAlgorithmException, InvalidKeySpecException {
        if (!isBase64(text)) return null;
        byte[] key = Base64.getDecoder().decode(text);
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(key));
    }
    private String createOutgoingAesText(PublicKey publicKey) throws BadPaddingException, InvalidKeyException, IllegalBlockSizeException {
        Objects.requireNonNull(publicKey, "public key cannot be null");
        byte[] key = aesKey.getEncoded();
        byte[] encryptedKey = Encryption.encryptWithRsa(Base64.getEncoder().encodeToString(key), publicKey);
        return Base64.getEncoder().encodeToString(encryptedKey);
    }
    private SecretKey parseIncomingAesKey(String text) throws BadPaddingException, InvalidKeyException, IllegalBlockSizeException {
        if (!isBase64(text)) return null;
        byte[] bytes = Base64.getDecoder().decode(text);
        String decryptedKey = Encryption.decryptWithRsa(bytes, privateKey);
        byte[] key = Base64.getDecoder().decode(decryptedKey);
        return new SecretKeySpec(key, 0, key.length, "AES");
    }

    /**
     * Send the public rsa key to the chat
     */
    @Deprecated
    private void sendRSAkey() {
        try {
            for (String user : users.keySet()) {
                if (ipfs.pubsub.peers(user).toString().split(",").length != users.size())
                    writeToPubsub(String.valueOf(publicKey), MessageType.UNKNOWN);
                else
                    throw new SecurityException("Someone else is in the chat while we are trying to send the rsa key to their private chat");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Send a message into the room. [this method is for internal use]
     *
     * @param phrase    text to be sent into the connected room
     * @param type      type of message (See {@link MessageType})
     */
    protected void writeToPubsub(String phrase, MessageType type) {
        // ensure a handshake has already occurred before sending
        if (!ready) throw new IllegalStateException("cannot send prior to handshake");

        try {
            long time = System.currentTimeMillis();
            String[] message = new String[]{
                    String.valueOf(phrase.hashCode()+time),
                    String.valueOf(time),
                    yourself.getUserName(),
                    phrase+type.getId()
            };
            String encPhrase = Encryption.encrypt(String.join("*", message), aesKey);
            ipfs.pubsub.pub(roomName, encPhrase);
        } catch (Exception e) { e.printStackTrace(); }
    }

    /**
     * Write the message to the specified room. [this method is for internal use]
     *
     * @param roomName  the room to send the message to
     * @param phrase    text to be sent into the specified room
     * @param type      type of message (See {@link MessageType})
     */
    protected void writeToPubsub(String roomName, String phrase, MessageType type) {
        // ensure a handshake has already occurred before sending
        if (!ready) throw new IllegalStateException("cannot send prior to handshake");

        try {
            long time = System.currentTimeMillis();
            String encPhrase = Encryption.encrypt(phrase.hashCode() + time + "*" + System.currentTimeMillis() + "*" + yourself.getUserName() + "*" + phrase + type.getId(), aesKey);
//            It breaks if you take this out
//            Encryption.decrypt(encPhrase, aesKey);
            ipfs.pubsub.pub(roomName, encPhrase);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Marks the specified message as read
     *
     * @param messageId message id of the message to be marked as read
     */
    private void setAsSeen(long messageId) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (messages.get(i).getMessageId() == messageId) {
                messages.get(i).editSeen(true);
            }
        }
    }

    private boolean isBase64(String s) {
        return s.matches("^([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)?$");
    }

    private void closeApp() {
        messages.clear();
    }
}
