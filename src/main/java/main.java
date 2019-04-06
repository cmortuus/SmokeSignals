import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class main {

    public static void main(String[] args) throws Exception{
        try {
//            ipfs ipfs = new ipfs();
//          TODO make this work for more chat rooms than the number of cores. Maybe rotate them through.
            ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            HashMap<String, pubsub> rooms = new HashMap<>();
            rooms.put("dubsOnly", new pubsub("dubsOnly"));
//            System.out.println(ipfs.getFile(ipfs.addFile("lol.txt")).toString().split(","));

            for(int i = 0; i < 5; i++)
                rooms.get("dubsOnly").writeToPubsub("lolripyou");

            executorService.submit(rooms.get("dubsOnly"));

            for(int i = 0; i < 5; i++)
                rooms.get("dubsOnly").writeToPubsub("lol");
        }
        catch (IOException e){
            System.out.println(e);
        }
    }
}
