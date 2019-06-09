import java.util.Scanner;

public class main {

    public static void main(String[] args) {

        User me = new User("Christian");

        String room = me.joinRoom("Caleb#184460");
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

