import io.ipfs.multihash.Multihash;

import java.util.HashMap;

 class SocialMediaFeed extends Pubsub {

    static HashMap<Long, Post> posts;
    private User yourself;

    SocialMediaFeed(User yourself) {
        super(yourself, IPFSnonPubsub.ipfsID, true);
        posts = new HashMap<>();
        this.yourself = yourself;
    }

     /**
      * Posts message to personal chats of all friends and if there is a photo to upload send the hash with it
      * Creates object post
      * @param post The pure text of what needs to be uploaded
      */
     private void postMessage(String post){
         for(OtherUser user : yourself.getOtherUsers()){
             writeToPubsub(user.hash.toString(), post, MessageType.POST);
         }
     }

    /**
     * Posts message to personal chats of all friends and if there is a photo to upload send the hash with it
     * Creates object post
     * @param post The pure text of what needs to be uploaded
     * @param hashOfImage The multiHash in string form of the image that is posted.
     */
    private void postMessage(String post, Multihash hashOfImage){
        for(OtherUser user : yourself.getOtherUsers()){
            writeToPubsub(user.hash.toString(), post + "#" + hashOfImage.toString(), MessageType.POST);
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
        writeToPubsub(postID + "#" + comment, MessageType.COMMENT);
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
