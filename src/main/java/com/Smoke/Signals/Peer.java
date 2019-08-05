package com.Smoke.Signals;

import org.json.JSONObject;

 class Peer {

    private final long userid;
    private String username;
    private String discriminator;
    private boolean isOnline;
    private long lastTimeOnline;

     Peer(long userid, String username, String discriminator) {
        this.userid = userid;
        this.username = username;
        this.discriminator = discriminator;
        this.isOnline = false;
        this.lastTimeOnline = 0;
    }

     Peer(JSONObject json) {
        if (!json.has("userid") || !json.has("username") || !json.has("discriminator"))
            throw new IllegalArgumentException("missing fields");
        userid = json.getLong("userid");
        username = json.getString("username");
        discriminator = json.getString("discriminator");
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

     JSONObject toJSONObject() {
        return new JSONObject()
                .put("userid", userid)
                .put("username", username)
                .put("discriminator", discriminator)
                .put("isOnline", isOnline)
                .put("lastTimeOnline", lastTimeOnline);
    }

}
