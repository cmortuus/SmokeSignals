import io.ipfs.multihash.Multihash;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

public class PersonalRoom extends Pubsub {

    public PersonalRoom() {
        super(User.userName);
    }

    /**
     * Threaded method that prints out the room name and then the data that follows
     * The handshake function was built into this one
     */
    @Override
    public void run() {
        room.forEach(stringObjectMap -> {
            try {
                String data = stringObjectMap.values().toString().split(",")[1].trim();
                byte[] key = IPFSnonPubsub.getFile(new Multihash(data.getBytes()));
                PublicKey publicKey = KeyFactory.getInstance("AES").generatePublic(new X509EncodedKeySpec(key));
                publicKeys.add(publicKey);
//              TODO check that this is what an empty pubsub room looks like
                if (ipfs.pubsub.peers(roomName).toString().equals("[ ]"))
                    throw new SecurityException("There is someone else listening in your personal room. This is really bad");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

}
