import io.ipfs.api.IPFS;
import io.ipfs.api.NamedStreamable;
import io.ipfs.multiaddr.MultiAddress;
import io.ipfs.multihash.Multihash;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class IPFSnonPubsub {

    static IPFS ipfs = new IPFS(new MultiAddress("/ip4/127.0.0.1/tcp/5001"));

    static String ipfsID;

    static {
        try {
            ipfsID = IPFSnonPubsub.ipfs.id().toString().split("(,|=)", 3)[1];
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * adds file to IPFS and pins it and returns hash
     *
     * @param filename location of the file that needs to be uploaded
     * @return hash of the file that gets uploaded, Null if fails open the file.
     */
    static Multihash addFile(String filename) {
        try {
            File f = new File(filename);
            if (!f.exists())
                throw new IOException("The file does not exist");
            NamedStreamable.FileWrapper file = new NamedStreamable.FileWrapper(f);
            Multihash hash = ipfs.add(file).get(0).hash;
            ipfs.pin.add(hash);
            return hash;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Uploads a list of files. Pins them and then returns all the hashes
     *
     * @param filename array of filenames that need to be uploaded
     * @return The hashes of the uploaded files
     */
    static ArrayList<Multihash> addFile(String[] filename) {
        try {
            ArrayList<Multihash> hashes = new ArrayList<>();
            for (String f : filename) {
                NamedStreamable.FileWrapper file = new NamedStreamable.FileWrapper(new File(f));
                hashes.add(ipfs.add(file).get(0).hash);
            }
            for (Multihash hash : hashes)
                ipfs.pin.add(hash);
            return hashes;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Download file from hash
     *
     * @param hash the identifiers of the files that need to be downloaded
     * @return the byte array that is the file
     */
    static byte[] getFile(Multihash hash) {
        try {
            return ipfs.cat(hash);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Download list of files. Returns in form of bytes. if it fails than it returns null
     *
     * @param hashes the identifiers of the files that need to be downloaded
     * @return Arraylist the byte arrays that are files
     */
    public static ArrayList<byte[]> getFile(Multihash[] hashes) {
        try {
            ArrayList<byte[]> files = new ArrayList<>();
            for (Multihash hash : hashes) {
                files.add(ipfs.cat(hash));
            }
            return files;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}