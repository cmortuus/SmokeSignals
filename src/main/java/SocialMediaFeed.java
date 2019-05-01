import io.ipfs.multihash.Multihash;

public class SocialMediaFeed extends Pubsub {

    SocialMediaFeed() {
        super(IPFSnonPubsub.ipfsID, true);
        System.out.println(IPFSnonPubsub.ipfsID);
    }

    /**
     * Posts message to personal chats of all friends and if there is a photo to upload send the hash with it
     * Creates object post
     * @param post The pure text of what needs to be uploaded
     * @param hashOfImage The multiHash in string form of the image that is posted.
     */
    private void postMessage(String post, Multihash hashOfImage){

    }

    /**
     * Uploads a comment to a message
     * Adds a element to the comments arraylist
     * @param comment Pure text of message
     * @param postID The identifier for the post
     */
    private void commentOnMessage(String comment, String postID){

    }
}
