package com.Smoke.Signals.account;

import org.json.JSONArray;
import org.json.JSONObject;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;

public class Account {

    private final long userid;
    private String username;
    private String discriminator;
    private ArrayList<String> roomnames;
    private HashMap<Long, Peer> peers;

    public Account(String username) {
        this.username = username;
        regenerateDiscriminator();
        userid = getFullUsername().hashCode() + System.currentTimeMillis();
        roomnames = new ArrayList<>();
        peers = new HashMap<>();
    }

    public Account(JSONObject json) {
        if (!json.has("userid") || !json.has("username") || !json.has("discriminator") || !json.has("roomnames") || !json.has("peers"))
            throw new IllegalArgumentException("missing fields");
        userid = json.getLong("userid");
        username = json.getString("username");
        discriminator = json.getString("discriminator");
        roomnames = new ArrayList<>();
        json.getJSONArray("roomnames").iterator().forEachRemaining(o -> roomnames.add(String.valueOf(o.toString())));
        peers = new HashMap<>();
        json.getJSONArray("peers").iterator().forEachRemaining(o -> {
            Peer p = new Peer(new JSONObject(o.toString()));
            peers.put(p.getUserId(), p);
        });
    }

    public long getUserId() {
        return userid;
    }

    public String getUsername() {
        return username;
    }

    public String getFullUsername() {
        return username+'#'+discriminator;
    }

    public String getDiscriminator() {
        return discriminator;
    }

    public ArrayList<String> getRoomnames() {
        return new ArrayList<>(roomnames);
    }

    public Peer getPeer(long userid) {
        if (!peers.containsKey(userid))
            addPeer(new Peer(userid, "Unknown", "000000"));
        return peers.get(userid);
    }

    public void updateUsername(String username) {
        this.username = username;
        regenerateDiscriminator();
    }

    public void addRoom(String roomname) {
        if (!roomnames.contains(roomname)) roomnames.add(roomname);
    }

    public void addPeer(Peer peer) {
        peers.put(peer.getUserId(), peer);
    }

    public JSONObject toJSONObject() {
        JSONArray peerArr = new JSONArray();
        peers.values().forEach(p -> peerArr.put(p.toJSONObject()));
        return new JSONObject()
                .put("userid", userid)
                .put("username", username)
                .put("discriminator", discriminator)
                .put("roomnames", roomnames)
                .put("peers", peerArr);
    }

    public String toString() {
        return username+'#'+discriminator+" ("+userid+")";
    }

    private void regenerateDiscriminator() {
        int min = 100000;
        int max = 1000000;
        discriminator = String.valueOf(new SecureRandom().nextInt((max - min) + 1) + min);
    }

}
