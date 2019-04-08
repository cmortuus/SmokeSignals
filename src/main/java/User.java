import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class User {
    String userName;
    HashMap<String, Pubsub> rooms;
    PublicKey publicKey;
    PrivateKey privateKey;

    //    TODO eventaully change this from one large file to one file that is for your username or aliasis
    public User(String user) throws IOException {
        rooms = new HashMap<>();

//       Create the file or open it
        File file = new File("users.txt");
        if(!file.exists())
            file.createNewFile();
        FileWriter fw = new FileWriter(file, true);

        try (Scanner scnr = new Scanner(new File("users.txt"))) {
            if (!scnr.hasNextLine()) {
                SecureRandom rand = new SecureRandom();
                int nums = 0;
                while(nums <= 100000)
                    nums = rand.nextInt(1000000);
                this.userName = user + '#' + nums;
                fw.append(this.userName);
                fw.append("\n");
                System.out.println(this.userName);
                fw.flush();

            } else {
                boolean check = false;
                String tempLine;
                while (scnr.hasNextLine()) {
                    tempLine = scnr.nextLine();
                    if ((tempLine.split("#")[0].equals(user))) {
                        this.userName = tempLine;
                        check = true;
                    }
                }

                if(!check) {
                    SecureRandom rand = new SecureRandom();
                    int nums = 0;
                    while(nums <= 100000)
                        nums = rand.nextInt(1000000);
                    this.userName = user + '#' + nums;
                    fw.append(this.userName);
                    fw.append("\n");
                    System.out.println(this.userName);
                    fw.flush();
                }
            }
        }
    }

    /**
     * @param otherUser
     */
    public void createRoom(String otherUser) {
        try {
            ExecutorService executorService = Executors.newFixedThreadPool(Integer.MAX_VALUE);
            String roomName = turnUsersToRoom(new User(otherUser));
            rooms.put(roomName, new Pubsub(roomName));
//            Test the room
            rooms.get(roomName).writeToPubsub("lol rip you");
            executorService.submit(rooms.get(roomName));
            rooms.get(roomName).writeToPubsub("lol");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Turn the usernames of two users into a room name
     * @param user
     * @return
     */
    public String turnUsersToRoom(User user) {
        String[] s = new String[]{user.userName, userName};
        Arrays.sort(s);
        return String.join("", s).replace('#', 'z');
    }

    /**
     * Overload of the normal method for group chats where there are many people in the chat
     * @param users
     * @return
     */
    public String turnUsersToRoom(User[] users){
        String[] s = new String[users.length];
        s[0] = userName;
        int i = 0;
        for(User user : users)
            s[++i] = user.userName;
        Arrays.sort(s);
        return String.join("", s).replace('#', 'z');
    }

}
