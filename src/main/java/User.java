import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.BufferedWriter;
import java.io.FileWriter;

public class User {
    String userName;
    HashMap<String, pubsub> rooms;

//    TODO eventaully change this from one large file to one file that is for your username or aliasis
    public User(String userName) throws IOException {
        SecureRandom rand = new SecureRandom();
        this.userName = userName + '#' + rand.nextInt(1000000);
        rooms = new HashMap<>();

        BufferedWriter bw = new BufferedWriter(new FileWriter("users.txt"));
        try(Scanner scnr = new Scanner(new File("users.txt"))) {
            while (scnr.hasNextLine()){
                if(!(scnr.nextLine().split("#")[0] == userName))
                    bw.append("\n" + userName);
            }

        }
    }

    public void test() throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(Integer.MAX_VALUE);
        System.out.println(turnUsersToRoon(new User("Matt").userName));

        rooms.put("dubsOnly", new pubsub("dubsOnly"));

        for (int i = 0; i < 5; i++)
            rooms.get("dubsOnly").writeToPubsub("lolripyou");

        executorService.submit(rooms.get("dubsOnly"));

        for (int i = 0; i < 5; i++)
            rooms.get("dubsOnly").writeToPubsub("lol");


    }

    public String turnUsersToRoon(String name) {
        String[] s = new String[]{name, userName};
        Arrays.sort(s);
        return String.join("", s);
    }
}
