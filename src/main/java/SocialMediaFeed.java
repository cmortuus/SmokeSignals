import io.ipfs.multihash.Multihash;

import java.util.HashMap;

 class SocialMediaFeed extends Pubsub {

    static HashMap<Long, Post> posts;

    SocialMediaFeed() {
        super(IPFSnonPubsub.ipfsID, true);
        posts = new HashMap<>();
    }

     /**
      * Posts message to personal chats of all friends and if there is a photo to upload send the hash with it
      * Creates object post
      * @param post The pure text of what needs to be uploaded
      */
     private void postMessage(String post){
         for(OtherUser user: User.otherUsers){
             writeToPubsub(user.hash.toString(), post, 6);
         }
     }

    /**
     * Posts message to personal chats of all friends and if there is a photo to upload send the hash with it
     * Creates object post
     * @param post The pure text of what needs to be uploaded
     * @param hashOfImage The multiHash in string form of the image that is posted.
     */
    private void postMessage(String post, Multihash hashOfImage){
        for(OtherUser user: User.otherUsers){
            writeToPubsub(user.hash.toString(), post + "#" + hashOfImage.toString(), 6);
        }
    }

    /**
     * Uploads a comment to a message
     * Adds a element to the comments arraylist
     * Sends message ending with 5
     * @param comment Pure text of message
     * @param postID The identifier for the post
     */
    private void commentOnMessage(String comment, String postID){
        writeToPubsub(postID + "#" + comment, 5);
    }

    private void deletePost(Long postID){

    }

    private void deleteComment(Long commentID){

    }

     /**
      * Check for explatives and in the future nudity and stuff like that to make sure that people are not seeing stuff they dont want to see
      * @param postToCheck the object of the post that needs to be filtered
      */
    private void filterPost(Post postToCheck){

    }
}
