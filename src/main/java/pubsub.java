import io.ipfs.api.IPFS;
import io.ipfs.multiaddr.MultiAddress;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.stream.Stream;

public class pubsub implements Runnable {
    Stream<Map<String, Object>> room;
    String roomName;
    IPFS ipfs;


    //    TODO implament rsa signing and encryption and aes encryption for the messages sent back and forth
    public pubsub(String roomName) {
        try {
            this.roomName = roomName;
            ipfs = new IPFS(new MultiAddress("/ip4/127.0.0.1/tcp/5001"));
            room = ipfs.pubsub.sub(roomName);
            System.out.println(ipfs.refs.local());
        } catch (Exception e) {
            System.out.println(e);
        }
    }


    /**
     * Threaded method that prints out the room name and then the data that follows
     */
    @Override
    public void run() {
        try {
            try (BufferedWriter bw = new BufferedWriter(new FileWriter("lol.txt"))) {
                room.forEach(stringObjectMap -> {
                    try {
                        String s = stringObjectMap.values().toString().split(",")[1].trim();
                        byte[] c = Base64.getDecoder().decode(s);
                        bw.write(new String(c) + "\n");
                        bw.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    //    I dont know exactly what this is returning, but I think it might be important
    public Object writeToPubsub(String phrase) throws Exception {
        return ipfs.pubsub.pub(roomName, phrase);
    }
}
