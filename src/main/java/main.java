import java.io.IOException;
import java.util.Scanner;

public class main {
    public static void main(String[] args) throws IOException {

        User user = new User("Christian");
        String roomname = user.joinExistingRoom("Caleb#123456");
        System.out.println("Connecting to room " + roomname);
        while (!user.isRoomReady(roomname)) {
            try { Thread.sleep(1000);
            } catch (InterruptedException ignore) {}
        }
        try (Scanner scnr = new Scanner(System.in)) {
            System.out.println("Connected to " + roomname);
            while (true) {
                String message = scnr.nextLine();
                if (message.equals("quit")) break;
                user.sendToRoom(roomname, message);
            }
        }
    }
}
