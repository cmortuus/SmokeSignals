package com.Smoke.Signals;

enum FriendType {
    CANCHAT(0),
    CANSEEYOURPOSTS(1),
    CANSEETHEIRPOSTS(2),
    CANSEEALLPOSTS(3),
    BLOCKED(4);

    private final int id;

    FriendType(int id) {
        this.id = id;
    }

    public int getID() {
        return id;
    }

}
