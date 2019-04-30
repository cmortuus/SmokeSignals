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
public class Pubsub implements Runnable {
    Boolean saveMessage;
    Stream<Map<String, Object>> room;
    String roomName;
    IPFS ipfs;
    //    Username and hash
    HashMap<String, String> users;
    ArrayList<PublicKey> publicKeys;
    PrivateKey privateKey;
    PublicKey publicKey;
    SecretKey aesKey;
    int numUsersFound;
    String username;
    //    THe first string is a hash to indacate the message than the inside hashmap is the message its self and whether or not it has been seen
    HashMap<Long, HashMap<String, Boolean>> messages;

    public Pubsub(String roomName, Boolean saveMessage, String username) {
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
            this.username = username;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Threaded method that prints out the room name and then the data that follows
     * The handshake function was built into this one
     */
    @Override
    public void run() {
//        Needs two try catches because the tey statment that buffered writer is in does not account for the IOExecption that FileWriter will throw
        try {
            File file = new File(roomName);
            if (saveMessage) {
                if (!file.exists())
                    file.createNewFile();
            }
            try (FileWriter fw = new FileWriter(file, true)) {
//                Write out each line of the stream to a file and check if they are one of the users
                room.forEach(stringObjectMap -> {
                    try {
//                      Handshake built into this func
//                      Gets the text of the message
                        String data = stringObjectMap.values().toString().split(",")[1].trim();
                        String sender = stringObjectMap.values().toString().split(",")[2].trim();
                        if (++numUsersFound == users.size() && ipfs.pubsub.peers(roomName).toString().split(",").length <= users.size()) {
                            for (String user : users.keySet()) {
                                writeToPubsub(User.userName, true);
                                if (data.equals(user)) {
//                                    check if both we found all the proper users and check that there are not more peers in the chat than there are supposed to be
                                    if (++numUsersFound == users.size() && ipfs.pubsub.peers(roomName).toString().split(",").length <= users.size()) {
                                        sendRSAkey();
                                        sendAESkeyEnc();
//                                    Get any rsa keys in a new thread
                                        Thread rsa = new Thread(() -> {
                                            try {
//                                            Hashes are 46 chars long
                                                if (data.length() == 46) {
                                                    byte[] key = IPFSnonPubsub.getFile(new Multihash(data.getBytes()));
                                                    PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(key));
                                                    publicKeys.add(publicKey);
                                                }
                                            } catch (NoSuchAlgorithmException e) {
                                                e.printStackTrace();
                                            } catch (InvalidKeySpecException e) {
                                                e.printStackTrace();
                                            }
                                        });
                                        rsa.start();
                                    } else {
                                        numUsersFound++;
                                        continue;
                                    }
                                }
                            }
                        }
                        byte[] decodedBytes = Base64.getDecoder().decode(data);
//                        For some reason it removes the +s and adds spaces which throw errors because that is not a thing in an aes string
                        String decodedString = new String(decodedBytes).replaceAll(" ", "+");
                        String decryptedMessage = Encryption.decrypt(decodedString, aesKey);
                        String[] timeAndMessage = decryptedMessage.split("\\*", 3);

                        StringBuilder sb = new StringBuilder();
//                        Print out message. For some reason when you do it in one print statment it hangs with no error message
                        for (int i = 0; i < timeAndMessage.length; i++) {
                            if (i == 0)
                                sb.append(getTime(timeAndMessage[0]) + "  ");
                            else if (i == 2)
                                sb.append(timeAndMessage[i].substring(0, timeAndMessage[2].length() - 1) + "  ");
                            else
                                sb.append(timeAndMessage[i].split("#")[0] + "  ");
                        }
                        System.out.println(sb.toString());
                        addMessage(timeAndMessage);
                        if (decryptedMessage.endsWith("1")) {
                            decryptedMessage = decryptedMessage.substring(0, data.length() - 1);
//                        TODO check here for any messages that have the same value as your ipfs id. There is probably a better way to check if a message has been seen
                            String ipfsID = ipfs.refs.local().toArray()[1].toString();
                            System.out.println(ipfsID);
                            if (decryptedMessage != null) {
                                System.out.println(decryptedMessage.trim());
                                if (saveMessage) {
                                    fw.write(decryptedMessage.substring(0, decryptedMessage.length() - 1) + "\n");
                                    fw.flush();
                                    System.out.println("Got data from pubsub room " + roomName + " writing to the file.\n");
                                }
                            } else if (decryptedMessage.endsWith("0") && decryptedMessage.equals(ipfsID.trim())) {
                                setAsSeen(decryptedMessage.substring(0, decryptedMessage.length() - 1));
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //TODO update this for new way messages are sent
    private long addMessage(String[] decryptedMessage) {
        HashMap<String, Boolean> hashMap = new HashMap<>();
        hashMap.put((decryptedMessage[2].substring(0, decryptedMessage[2].length() - 1)), false);
        messages.put(Long.parseLong(decryptedMessage[0]), hashMap);
        return Long.parseLong(decryptedMessage[0]);
    }

    public String getTime(String time) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        return sdf.format(new Date(Long.parseLong(time)));
    }


    /**
     * Encrypt the aes key and then send it to each person's private room individually to reduce clutter on massave servers when someone new joins
     */
    public void sendAESkeyEnc() {
        try {
            for (String user : users.keySet())
                writeToPubsub(user, new String(Encryption.encryptAESwithRSA(publicKey, aesKey)), true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //    TODO make sure this does not send the message if there is more than one person in the chat and throws erros gloore if there is another person
    public ArrayList<Object> sendRSAkey() {
        ArrayList<Object> objects = new ArrayList<>();
        try {
            for (String user : users.keySet()) {
                if (ipfs.pubsub.peers(user).toString().split(",").length != users.size()) {
                    objects.add(ipfs.pubsub.pub(user, String.valueOf(publicKey)));
                    return objects;
                } else {
                    throw new SecurityException("Someone else is in the chat while we are trying to send the rsa key to their private chat");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    /**
     * Write the message to pubsub. Is called by encryptMessage
     * Adding a 1 means that it in an internal message a 0 means that it is external
     *
     * @param phrase
     * @return I dont know exactly what this is returning, but I think it might be important
     */
    public Object writeToPubsub(String phrase, boolean isInternal) {
        try {
            String encPhrase;
            if (isInternal)
                encPhrase = Encryption.encrypt(System.currentTimeMillis() + "*" + username + "*" + phrase + '1', aesKey);
            else
                encPhrase = Encryption.encrypt(System.currentTimeMillis() + "*" + username + "*" + phrase + '0', aesKey);
//            It breaks if you take this out
            Encryption.decrypt(encPhrase, aesKey);
            return ipfs.pubsub.pub(this.roomName, encPhrase);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Write the message to pubsub. Is called by encryptMessage
     *
     * @param phrase
     * @return I dont know exactly what this is returning, but I think it might be important
     */
    public Object writeToPubsub(String roomName, String phrase, boolean isInternal) {
        try {
            String encPhrase;
            if (isInternal)
                encPhrase = Encryption.encrypt(System.currentTimeMillis() + "*" + username + "*" + phrase + '1', aesKey);
            else
                encPhrase = Encryption.encrypt(System.currentTimeMillis() + "*" + username + "*" + phrase + '0', aesKey);
//            It breaks if you take this out
            Encryption.decrypt(encPhrase, aesKey);
            return ipfs.pubsub.pub(roomName, encPhrase);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * Set latest message by the person who's hash is passed in as read
     *
     * @param hash of the user who has seen the message
     */
    public void setAsSeen(String hash) {

    }

}
