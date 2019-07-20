package com.Smoke.Signals;

import io.ipfs.multihash.Multihash;

import javax.crypto.SecretKey;
import java.security.PublicKey;

class OtherUser {
    private String userName;
    private Multihash hash;
    private SecretKey secretKey;
    private PublicKey publicKey;
    private FriendType friendType;

    OtherUser(String userName, Multihash hash, SecretKey secretKey, PublicKey publicKey, FriendType friendType) {
        this.userName = userName;
        this.hash = hash;
        this.secretKey = secretKey;
        this.publicKey = publicKey;
        this.friendType = friendType;
    }

    String getUserName() {
        return userName;
    }

    Multihash getHash() {
        return hash;
    }

    FriendType getFriendType() {
        return friendType;
    }

}