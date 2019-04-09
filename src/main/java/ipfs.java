import io.ipfs.api.IPFS;
import io.ipfs.api.NamedStreamable;
import io.ipfs.multiaddr.MultiAddress;
import io.ipfs.multihash.Multihash;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class ipfs {
    IPFS ipfs;

    public ipfs() {
        ipfs = new IPFS(new MultiAddress("/ip4/127.0.0.1/tcp/5001"));
    }

    protected Multihash addFile(String filename) throws IOException {
        NamedStreamable.FileWrapper file = new NamedStreamable.FileWrapper(new File(filename));
        return ipfs.add(file).get(0).hash;
    }


    protected ArrayList<Multihash> addFile(String[] filename) throws IOException {
        ArrayList<Multihash> hashes = new ArrayList<>();
        for (String f : filename) {
            NamedStreamable.FileWrapper file = new NamedStreamable.FileWrapper(new File(f));
            hashes.add(ipfs.add(file).get(0).hash);
        }
        return hashes;
    }

    protected byte[] getFile(Multihash hash) throws IOException {
        return ipfs.cat(hash);
    }

    protected ArrayList<byte[]> getFile(Multihash[] hashes) throws IOException {
        ArrayList<byte[]> files = new ArrayList<>();
        for (Multihash hash : hashes) {
            files.add(ipfs.cat(hash));
        }
        return files;
    }

}