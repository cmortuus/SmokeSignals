package com.Smoke.Signals;

import java.io.IOException;
import java.util.Scanner;

public class main {

    public static void main(String[] args) throws IOException {

        User me = new User("Christian#636174");
        me.initialize();
        connect(me);

    }

    private static void connect(User me) {
        String name;
        try { name = me.joinRoom("Caleb#214628");
        } catch (Exception e) { name = "Calebz214628Christianz636174"; }
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
}

