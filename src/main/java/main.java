public class main {

    public static void main(String[] args) {
        try {
            User Caleb = new User("CalebMorton");
            Caleb.createRoom("MattBennet");
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
