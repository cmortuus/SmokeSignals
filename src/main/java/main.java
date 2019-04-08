public class main {

    //    TODO implament rsa signing and encryption and aes encryption for the messages sent back and forth
    public static void main(String[] args) {
        try {
            User Caleb = new User("CalebMorton");
            Caleb.createRoom("MattBennet");
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
