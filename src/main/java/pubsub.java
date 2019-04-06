import io.ipfs.api.IPFS;
import io.ipfs.multiaddr.MultiAddress;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.stream.Stream;

public class pubsub implements Runnable {
    Stream<Map<String, Object>> room;
    String roomName;
    IPFS ipfs;

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
        System.out.println(ipfs.pubsub.peers());
//            BufferedWriter bw = new BufferedWriter(new FileWriter(roomName + ipfs.pubsub.peers()));
//            room.forEach(stringObjectMap -> System.out.println(stringObjectMap.values()));
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    //    I dont know exactly what this is returning, but I think it might be important
    public Object writeToPubsub(String phrase) throws Exception {
        return ipfs.pubsub.pub(roomName, phrase);
    }
}
