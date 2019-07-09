package com.Smoke.Signals;

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
     *
     * @param post The pure text of what needs to be uploaded
     */
    private void postMessage(String post) {
        for (OtherUser user : yourself.getOtherUsers()) {
            writeToPubsub(user.getHash().toString(), post, MessageType.POST);
        }
    }

    /**
     * Posts message to personal chats of all friends and if there is a photo to upload send the hash with it
     * Creates object post
     *
     * @param post        The pure text of what needs to be uploaded
     * @param hashOfImage The multiHash in string form of the image that is posted.
     */
    private void postMessage(String post, Multihash hashOfImage) {
        for (OtherUser user : yourself.getOtherUsers()) {
            writeToPubsub(user.getHash().toString(), post + "#" + hashOfImage.toString(), MessageType.POST);
        }
    }

    /**
     * Uploads a comment to a message
     * Adds a element to the comments arraylist
     * Sends message ending with 5
     *
     * @param comment Pure text of message
     * @param post    the post we are commenting on
     */
    private void commentOnMessage(String comment, Post post) {
        writeToPubsub(post.getMessageId() + "#" + post.getAuthorId() + "#" + comment, MessageType.COMMENT);
    }

    /**
     * Edit message without image. This can either edit a message or remove the image of a previous comment
     *
     * @param newContent new text for the message
     * @param post       the original post that we are editing a comment of
     * @param commentID  the comment we are editing on the post
     */
    private void editComment(String newContent, Post post, Long commentID) {
        writeToPubsub(post.getMessageId() + "#" + post.getAuthorId() + "#" + commentID + newContent, MessageType.EDIT_COMMENT);
    }

    /**
     * Edit comment that will finish with an image in it. This can either add an image or edit the image that is there.
     * The comment does not need to start with an image do use this method.
     *
     * @param newContent new Text that is added
     * @param post       The original post that this message is going to be sent to
     * @param commentID  the id of the comment so we can know what to edit
     * @param image      the String representation of the multihash of the image so that we can get the new image to put in
     */
    private void editComment(String newContent, Post post, long commentID, String image) {
        writeToPubsub(post.getMessageId() + "#" + post.getAuthorId() + "#" +
                commentID + "#" + image + "#" + newContent, MessageType.EDIT_COMMENT_WITH_IMAGE);
    }

    private void deletePost(Long postID) {
        writeToPubsub(String.valueOf(postID), MessageType.DELETE_POST);

    }

    private void deleteComment(Long postID, Long commentID) {
        writeToPubsub(postID + "#" + commentID, MessageType.DELETE_COMMENT);
    }

    /**
     * Check for explatives and in the future nudity and stuff like that to make sure that people are not seeing stuff they dont want to see
     *
     * @param postToCheck the object of the post that needs to be filtered
     */
    private void filterPost(Post postToCheck) {

    }
}
