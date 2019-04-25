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
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

//TODO Make it so that you can edit texts after you send them
//    TODO give each message a hash and refference it by the hash of the message. Every message sent muight havea  hash that I just need to find
//    TODO use this to edit the message, maybe keep a history maybe dont
//TODO allow people to create a chat where after a message is seen it dissapears
//TODO make private room based of hash if possable. I dont know if that is going to work
//TODO when making the socaial media side of it allow people to add people to chats, but not add them to the social media feed
//TODO add handshake for seen messages and write the messages that the user sends to the log file
//TODO put a handshake in on each message so that if somebody misses a message than they all fill in the gaps
//TODO Have them keep trying to decrypt the aes keys until they have everyone in the chat and then if one of the keys does not work throw an error to have everyone remake and resend the keys
//TODO add voice features in as well
//TODO make it so that it dyncmicly choses to use one aes key per person or per room depending on number of people in room. If you have an aes key for each person you have to send a message for each person.
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

    public Pubsub(String roomName, Boolean saveMessage) {
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
                        if (++numUsersFound == users.size() && ipfs.pubsub.peers(roomName).toString().split(",").length <= users.size()) {
                            for (String user : users.keySet()) {
                                writeToPubsub(User.userName);
                                if (data.equals(user)) {
//                                    check if both we found all the proper users and check that there are not more peers in the chat than there are supposed to be
                                    if (++numUsersFound == users.size() && ipfs.pubsub.peers(roomName).toString().split(",").length <= users.size()) {
//                                    TODO make this so that it will not stop trying to decrypt the aes key until it has all the rsa keys
//                                    TODO Validate that we can tell when it is not decrypted properly by the aes function
                                        sendRSAkey();
                                        sendAESkeyEnc();
//                                    TODO test this with random files that are not public keys to make sure it fails and figure out how to account for that maybe try to encrypt something with it and if it throws an error discard it
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
                                    } else
                                        numUsersFound++;
//                                this makes sure that it does not print out the usernames to file and it is totally silent
                                    continue;
                                }
                            }
                        }
                        byte[] decodedBytes = Base64.getDecoder().decode(data);
                        String decodedString = new String(decodedBytes);
                        String decryptedMessage = Encryption.decrypt(decodedString, aesKey);
                        System.out.println("decrypted message = " + decryptedMessage);
//                        TODO check here for any messages that have the same value as your ipfs id. There is probably a better way to check if a message has been seen
                        String ipfsID = ipfs.refs.local().toArray()[1].toString();
                        System.out.println(ipfsID);
                        if (decryptedMessage.equals(ipfsID.trim())) {
//                            Sends in hash
                            setAsSeen(stringObjectMap.values().toString().split(",")[1].trim());
//                        If it is not a confomration message that it has been seen write the message to file
                        } else {
                            System.out.println(decryptedMessage.trim());
                            if (saveMessage) {
                                fw.write(decryptedMessage + "\n");
                                fw.flush();
                                System.out.println("Got data from pubsub room " + roomName + " writing to the file.\n");
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Encrypt the aes key and then send it to each person's private room individually to reduce clutter on massave servers when someone new joins
     */
    public void sendAESkeyEnc() {
        try {
            for (String user : users.keySet())
                writeToPubsub(user, new String(Encryption.encryptAESwithRSA(publicKey, aesKey)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //    TODO make sure this does not send the message if there is more than one person in the chat and throws erros gloore if there is another person
    public ArrayList<Object> sendRSAkey() {
        ArrayList<Object> objects = new ArrayList<>();
        try {
            for (String user : users.keySet())
                objects.add(ipfs.pubsub.pub(user, String.valueOf(publicKey)));
            return objects;
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
    public Object writeToPubsub(String phrase) {
        try {
            String encPhrase = Encryption.encrypt(phrase, aesKey);
//            It breaks if you take this out
            System.out.println("dec phrase = " + Encryption.decrypt(encPhrase, aesKey) + "\n");
            return ipfs.pubsub.pub(roomName, encPhrase);
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
    public Object writeToPubsub(String roomName, String phrase) {
        try {
            String encPhrase = Encryption.encrypt(phrase, aesKey);
//            It breaks if you take this out
            System.out.println("dec phrase = " + Encryption.decrypt(encPhrase, aesKey) + "\n");
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
