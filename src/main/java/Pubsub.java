import io.ipfs.api.IPFS;
import io.ipfs.multiaddr.MultiAddress;
import io.ipfs.multihash.Multihash;

import javax.crypto.SecretKey;
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
//  TODO give each message a hash and refference it by the hash of the message. Every message sent muight havea  hash that I just need to find
//  TODO use this to edit the message, maybe keep a history maybe dont
//TODO when making the socaial media side of it allow people to add people to chats, but not add them to the social media feed
//TODO add handshake for seen messages and write the messages that the user sends to the log file
//TODO put a handshake in on each message so that if somebody misses a message than they all fill in the gaps
//TODO Have them keep trying to decrypt the aes keys until they have everyone in the chat and then if one of the keys does not work throw an error to have everyone remake and resend the keys
//TODO add voice features in as well
//TODO make it so that it dyncmicly choses to use one aes key per person or per room depending on number of people in room. If you have an aes key for each person you have to send a message for each person.
//TODO make it so you can delete a person from a chatroom if they delete the app
//TODO make sure that people can see the message when it is not writing to a file
//TODO send message when you go online and then when the app closes send message that you are offline when you come online everyone tells you if they are online everything else is assumed offline
//TODO write method to pull messages from the file after app has been closed
public class Pubsub implements Runnable {
    private boolean saveMessage;
    private Stream<Map<String, Object>> room;
    private String roomName;
    private IPFS ipfs;
    //    Username and hash
    private HashMap<String, String> users;
    private ArrayList<PublicKey> publicKeys;
    private ArrayList<SecretKey> secretKeys;
    private PrivateKey privateKey;
    private PublicKey publicKey;
    private SecretKey aesKey;
    private int numUsersFound;
    //          Time ID       Username        Message isRead
    HashMap<Long, HashMap<String, HashMap<String, Boolean>>> messages;

    Pubsub(String roomName, Boolean saveMessage) {
        try {
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
            messages = new HashMap<>();
            secretKeys = new ArrayList<>();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Threaded method that reads the messages out after they have been sent, unHashes them, decrypts them, and in the case of internal messages applies their message.
     * The handshake function was built into this one
     */
    @Override
    public void run() {
//        Needs two try catches because the tey statment that buffered writer is in does not account for the IOExecption that FileWriter will throw
        try {
            File file = new File(roomName);
            if (!file.exists() && saveMessage)
                file.createNewFile();
            try (FileWriter fw = new FileWriter(file, true)) {
//                Write out each line of the stream to a file and check if they are one of the users
                room.forEach(stringObjectMap -> {
                    try {
                        String data = stringObjectMap.values().toString().split(",")[1].trim();
                        Thread sendUsername = new Thread(() -> {
                            try {
                                while (numUsersFound < users.size()) {
                                    writeToPubsub("username", 4);
                                    Thread.sleep(1000);
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        });
                        sendUsername.run();
                        byte[] decodedBytes = Base64.getDecoder().decode(data);
//                        For some reason it removes the +s and adds spaces which throw errors because that is not a thing in an aes string
                        String decodedString = new String(decodedBytes).replaceAll(" ", "+");
                        String decryptedMessage = Encryption.decrypt(decodedString, aesKey);
                        String[] timeAndMessage = decryptedMessage.split("\\*", 3);

                        StringBuilder sb = new StringBuilder();
//                        Print out message. For some reason when you do it in one print statment it hangs with no error message
                        for (int i = 0; i < timeAndMessage.length; i++) {
                            if (i == 0) {
                                sb.append(getTime(timeAndMessage[0]));
                                sb.append(",");
                            } else if (i == 2) {
                                sb.append(timeAndMessage[i], 0, timeAndMessage[2].length() - 1);
                                sb.append(",");
                            } else {
                                sb.append(timeAndMessage[i].split("#")[0]);
                                sb.append(",");
                            }
                        }
                        System.out.println(sb.toString().replaceAll(",", "  "));
                        addMessage(timeAndMessage);
                        String ipfsID = ipfs.refs.local().toArray()[1].toString();
                        addMessage(timeAndMessage);
                        if (decryptedMessage.endsWith("0") && saveMessage) {
                            fw.write(sb.toString() + "\n");
                            fw.flush();
                        } else if (timeAndMessage[2].endsWith("1") && decryptedMessage.equals(ipfsID.trim())) {
                            setAsSeen(Long.parseLong(timeAndMessage[0]), users.get(timeAndMessage[1]));
                        } else if (timeAndMessage[2].endsWith("2")) {
                            try {
                                byte[] key = IPFSnonPubsub.getFile(new Multihash(data.getBytes()));
                                PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(key));
                                publicKeys.add(publicKey);
                                if (++numUsersFound == users.size() && ipfs.pubsub.peers(roomName).toString().split(",").length <= users.size()) {
                                    sendRSAkey();
                                    sendAESkeyEnc();
                                }
                            } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                                e.printStackTrace();
                            }
                        }
                    } catch (IOException | NullPointerException e) {
                        e.printStackTrace();
                    }
                });
            }
        } catch (
                IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Adds message to memory where we are temp storing it. Sending it to file is elsewhere
     *
     * @param decryptedMessage Array of all the parts of a decrypted message. 0 is time stamp. 1 is username. 2 is text.
     */
    private void addMessage(String[] decryptedMessage) {
        HashMap<String, Boolean> messageAndSeen = new HashMap<>();
        messageAndSeen.put((decryptedMessage[2].substring(0, decryptedMessage[2].length() - 1)), false);
        HashMap<String, HashMap<String, Boolean>> outerHashmap = new HashMap<>();
        outerHashmap.put(users.get(decryptedMessage[0]), messageAndSeen);
        messages.put(Long.parseLong(decryptedMessage[0]), outerHashmap);
    }

    /**
     * Turns the epoc time sent in the message to human readable time
     * At some point this will have to be related to current time. ie 20 min ago
     *
     * @param time The Epoc time that the message was sent at
     * @return The human readable time that the message was sent at
     */
    private String getTime(String time) {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss");
        return sdf.format(new Date(Long.parseLong(time)));
    }

    /**
     * Encrypt the aes key and then send it to each person's private room individually to reduce clutter on massave servers when someone new joins
     * Also if the it fails to encrypt the rsa key than run the method again after recreating the aes keys
     */
    private void sendAESkeyEnc() {
        try {
            String aesEnc = new String(Encryption.encryptAESwithRSA(publicKey, aesKey));
            writeToPubsub(aesEnc, 3);
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

    //TODO change this to sending to the main chat encrypted with the rsa keys of each person insted of trying to send to private room. We removed private rooms
    private void sendRSAkey() {
        try {
            for (String user : users.keySet()) {
                if (ipfs.pubsub.peers(user).toString().split(",").length != users.size()) {
                    writeToPubsub(String.valueOf(publicKey), 3);
                } else {
                    throw new SecurityException("Someone else is in the chat while we are trying to send the rsa key to their private chat");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Write the message to pubsub. Is called by encryptMessage
     * Adding a 1 means that it in an internal message a 0 means that it is external
     *
     * @param phrase    The phrase to be encrypted and then sent
     * @param delimiter tells the program what to do with the message. 0 is read the message to the user. 1 is it is a read response. 2 is sending of an rsa key. 3 is an aes key
     */
    void writeToPubsub(String phrase, int delimiter) {
        try {
            String encPhrase = Encryption.encrypt(System.currentTimeMillis() + "*" + User.userName + "*" + phrase + delimiter, aesKey);
//            It breaks if you take this out
            Encryption.decrypt(encPhrase, aesKey);
            ipfs.pubsub.pub(this.roomName, encPhrase);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Write the message to pubsub. Is called by encryptMessage
     * Adding a 1 means that it in an internal message a 0 means that it is external
     *
     * @param roomName  The room to post to
     * @param phrase    The phrase to be encrypted and then sent
     * @param delimiter tells the program what to do with the message. 0 is read the message to the user. 1 is it is a read response. 2 is sending of an rsa key. 3 is an aes key. Just sending username for
     */
    void writeToPubsub(String roomName, String phrase, short delimiter) {
        try {
            String encPhrase = Encryption.encrypt(System.currentTimeMillis() + "*" + User.userName + "*" + phrase + delimiter, aesKey);
//            It breaks if you take this out
            Encryption.decrypt(encPhrase, aesKey);
            ipfs.pubsub.pub(roomName, encPhrase);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Set latest message by the person who's hash is passed in as read
     *
     * @param timeOfText the time the text was sent which is the key used to look up the message in the messages adt
     * @param hash       of the user who has seen the message
     */
    private void setAsSeen(long timeOfText, String hash) {
//        HashMap<String, HashMap<String, HashMap<String, Boolean>>> message = new HashMap<>();
//        messages.put();
    }

    private void closeApp() {
        messages.clear();
    }
}
