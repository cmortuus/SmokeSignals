import java.io.IOException;

public class main {

    public static void main(String[] args) {
        try {
            User caleb = new User("Caleb");
            caleb.test();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}