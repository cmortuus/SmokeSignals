package com.Smoke.Signals;

import java.util.ArrayList;
import java.util.HashMap;

class Post extends Message {
    private HashMap<Long, Post> comments;
    private ArrayList<String> likes;
    private ArrayList<String> views;
    private String postContent;
    private String hashOfImage;

    int getNumLikes() {
        return likes.size();
    }

    int getNumViews() {
        return views.size();
    }

    void addLike(OtherUser user) {
        likes.add(user.getUserName());
    }

    void addLike(String user) {
        likes.add(user);
    }

    void addView(OtherUser user) {
        views.add(user.getUserName());
    }

    void addView(String user) {
        views.add(user);
    }

    HashMap<Long, Post> getComments() {
        return comments;
    }

    void addComment(Post comment) {
        comments.put(comment.getMessageId(), comment);
    }

    void editComment(String content, Long commentId) {
        for (Post comment : comments.values())
            if (comment.getMessageId() == commentId)
                this.postContent = content;
    }

    void editComment(String content, Long commentId, String commentImage) {
        for (Post comment : comments.values()) {
            if (comment.getMessageId() == commentId) {
                this.postContent = content;
                this.hashOfImage = commentImage;
            }
        }
    }

    void deleteComment(long commentID) {
        comments.remove(commentID);
    }

    String getPostContent() {
        return postContent;
    }

    String getHashOfImage() {
        return hashOfImage;
    }

    Post(Message message) {
        super(message.toJSONObject());
        this.comments = new HashMap<>();
        this.likes = new ArrayList<>();
        this.views = new ArrayList<>();

        if (message.getMessageType() == MessageType.COMMENT) {
            String[] splitMessage = message.getContent().split("#", 4);
            this.postContent = splitMessage[2];
            if (IPFSnonPubsub.isMultiHash(splitMessage[3]))
                this.hashOfImage = splitMessage[1];
            else
                this.hashOfImage = null;
        } else if (message.getMessageType() == MessageType.POST) {
            String[] splitMessage = message.getContent().split("#", 1);
            this.postContent = splitMessage[0];
            if (IPFSnonPubsub.isMultiHash(splitMessage[1]))
                this.hashOfImage = splitMessage[1];
            else
                this.hashOfImage = null;

        }
    }
}
