import java.io.IOException;
import java.io.OutputStream;
import java.util.Scanner;

public class main {


    public static void main(String[] args) {

        Runtime rt = Runtime.getRuntime();
        try {
            Process pr = rt.exec("./ipfs daemon --enable-pubsub-experiment --");
            OutputStream processData = pr.getOutputStream();
            new Thread(() -> System.out.println(processData));
        } catch (IOException e) {
            e.printStackTrace();
        }

        User me = new User("Christian");
        String room = me.joinExistingRoom("Caleb#184460");
        while (!me.isRoomReady(room)) {
            try {
                Thread.sleep(5000);
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

    }
}

