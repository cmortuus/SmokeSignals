package com.Smoke.Signals;

import java.util.Scanner;

public class main {

    static final boolean DEBUG = false;

    public static void main(String[] args) throws Exception {

        //User me = new User("Caleb#214628");
        User me = new User("Christian#636174");
        me.initialize();
        connect(me);

    }

    private static void connect(User me) {
        String name;
        try { //name = me.joinRoom("Christian#636174");
            //name = me.joinRoom("Caleb#214628");
            name = me.joinRoom("Christian#636174", "Calebz214628Christianz636174");
        } catch (Exception e) {
            name = "Calebz214628Christianz636174"; }
        while (!me.isRoomReady(name))
            try { Thread.sleep(1000);
            } catch (InterruptedException ignore) {}
        System.out.println("live chat enabled for "+name);
        try (Scanner scnr = new Scanner(System.in)) {
            while (true) {
                String msg = scnr.nextLine();
                if (msg.endsWith("quit")) break;
                me.sendToRoom(name, msg);
            }
        }
        me.saveAccount();
        System.exit(0);
    }


    static void debug(String message) {
        if (DEBUG) System.out.println("[DEBUG] " + message);
    }
}

