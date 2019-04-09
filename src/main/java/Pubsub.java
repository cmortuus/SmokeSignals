import io.ipfs.api.IPFS;
import io.ipfs.multiaddr.MultiAddress;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Stream;

public class Pubsub implements Runnable {
    Stream<Map<String, Object>> room;
    String roomName;
    IPFS ipfs;
    ArrayList<User> users;
    PrivateKey privateKey;
    PublicKey publicKey;

    //    TODO implament rsa signing and encryption and aes encryption for the messages sent back and forth

    /**
     * Subscribes to pubsub room
     *
     * @param roomName
     */
    public Pubsub(String roomName) {
        try {
            users = new ArrayList<>();
            this.roomName = roomName;
            ipfs = new IPFS(new MultiAddress("/ip4/127.0.0.1/tcp/5001"));
            room = ipfs.pubsub.sub(roomName);

//            TODO save the keys to file so that they can be sent
            KeyPair keypair = Encrypt.generateKeys();
            publicKey = keypair.getPublic();
            privateKey = keypair.getPrivate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Threaded method that prints out the room name and then the data that follows
     */
    @Override
    public void run() {
//        Needs two try catches because the tey statment that buffered writer is in does not account for the IOExecption that FileWriter will throw
        try {
            File file = new File(roomName);
            if (!file.exists())
                file.createNewFile();
            try (Scanner scnr = new Scanner(file)) {
                try (FileWriter fw = new FileWriter(file, true)) {
//                Write out each line of the stream to a file
                    room.forEach(stringObjectMap -> {
                        try {
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
            }
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    //    I dont know exactly what this is returning, but I think it might be important
    public Object writeToPubsub(String phrase) throws Exception {
        System.out.println(RSA.rsaDecrypt(RSA.rsaEncrypt("hi", publicKey).getBytes(), privateKey));
        return phrase.length() <= 46 ? ipfs.pubsub.pub(roomName, RSA.rsaEncrypt(phrase, publicKey)) : RSA.rsaEncrypt(new ipfs().addText(phrase), privateKey);
    }
}
