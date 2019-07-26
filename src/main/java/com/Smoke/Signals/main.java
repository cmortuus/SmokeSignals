package com.Smoke.Signals;

import java.util.Scanner;

public class main {

    static Logging logging;

    public static void main(String[] args) {

        User me = new User("Christian");
//        logging = new Logging(me);

        String room = me.joinRoom("Caleb#214628");
        while (!me.isRoomReady(room)) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignore) {
            }
        }
        System.out.println("live chat enabled");
        Scanner scnr = new Scanner(System.in);
        while (true) {
            String msg = scnr.nextLine();
            if (msg.equals("quit")) break;
            me.sendToRoom(room, msg);
        }
        scnr.close();

    }
}

