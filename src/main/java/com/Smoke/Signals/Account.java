package com.Smoke.Signals;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

class Account {

    private final long userid;
    private String username;
    private String discriminator;
    private HashSet<String> roomnames;
    private HashMap<Long, Peer> peers;
    private transient HashMap<String, Peer> idMap;
    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int N = ALPHABET.length();

    Account(String username) {
        this.username = username;
        discriminator = generateDiscriminator();
        userid = getFullUsername().hashCode() + System.currentTimeMillis();
        roomnames = new HashSet<>();
        peers = new HashMap<>();
        idMap = new HashMap<>();
    }

    Account(String username, String discriminator) {
        this.username = username;
        this.discriminator = discriminator;
        userid = getFullUsername().hashCode() + System.currentTimeMillis();
        roomnames = new HashSet<>();
        peers = new HashMap<>();
        idMap = new HashMap<>();
    }

    Account(JSONObject json) {
        if (!json.has("userid") || !json.has("username") || !json.has("discriminator") || !json.has("roomnames") || !json.has("peers"))
            throw new IllegalArgumentException("missing fields");
        userid = json.getLong("userid");
        username = json.getString("username");
        discriminator = json.getString("discriminator");
        roomnames = new HashSet<>();
        json.getJSONArray("roomnames").iterator().forEachRemaining(o -> roomnames.add(String.valueOf(o.toString())));
        peers = new HashMap<>();
        for (Object o : json.getJSONArray("peers")) {
            Peer p = new Peer(new JSONObject(o.toString()));
            peers.put(p.getUserId(), p);
        }
//        json.getJSONArray("peers").iterator().forEachRemaining(o -> {
//            Peer p = new Peer(new JSONObject(o.toString()));
//            peers.put(p.getUserId(), p);
//        });
        idMap = new HashMap<>();
    }

    long getUserId() {
        return userid;
    }

    String getUsername() {
        return username;
    }

    String getFullUsername() {
        return username + '#' + discriminator;
    }

    String getDiscriminator() {
        return discriminator;
    }

    ArrayList<String> getRoomnames() {
        return new ArrayList<>(roomnames);
    }

    Peer getPeer(long userid) {
        if (!peers.containsKey(userid))
            addPeer(new Peer(userid, "Unknown", "000000"));
        return peers.get(userid);
    }

    Peer getPeer(String peerId) {
        return idMap.get(peerId);
    }

    HashMap<Long, Peer> getPeers() {
        return peers;
    }

    void registerPeerId(String id, Peer peer) {
        idMap.put(id, peer);
    }

    Peer unregisterPeerId(String id) {
        return idMap.remove(id);
    }

    /**
     * Changes the username of the account and generates a new discriminator.
     *
     * @param username the new username to use
     * @return the old username#discriminator
     */
    String updateUsername(String username) {
        String result = getFullUsername();
        this.username = username;
        discriminator = generateDiscriminator();
        return result;
    }

    void addRoom(String roomname) {
        roomnames.add(roomname);
    }

    void addPeer(Peer peer) {
        peers.put(peer.getUserId(), peer);
    }

    String getSaveFilename() {
        return userid + ".json";
    }

    JSONObject toJSONObject() {
        JSONArray peerArr = new JSONArray();
        peers.values().forEach(p -> peerArr.put(p.toJSONObject()));
        return new JSONObject()
                .put("userid", userid)
                .put("username", username)
                .put("discriminator", discriminator)
                .put("roomnames", roomnames)
                .put("peers", peerArr);
    }

    @Override
    public String toString() {
        return username + '#' + discriminator + " (" + userid + ")";
    }

    static String generateDiscriminator() {
        Random r = new Random();
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < 6; i++)
            s.append(ALPHABET.charAt(r.nextInt(N)));
        return s.toString();
    }
}
