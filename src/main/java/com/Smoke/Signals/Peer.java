package com.Smoke.Signals;

import org.json.JSONObject;

import java.util.HashSet;

class Peer {

    private final long userid;
    private String username;
    private String discriminator;
    private boolean isOnline;
    private long lastTimeOnline;
    private HashSet<String> joinedRooms;
    private HashSet<String> joinedSocialMediaProfiles;

    Peer(long userid, String username, String discriminator) {
        this.userid = userid;
        this.username = username;
        this.discriminator = discriminator;
        this.isOnline = false;
        this.lastTimeOnline = 0;
        joinedRooms = new HashSet<>();
        joinedSocialMediaProfiles = new HashSet<>();
    }

    Peer(JSONObject json) {
        if (!json.has("userid") || !json.has("username") || !json.has("discriminator"))
            throw new IllegalArgumentException("missing fields");
        userid = json.getLong("userid");
        username = json.getString("username");
        discriminator = json.getString("discriminator");
        if (json.has("associated-rooms"))
            json.getJSONArray("associated-rooms").iterator().forEachRemaining(o -> joinedRooms.add(String.valueOf(o.toString())));
        else joinedRooms = new HashSet<>();
        if (json.has("associated-social-media"))
            json.getJSONArray("associated-social-media").iterator().forEachRemaining(o -> joinedSocialMediaProfiles.add(String.valueOf(o.toString())));
        else joinedSocialMediaProfiles = new HashSet<>();
    }

    long getUserId() {
        return userid;
    }

    String getUsername() {
        return username;
    }

    String getFullUsername() {
        return username+'#'+discriminator;
    }

    String getDiscriminator() {
        return discriminator;
    }

    void updateUsername(String username) {
        this.username = username;
    }

    void updateDiscriminator(String discriminator) {
        this.discriminator = discriminator;
    }

    void setOnline(boolean isOnline){
        this.isOnline = isOnline;
    }

    boolean getIsOnline(){
        return isOnline;
    }

    long getLastTimeOnline() {
        return lastTimeOnline;
    }

    void setLastTimeOnline(long lastTimeOnline) {
        this.lastTimeOnline = lastTimeOnline;
    }

    HashSet<String> getJoinedRooms() {
        return new HashSet<>(joinedRooms);
    }

    void addJoinedRoom(String roomname) {
        joinedRooms.add(roomname);
    }

    void removeJoinedRoom(String roomname) {
        joinedRooms.remove(roomname);
    }

    HashSet<String> getJoinedSocialMediaProfiles() {
        return new HashSet<>(joinedSocialMediaProfiles);
    }

    void addJoinedSocialMediaProfile(String name) {
        joinedSocialMediaProfiles.add(name);
    }

    void removeJoinedSocialMediaProfile(String name) {
        joinedSocialMediaProfiles.remove(name);
    }

    JSONObject toJSONObject() {
        return new JSONObject()
                .put("userid", userid)
                .put("username", username)
                .put("discriminator", discriminator)
                .put("isOnline", isOnline)
                .put("lastTimeOnline", lastTimeOnline);
    }

}
