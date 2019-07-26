package com.Smoke.Signals.account;

import org.json.JSONArray;
import org.json.JSONObject;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class Account {

    private final long userid;
    private String username;
    private String discriminator;
    private HashSet<String> roomnames;
    private HashMap<Long, Peer> peers;

    public Account(String username) {
        this.username = username;
        discriminator = generateDiscriminator();
        userid = getFullUsername().hashCode() + System.currentTimeMillis();
        roomnames = new HashSet<>();
        peers = new HashMap<>();
    }

    public Account(String username, String discriminator) {
        this.username = username;
        this.discriminator = discriminator;
        userid = getFullUsername().hashCode() + System.currentTimeMillis();
        roomnames = new HashSet<>();
        peers = new HashMap<>();
    }

    public Account(JSONObject json) {
        if (!json.has("userid") || !json.has("username") || !json.has("discriminator") || !json.has("roomnames") || !json.has("peers"))
            throw new IllegalArgumentException("missing fields");
        userid = json.getLong("userid");
        username = json.getString("username");
        discriminator = json.getString("discriminator");
        roomnames = new HashSet<>();
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

    /**
     * Changes the username of the account and generates a new discriminator.
     *
     * @param username  the new username to use
     * @return          the old username#discriminator
     */
    public String updateUsername(String username) {
        String result = getFullUsername();
        this.username = username;
        discriminator = generateDiscriminator();
        return result;
    }

    public void addRoom(String roomname) {
        roomnames.add(roomname);
    }

    public void addPeer(Peer peer) {
        peers.put(peer.getUserId(), peer);
    }

    public String getSaveFilename() {
        return userid+".json";
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

    public static String generateDiscriminator() {
        int min = 100000;
        int max = 1000000;
        return String.valueOf(new SecureRandom().nextInt((max - min) + 1) + min);
    }

}
