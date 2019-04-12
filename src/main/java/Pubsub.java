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
import java.util.Map;
import java.util.stream.Stream;

//TODO put a handshake in on each message so that if somebody misses a message than they all fill in the gaps
public class Pubsub implements Runnable {
    Stream<Map<String, Object>> room;
    String roomName;
    IPFS ipfs;
    ArrayList<String> users;
    static ArrayList<PublicKey> publicKeys;
    PrivateKey privateKey;
    PublicKey publicKey;
    SecretKey secretKey;
    int numUsersFound;

    public Pubsub(String roomName) {
        try {
            users = new ArrayList<>();
            this.roomName = roomName;
            ipfs = new IPFS(new MultiAddress("/ip4/127.0.0.1/tcp/5001"));
            room = ipfs.pubsub.sub(roomName);
//          Encryption keys
            KeyPair keypair = Encryption.generateKeys();
            publicKey = keypair.getPublic();
            privateKey = keypair.getPrivate();
            secretKey = Encryption.generateAESkey();
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
            if (!file.exists())
                file.createNewFile();
            try (FileWriter fw = new FileWriter(file, true)) {
//                Write out each line of the stream to a file and check if they are one of the users
                room.forEach(stringObjectMap -> {
                    try {
//                            Handshake built into this func
                        String data = stringObjectMap.values().toString().split(",")[1].trim();
                        for (String user : users) {
                            writeToPubsub(User.userName);
                            if (data.equals(user)) {
//                                    check if both we found all the proper users and check that there are not more peers in the chat than there are supposed to be
                                if (++numUsersFound == user.length() && ipfs.pubsub.peers(roomName).toString().split(",").length <= user.length()) {
//                                    TODO make this so that it will not stop trying to decrypt the aes key until it has all the rsa keys
//                                    TODO Validate that we can tell when it is not decrypted properly by the aes function
                                    sendRSAkey();
                                    sendAESkeyEnc();
//                                    TODO test this with random files that are not public keys to make sure it fails and figure out how to account for that maybe try to encrypt something with it and if it throws an error discard it
//                                    Get any rsa keys in a new thread
                                    Thread t = new Thread(() -> {
                                        try {
//                                            Hashes are 46 chars long
                                            if(data.length() == 46) {
                                                byte[] key = IPFSnonPubsub.getFile(new Multihash(data.getBytes()));
                                                PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(key));

                                                publicKeys.add(publicKey);
                                            }
                                        }catch (NoSuchAlgorithmException e){
                                            e.printStackTrace();
                                        }catch (InvalidKeySpecException e){
                                            e.printStackTrace();
                                        }
                                    });
                                    t.start();
                                } else
                                    numUsersFound++;
//                                this makes sure that it does not print out the usernames to file and it is totally silent
                                continue;
                            }
                        }
                        String s = stringObjectMap.values().toString().split(",")[1].trim();
                        byte[] c = Base64.getDecoder().decode(s);
//                        Replacing _ with spaces because inorder to send it into pubsub for now I have to send it in with underscores replacing spaces
                        fw.write(new String(c) + "\n");
                        fw.flush();
//                            System.out.println(RSA.rsaDecrypt(scnr.nextLine().getBytes(), privateKey));
                        System.out.println("Got data from pubsub room " + roomName + " writing to the file.");
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
            for(String user : users)
                writeToPubsub(user, new String(Encryption.encryptAESwithRSA(publicKey, secretKey)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //    TODO make sure this does not send the message if there is more than one person in the chat and throws erros gloore if there is another person
    public ArrayList<Object> sendRSAkey() {
        ArrayList<Object> objects = new ArrayList<>();
        try {
            for (String user : users)
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
}
