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

//                        byte[] c = Base64.getDecoder().decode(data);
                        String decryptedMessage = Encryption.decryptWithAES(aesKey.getEncoded(), data.getBytes());
//                      This probably gets the hash of the user
                        if (decryptedMessage.equals("3a73fa32e7f57fc947053e8edfd27d89a5d11b21c0f0415d9d5669dd6bb07bc5" + ipfs.id().values().toArray()[0])) {
//                            Sends in hash
                            setAsSeen(stringObjectMap.values().toString().split(",")[1].trim());
                        } else {
                            System.out.println(decryptedMessage);
                            if (saveMessage) {
                                fw.write(decryptedMessage + "\n");
                                fw.flush();
                                System.out.println("Got data from pubsub room " + roomName + " writing to the file.");
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        } catch (IOException e) {
            System.out.println(e);
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
     * encrypt message with aes and then send the message
     *
     * @param message
     * @param secretKey
     */
    public void encryptMessage(String message, SecretKey secretKey) {
        String s = new String(Encryption.encryptAES(message, secretKey));
        if (s != null)
            writeToPubsub(s);
    }

    /**
     * Write the message to pubsub. Is called by encryptMessage
     *
     * @param phrase
     * @return I dont know exactly what this is returning, but I think it might be important
     */
    public Object writeToPubsub(String phrase) {
        try {
            return ipfs.pubsub.pub(roomName, phrase);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Object writeToPubsub(String roomName, String phrase) {
        try {
            return ipfs.pubsub.pub(roomName, phrase);
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
